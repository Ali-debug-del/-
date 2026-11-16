package com.example.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppShieldDatabase
import com.example.data.local.BlockLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.nio.ByteBuffer

class FirewallVpnService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetLoopJob: Job? = null
    private var memoryLoopJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopFirewall()
                return START_NOT_STICKY
            }
            ACTION_START, ACTION_UPDATE_RULES, null -> {
                ensureForegroundNotification()
                serviceScope.launch {
                    applyRulesAndRebuildTunnel()
                }
            }
        }
        return START_STICKY
    }

    private fun ensureForegroundNotification() {
        createNotificationChannel()
        val notification = buildNotification(
            title = getString(R.string.app_name),
            text = "جدار الحماية ودرع الذاكرة يعملان لحماية الجهاز"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun applyRulesAndRebuildTunnel() {
        val dao = AppShieldDatabase.getInstance(applicationContext).appShieldDao()
        val rules = dao.getAllRulesSync().filter { it.isActive }
        val internetPackages = rules.filter { it.blockMode.blocksInternet }.map { it.packageName }
        val memoryPackages = rules.filter { it.blockMode.blocksMemory }.map { it.packageName }

        FirewallStateHolder.updateRuleCounts(
            internetCount = internetPackages.size,
            memoryCount = memoryPackages.size
        )

        // Rebuild Memory Sweep Loop
        memoryLoopJob?.cancel()
        if (memoryPackages.isNotEmpty()) {
            val memoryGuard = MemoryGuardManager(applicationContext)
            memoryLoopJob = serviceScope.launch {
                while (isActive) {
                    memoryGuard.enforceMemoryBlock(memoryPackages)
                    delay(20_000L)
                }
            }
        }

        // Rebuild VPN Sinkhole TUN
        packetLoopJob?.cancel()
        runCatching { vpnInterface?.close() }
        vpnInterface = null

        if (internetPackages.isEmpty()) {
            FirewallStateHolder.setVpnActive(false)
            if (memoryPackages.isEmpty()) {
                stopFirewall()
                return
            }
            updateNotification(
                "درع الذاكرة نشط (${memoryPackages.size} تطبيق)",
                "يتم مراقبة وتقييد استخدام الذاكرة في الخلفية"
            )
            return
        }

        val builder = Builder()
            .setSession("AppShield Local Firewall")
            .addAddress("10.8.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("10.8.0.1")
            .setMtu(1500)

        var addedCount = 0
        for (pkg in internetPackages) {
            runCatching {
                builder.addAllowedApplication(pkg)
                addedCount++
            }
        }

        if (addedCount == 0) {
            FirewallStateHolder.setVpnActive(false)
            return
        }

        val tun = runCatching { builder.establish() }.getOrNull()
        if (tun == null) {
            FirewallStateHolder.setVpnActive(false)
            return
        }

        vpnInterface = tun
        FirewallStateHolder.setVpnActive(true)
        updateNotification(
            "الحماية نشطة • $addedCount تطبيق محجوب عن الإنترنت",
            if (memoryPackages.isNotEmpty()) {
                "ومراقبة الذاكرة لـ ${memoryPackages.size} تطبيق"
            } else {
                "جميع اتصالات التطبيقات المحجوبة معزولة محلياً"
            }
        )

        packetLoopJob = serviceScope.launch {
            drainSinkholePackets(tun, internetPackages)
        }
    }

    private suspend fun drainSinkholePackets(
        tun: ParcelFileDescriptor,
        internetPackages: List<String>
    ) {
        val input = FileInputStream(tun.fileDescriptor)
        val buffer = ByteArray(32767)
        var pendingBatch = 0L
        var lastPersistTime = System.currentTimeMillis()
        val dao = AppShieldDatabase.getInstance(applicationContext).appShieldDao()

        while (serviceScope.isActive) {
            val length = runCatching { input.read(buffer) }.getOrDefault(-1)
            if (length < 0) break
            if (length == 0) {
                delay(20L)
                continue
            }

            val protocolLabel = inspectIpProtocol(buffer, length)
            FirewallStateHolder.recordBlockedPacket(length, protocolLabel)
            pendingBatch++

            val now = System.currentTimeMillis()
            if (pendingBatch >= 15L || now - lastPersistTime >= 8_000L) {
                val delta = pendingBatch
                pendingBatch = 0L
                lastPersistTime = now
                runCatching {
                    if (internetPackages.isNotEmpty()) {
                        dao.incrementBlockedPacketsForPackages(internetPackages, delta)
                        dao.insertLog(
                            BlockLogEntity(
                                packageName = internetPackages.first(),
                                appName = if (internetPackages.size == 1) {
                                    internetPackages.first()
                                } else {
                                    "${internetPackages.size} تطبيقات محجوبة"
                                },
                                actionType = BlockLogEntity.ACTION_INTERNET_BLOCKED,
                                details = "تم صد $delta حزمة شبكة ($protocolLabel) محلياً"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun inspectIpProtocol(packet: ByteArray, length: Int): String {
        if (length < 20) return "IP"
        val bb = ByteBuffer.wrap(packet, 0, length)
        val version = (bb.get(0).toInt() ushr 4) and 0x0F
        return if (version == 4) {
            when (bb.get(9).toInt() and 0xFF) {
                6 -> "TCP/IPv4"
                17 -> "UDP/IPv4"
                1 -> "ICMP/IPv4"
                else -> "IPv4"
            }
        } else if (version == 6 && length >= 40) {
            "IPv6"
        } else {
            "Packet"
        }
    }

    private fun stopFirewall() {
        packetLoopJob?.cancel()
        memoryLoopJob?.cancel()
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        FirewallStateHolder.setVpnActive(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار مستمر لحالة جدار الحماية ودرع الذاكرة"
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(title: String, text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(title, text))
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FirewallVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pi)
            .addAction(0, "إيقاف الحماية", stopPi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onRevoke() {
        stopFirewall()
        super.onRevoke()
    }

    override fun onDestroy() {
        packetLoopJob?.cancel()
        memoryLoopJob?.cancel()
        runCatching { vpnInterface?.close() }
        FirewallStateHolder.setVpnActive(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.action.START_FIREWALL"
        const val ACTION_UPDATE_RULES = "com.example.action.UPDATE_RULES"
        const val ACTION_STOP = "com.example.action.STOP_FIREWALL"
        private const val CHANNEL_ID = "appshield_firewall_channel"
        private const val NOTIFICATION_ID = 4101

        fun startOrUpdate(context: Context) {
            val intent = Intent(context, FirewallVpnService::class.java).apply {
                action = ACTION_UPDATE_RULES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FirewallVpnService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.startService(intent) }
        }
    }
}
