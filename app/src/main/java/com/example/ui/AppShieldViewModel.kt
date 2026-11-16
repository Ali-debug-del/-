package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppRuleEntity
import com.example.data.local.AppShieldRepository
import com.example.data.local.BlockLogEntity
import com.example.data.local.BlockScheduleEntity
import com.example.data.local.MemoryTelemetryEntity
import com.example.data.model.BlockMode
import com.example.data.model.InstalledAppInfo
import com.example.data.model.MemorySnapshot
import com.example.system.FirewallRuntimeState
import com.example.system.FirewallStateHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppFilterTab(val labelAr: String) {
    ALL("الكل"),
    BLOCKED("المحجوبة"),
    INTERNET_CAPABLE("تستخدم الإنترنت"),
    USER_APPS("تطبيقات المستخدم"),
    SYSTEM_APPS("النظام")
}

data class AppShieldUiState(
    val isLoadingApps: Boolean = true,
    val apps: List<InstalledAppInfo> = emptyList(),
    val filteredApps: List<InstalledAppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val searchQuery: String = "",
    val activeFilter: AppFilterTab = AppFilterTab.ALL,
    val rules: List<AppRuleEntity> = emptyList(),
    val schedules: List<BlockScheduleEntity> = emptyList(),
    val logs: List<BlockLogEntity> = emptyList(),
    val memorySnapshot: MemorySnapshot = MemorySnapshot(),
    val runtimeState: FirewallRuntimeState = FirewallRuntimeState(),
    val hasVpnConsent: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val pendingVpnIntent: Intent? = null,
    val snackbarMessage: String? = null,
    val telemetryPoints: List<MemoryTelemetryEntity> = emptyList(),
    val totalFreedMb: Long = 0L
)

class AppShieldViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppShieldRepository(application)

    private val _rawApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)
    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val _isMultiSelectMode = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(AppFilterTab.ALL)
    private val _memorySnapshot = MutableStateFlow(repository.getMemorySnapshot())
    private val _pendingVpnIntent = MutableStateFlow<Intent?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _hasVpnConsent = MutableStateFlow(repository.prepareVpnIntent() == null)
    private val _hasUsageStats = MutableStateFlow(repository.hasUsageStatsPermission())

    private val rulesFlow = repository.observeRules()
    private val schedulesFlow = repository.observeSchedules()
    private val logsFlow = repository.observeLogs()
    private val telemetryPointsFlow = repository.observeMemoryTelemetry()
    private val totalFreedFlow = repository.observeTotalFreedMb()

    private val appsStateFlow = combine(
        _rawApps,
        rulesFlow,
        schedulesFlow,
        _searchQuery,
        _activeFilter
    ) { rawApps, rules, schedules, query, filter ->
        val ruleMap = rules.associateBy { it.packageName }
        val scheduleCountMap = mutableMapOf<String, Int>()
        for (schedule in schedules.filter { it.isEnabled }) {
            for (pkg in schedule.targetPackages()) {
                scheduleCountMap[pkg] = (scheduleCountMap[pkg] ?: 0) + 1
            }
        }

        val merged = rawApps.map { app ->
            val rule = ruleMap[app.packageName]
            app.copy(
                blockMode = rule?.blockMode ?: BlockMode.NONE,
                isRuleActive = rule?.isActive == true,
                scheduledCount = scheduleCountMap[app.packageName] ?: 0
            )
        }.sortedWith(
            compareByDescending<InstalledAppInfo> { it.blockMode.isBlocked }
                .thenBy { it.isSystemApp }
                .thenByDescending { it.isRunningInBackground }
                .thenBy { it.appName.lowercase() }
        )

        val q = query.trim().lowercase()
        val filtered = merged.filter { app ->
            val matchesQuery = q.isEmpty() ||
                app.appName.lowercase().contains(q) ||
                app.packageName.lowercase().contains(q)
            val matchesFilter = when (filter) {
                AppFilterTab.ALL -> true
                AppFilterTab.BLOCKED -> app.blockMode.isBlocked
                AppFilterTab.INTERNET_CAPABLE -> app.hasInternetPermission
                AppFilterTab.USER_APPS -> !app.isSystemApp
                AppFilterTab.SYSTEM_APPS -> app.isSystemApp
            }
            matchesQuery && matchesFilter
        }

        Triple(merged, filtered, Pair(rules, schedules))
    }

    private val selectionAndSystemFlow = combine(
        _isLoadingApps,
        _selectedPackages,
        _isMultiSelectMode,
        _searchQuery,
        _activeFilter
    ) { loading, selected, multi, query, filter ->
        SelectionUiMeta(loading, selected, multi, query, filter)
    }

    private val telemetryFlow = combine(
        _memorySnapshot,
        FirewallStateHolder.state,
        logsFlow,
        _hasVpnConsent,
        _hasUsageStats
    ) { mem, runtime, logs, vpnOk, usageOk ->
        TelemetryMeta(mem, runtime, logs, vpnOk, usageOk)
    }

    private val chartsFlow = combine(
        telemetryPointsFlow,
        totalFreedFlow
    ) { points, totalFreed ->
        points to totalFreed
    }

    val uiState: StateFlow<AppShieldUiState> = combine(
        appsStateFlow,
        selectionAndSystemFlow,
        telemetryFlow,
        chartsFlow,
        combine(_pendingVpnIntent, _snackbarMessage) { vpn, snack -> vpn to snack }
    ) { appsData, selMeta, telMeta, chartsData, events ->
        val (mergedApps, filteredApps, rulesAndSchedules) = appsData
        val (rules, schedules) = rulesAndSchedules
        val (points, totalFreed) = chartsData
        val (pendingVpn, snackbar) = events
        AppShieldUiState(
            isLoadingApps = selMeta.isLoading,
            apps = mergedApps,
            filteredApps = filteredApps,
            selectedPackages = selMeta.selectedPackages,
            isMultiSelectMode = selMeta.isMultiSelectMode,
            searchQuery = selMeta.searchQuery,
            activeFilter = selMeta.activeFilter,
            rules = rules,
            schedules = schedules,
            logs = telMeta.logs,
            memorySnapshot = telMeta.memorySnapshot,
            runtimeState = telMeta.runtimeState,
            hasVpnConsent = telMeta.hasVpnConsent,
            hasUsageStatsPermission = telMeta.hasUsageStatsPermission,
            pendingVpnIntent = pendingVpn,
            snackbarMessage = snackbar,
            telemetryPoints = points,
            totalFreedMb = totalFreed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppShieldUiState()
    )

    init {
        refreshInstalledApps()
        startMemoryPolling()
        viewModelScope.launch {
            repository.syncServiceWithCurrentRules()
        }
    }

    private var lastSampledAt = 0L

    fun refreshInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val list = repository.loadInstalledApps()
            _rawApps.value = list
            _memorySnapshot.value = repository.getMemorySnapshot()
            refreshPermissionStates()
            recordCurrentSample(force = false)
            _isLoadingApps.value = false
        }
    }

    fun captureTelemetrySampleNow() {
        viewModelScope.launch {
            val list = repository.loadInstalledApps()
            _rawApps.value = list
            _memorySnapshot.value = repository.getMemorySnapshot()
            recordCurrentSample(force = true)
            _snackbarMessage.value = "تم تحديث قراءات الذاكرة والرسوم البيانية"
        }
    }

    private suspend fun recordCurrentSample(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSampledAt < 12_000L) return
        lastSampledAt = now
        val currentState = uiState.value
        val blockedApps = currentState.apps.filter { it.blockMode.isBlocked && it.isRuleActive }
        val blockedRam = blockedApps.sumOf { it.estimatedRamMb }
        repository.recordTelemetrySnapshot(
            blockedAppsRamMb = blockedRam,
            blockedAppsCount = blockedApps.size,
            snapshotOverride = _memorySnapshot.value
        )
    }

    fun refreshPermissionStates() {
        _hasVpnConsent.value = repository.prepareVpnIntent() == null
        _hasUsageStats.value = repository.hasUsageStatsPermission()
        _memorySnapshot.value = repository.getMemorySnapshot()
    }

    private fun startMemoryPolling() {
        viewModelScope.launch {
            while (isActive) {
                _memorySnapshot.value = repository.getMemorySnapshot()
                recordCurrentSample(force = false)
                delay(5000L)
            }
        }
    }

    suspend fun loadAppIcon(packageName: String): ImageBitmap? =
        repository.appsProvider.loadAppIcon(packageName)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: AppFilterTab) {
        _activeFilter.value = filter
    }

    fun toggleMultiSelectMode(enabled: Boolean) {
        _isMultiSelectMode.value = enabled
        if (!enabled) {
            _selectedPackages.value = emptySet()
        }
    }

    fun togglePackageSelection(packageName: String) {
        _selectedPackages.update { current ->
            val next = if (packageName in current) current - packageName else current + packageName
            if (next.isNotEmpty()) _isMultiSelectMode.value = true
            next
        }
    }

    fun selectAllVisible(apps: List<InstalledAppInfo>) {
        _isMultiSelectMode.value = true
        _selectedPackages.value = apps.map { it.packageName }.toSet()
    }

    fun clearSelection() {
        _selectedPackages.value = emptySet()
        _isMultiSelectMode.value = false
    }

    fun applyImmediateBlock(
        targetApps: List<InstalledAppInfo>,
        blockMode: BlockMode
    ) {
        viewModelScope.launch {
            val outcome = repository.applyImmediateBlock(targetApps, blockMode)
            _memorySnapshot.value = repository.getMemorySnapshot()
            refreshPermissionStates()
            clearSelection()

            if (outcome.needsVpnConsent != null) {
                _pendingVpnIntent.value = outcome.needsVpnConsent
                _snackbarMessage.value = "يرجى الموافقة على إذن الشبكة (VPN) لتفعيل حجب الإنترنت"
            } else {
                val msg = when (blockMode) {
                    BlockMode.NONE -> "تم إلغاء الحجب عن ${targetApps.size} تطبيق"
                    BlockMode.MEMORY_ONLY -> {
                        val freed = outcome.sweepResult?.freedMb ?: 0L
                        "تم تفعيل حجب الذاكرة فوراً لـ ${targetApps.size} تطبيق (تحرير ${freed} MB)"
                    }
                    BlockMode.INTERNET_ONLY -> "تم تفعيل حجب الإنترنت فوراً لـ ${targetApps.size} تطبيق"
                    BlockMode.BOTH -> {
                        val freed = outcome.sweepResult?.freedMb ?: 0L
                        "تم تفعيل حجب الذاكرة والإنترنت فوراً لـ ${targetApps.size} تطبيق"
                    }
                }
                _snackbarMessage.value = msg
            }
            refreshInstalledApps()
        }
    }

    fun createSchedule(
        title: String,
        targetApps: List<InstalledAppInfo>,
        blockMode: BlockMode,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        hasEndTime: Boolean,
        repeatDaily: Boolean,
        daysMask: Int
    ) {
        viewModelScope.launch {
            repository.createSchedule(
                title = title,
                apps = targetApps,
                blockMode = blockMode,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                hasEndTime = hasEndTime,
                repeatDaily = repeatDaily,
                daysMask = daysMask
            )
            clearSelection()
            _snackbarMessage.value =
                "تمت جدولة ${targetApps.size} تطبيق (${blockMode.displayLabelAr()}) بنجاح"
        }
    }

    fun toggleSchedule(schedule: BlockScheduleEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleScheduleEnabled(schedule, enabled)
            _snackbarMessage.value = if (enabled) "تم تفعيل الجدولة" else "تم إيقاف الجدولة مؤقتاً"
        }
    }

    fun deleteSchedule(schedule: BlockScheduleEntity) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
            _snackbarMessage.value = "تم حذف الجدولة"
        }
    }

    fun runScheduleNow(schedule: BlockScheduleEntity) {
        viewModelScope.launch {
            val outcome = repository.runScheduleNow(schedule, uiState.value.apps)
            if (outcome.needsVpnConsent != null) {
                _pendingVpnIntent.value = outcome.needsVpnConsent
            }
            _snackbarMessage.value = "تم تطبيق قواعد الجدولة (${schedule.title}) فوراً"
        }
    }

    fun toggleMasterShield(enable: Boolean) {
        viewModelScope.launch {
            val consent = repository.toggleMasterShield(enable)
            refreshPermissionStates()
            if (consent != null) {
                _pendingVpnIntent.value = consent
            } else {
                _snackbarMessage.value = if (enable) "تم تفعيل درع الحماية" else "تم إيقاف درع الحماية"
            }
        }
    }

    fun enforceMemorySweepNow() {
        viewModelScope.launch {
            val activeMemCount = uiState.value.rules.count { it.isActive && it.blockMode.blocksMemory }
            if (activeMemCount == 0) {
                _snackbarMessage.value = "لا توجد تطبيقات محجوبة عن الذاكرة حالياً. حدد تطبيقات أولاً."
                return@launch
            }
            val result = repository.enforceMemorySweepAll()
            _memorySnapshot.value = result.snapshotAfter
            _snackbarMessage.value =
                "تم فحص ${result.targetedCount} تطبيق وتنظيف الذاكرة (${result.freedMb} MB متاحة إضافية)"
            refreshInstalledApps()
        }
    }

    fun requestVpnConsent() {
        val intent = repository.prepareVpnIntent()
        if (intent != null) {
            _pendingVpnIntent.value = intent
        } else {
            _hasVpnConsent.value = true
            viewModelScope.launch { repository.syncServiceWithCurrentRules() }
            _snackbarMessage.value = "إذن جدار الحماية مفعّل بالفعل"
        }
    }

    fun onVpnConsentHandled(granted: Boolean) {
        _pendingVpnIntent.value = null
        refreshPermissionStates()
        if (granted) {
            viewModelScope.launch {
                repository.syncServiceWithCurrentRules()
                _snackbarMessage.value = "تم تفعيل جدار الحماية (VPN) لحجب الإنترنت"
            }
        } else {
            _snackbarMessage.value = "لم يتم منح إذن جدار الحماية؛ قواعد الإنترنت بانتظار الموافقة"
        }
    }

    fun openUsageAccessSettings() {
        repository.openUsageAccessSettings()
    }

    fun openAppDetailsSettings(packageName: String) {
        repository.openAppDetailsSettings(packageName)
    }

    fun clearAllRules() {
        viewModelScope.launch {
            repository.clearAllRules()
            _snackbarMessage.value = "تم فك الحجب عن جميع التطبيقات"
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _snackbarMessage.value = "تم مسح سجل النشاط"
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }
}

private data class SelectionUiMeta(
    val isLoading: Boolean,
    val selectedPackages: Set<String>,
    val isMultiSelectMode: Boolean,
    val searchQuery: String,
    val activeFilter: AppFilterTab
)

private data class TelemetryMeta(
    val memorySnapshot: MemorySnapshot,
    val runtimeState: FirewallRuntimeState,
    val logs: List<BlockLogEntity>,
    val hasVpnConsent: Boolean,
    val hasUsageStatsPermission: Boolean
)
