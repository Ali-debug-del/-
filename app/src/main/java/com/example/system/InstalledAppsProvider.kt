package com.example.system

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class InstalledAppsProvider(private val context: Context) {

    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    suspend fun loadInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val runningProcesses = runCatching { am.runningAppProcesses }.getOrNull().orEmpty()
        val runningPackageSet = HashSet<String>()
        val pidsByPackage = HashMap<String, IntArray>()
        for (proc in runningProcesses) {
            proc.pkgList?.forEach { pkg ->
                runningPackageSet.add(pkg)
                val existing = pidsByPackage[pkg]
                pidsByPackage[pkg] = if (existing == null) intArrayOf(proc.pid) else existing + proc.pid
            }
        }

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val seenPackages = HashSet<String>()
        val result = ArrayList<InstalledAppInfo>(resolveInfos.size)

        for (ri in resolveInfos) {
            val activityInfo = ri.activityInfo ?: continue
            val pkgName = activityInfo.packageName ?: continue
            if (pkgName == context.packageName || !seenPackages.add(pkgName)) continue

            val appInfo = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkgName, PackageManager.ApplicationInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkgName, 0)
                }
            }.getOrNull() ?: continue

            val label = runCatching { pm.getApplicationLabel(appInfo).toString() }
                .getOrDefault(pkgName)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

            val packageInfo = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(
                        pkgName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkgName, PackageManager.GET_PERMISSIONS)
                }
            }.getOrNull()

            val hasInternet = packageInfo?.requestedPermissions
                ?.contains(Manifest.permission.INTERNET) == true
            val versionName = packageInfo?.versionName ?: "1.0"

            val isRunning = runningPackageSet.contains(pkgName)
            var ramMb = 0.0
            if (isRunning) {
                val pids = pidsByPackage[pkgName]
                if (pids != null && pids.isNotEmpty()) {
                    val memInfos = runCatching { am.getProcessMemoryInfo(pids) }.getOrNull()
                    if (memInfos != null) {
                        val totalPssKb = memInfos.sumOf { it.totalPss.toLong() }
                        ramMb = totalPssKb / 1024.0
                    }
                }
            }

            result.add(
                InstalledAppInfo(
                    packageName = pkgName,
                    appName = label,
                    isSystemApp = isSystem,
                    hasInternetPermission = hasInternet,
                    isRunningInBackground = isRunning,
                    estimatedRamMb = ramMb,
                    versionName = versionName
                )
            )
        }

        result.sortedWith(
            compareBy<InstalledAppInfo> { it.isSystemApp }
                .thenByDescending { it.isRunningInBackground }
                .thenBy { it.appName.lowercase() }
        )
    }

    suspend fun loadAppIcon(packageName: String): ImageBitmap? {
        iconCache[packageName]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val bitmap = drawableToBitmap(drawable)
                val img = bitmap.asImageBitmap()
                iconCache[packageName] = img
                img
            }.getOrNull()
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        val scaledW = width.coerceIn(48, 128)
        val scaledH = height.coerceIn(48, 128)
        val bitmap = Bitmap.createBitmap(scaledW, scaledH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
