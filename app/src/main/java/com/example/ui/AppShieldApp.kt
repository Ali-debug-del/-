package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.InstalledAppInfo
import com.example.ui.components.BlockConfigBottomSheet
import com.example.ui.components.BlockTimingMode
import com.example.ui.screens.AppsBlockerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LogsAndSystemScreen
import com.example.ui.screens.MemoryChartsScreen
import com.example.ui.screens.SchedulesScreen

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_APPS = "apps"
const val ROUTE_CHARTS = "charts"
const val ROUTE_SCHEDULES = "schedules"
const val ROUTE_LOGS = "logs"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopLevelDestinations = listOf(
    NavDestinationItem(
        route = ROUTE_DASHBOARD,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Rounded.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    NavDestinationItem(
        route = ROUTE_APPS,
        labelRes = R.string.nav_apps,
        selectedIcon = Icons.Rounded.Apps,
        unselectedIcon = Icons.Outlined.Apps,
        testTag = "nav_apps"
    ),
    NavDestinationItem(
        route = ROUTE_CHARTS,
        labelRes = R.string.nav_charts,
        selectedIcon = Icons.Rounded.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
        testTag = "nav_charts"
    ),
    NavDestinationItem(
        route = ROUTE_SCHEDULES,
        labelRes = R.string.nav_schedules,
        selectedIcon = Icons.Rounded.Schedule,
        unselectedIcon = Icons.Outlined.Schedule,
        testTag = "nav_schedules"
    ),
    NavDestinationItem(
        route = ROUTE_LOGS,
        labelRes = R.string.nav_logs,
        selectedIcon = Icons.Rounded.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_logs"
    )
)

@Composable
fun AppShieldApp(viewModel: AppShieldViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    var sheetApps by remember { mutableStateOf<List<InstalledAppInfo>?>(null) }
    var sheetInitialTiming by remember { mutableStateOf(BlockTimingMode.IMMEDIATE) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onVpnConsentHandled(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(uiState.pendingVpnIntent) {
        val intent = uiState.pendingVpnIntent
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissionStates()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        onPauseOrDispose {}
    }

    fun navigateToRoute(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            TopLevelDestinations.forEach { dest ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == dest.route
                                } == true
                                val badgeCount = when (dest.route) {
                                    ROUTE_APPS -> uiState.rules.count { it.isActive }
                                    ROUTE_SCHEDULES -> uiState.schedules.count { it.isEnabled }
                                    else -> 0
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToRoute(dest.route) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (badgeCount > 0) {
                                                    Badge { Text("$badgeCount") }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = stringResource(dest.labelRes)
                                            )
                                        }
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            TopLevelDestinations.forEach { dest ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == dest.route
                                } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToRoute(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_DASHBOARD,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(ROUTE_DASHBOARD) {
                            DashboardScreen(
                                uiState = uiState,
                                loadIcon = { viewModel.loadAppIcon(it) },
                                onToggleMasterShield = { viewModel.toggleMasterShield(it) },
                                onEnforceMemorySweep = { viewModel.enforceMemorySweepNow() },
                                onNavigateToApps = { navigateToRoute(ROUTE_APPS) },
                                onNavigateToCharts = { navigateToRoute(ROUTE_CHARTS) },
                                onNavigateToSchedules = { navigateToRoute(ROUTE_SCHEDULES) },
                                onNavigateToLogs = { navigateToRoute(ROUTE_LOGS) }
                            )
                        }

                        composable(ROUTE_APPS) {
                            AppsBlockerScreen(
                                uiState = uiState,
                                loadIcon = { viewModel.loadAppIcon(it) },
                                onSearchChanged = viewModel::onSearchQueryChanged,
                                onFilterSelected = viewModel::onFilterSelected,
                                onToggleMultiSelect = viewModel::toggleMultiSelectMode,
                                onTogglePackageSelection = viewModel::togglePackageSelection,
                                onSelectAllVisible = viewModel::selectAllVisible,
                                onClearSelection = viewModel::clearSelection,
                                onRefreshApps = viewModel::refreshInstalledApps,
                                onConfigureApps = { apps ->
                                    sheetInitialTiming = BlockTimingMode.IMMEDIATE
                                    sheetApps = apps
                                }
                            )
                        }

                        composable(ROUTE_CHARTS) {
                            MemoryChartsScreen(
                                uiState = uiState,
                                loadIcon = { viewModel.loadAppIcon(it) },
                                onCaptureSampleNow = viewModel::captureTelemetrySampleNow,
                                onEnforceMemorySweep = viewModel::enforceMemorySweepNow,
                                onConfigureApp = { app ->
                                    sheetInitialTiming = BlockTimingMode.IMMEDIATE
                                    sheetApps = listOf(app)
                                },
                                onNavigateToApps = { navigateToRoute(ROUTE_APPS) }
                            )
                        }

                        composable(ROUTE_SCHEDULES) {
                            SchedulesScreen(
                                uiState = uiState,
                                loadIcon = { viewModel.loadAppIcon(it) },
                                onToggleSchedule = viewModel::toggleSchedule,
                                onDeleteSchedule = viewModel::deleteSchedule,
                                onRunScheduleNow = viewModel::runScheduleNow,
                                onCreateScheduleWithApps = { apps ->
                                    sheetInitialTiming = BlockTimingMode.SCHEDULED
                                    sheetApps = apps
                                }
                            )
                        }

                        composable(ROUTE_LOGS) {
                            LogsAndSystemScreen(
                                uiState = uiState,
                                hasNotificationPermission = hasNotificationPermission,
                                onRequestVpnConsent = viewModel::requestVpnConsent,
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onOpenUsageAccessSettings = viewModel::openUsageAccessSettings,
                                onClearAllRules = viewModel::clearAllRules,
                                onClearLogs = viewModel::clearLogs
                            )
                        }
                    }
                }
            }

            val currentSheetApps = sheetApps
            if (currentSheetApps != null && currentSheetApps.isNotEmpty()) {
                BlockConfigBottomSheet(
                    targetApps = currentSheetApps,
                    initialTimingMode = sheetInitialTiming,
                    loadIcon = { viewModel.loadAppIcon(it) },
                    onDismiss = { sheetApps = null },
                    onApplyImmediate = { mode ->
                        sheetApps = null
                        viewModel.applyImmediateBlock(currentSheetApps, mode)
                    },
                    onCreateSchedule = { title, mode, sh, sm, eh, em, hasEnd, repeat, daysMask ->
                        sheetApps = null
                        viewModel.createSchedule(
                            title = title,
                            targetApps = currentSheetApps,
                            blockMode = mode,
                            startHour = sh,
                            startMinute = sm,
                            endHour = eh,
                            endMinute = em,
                            hasEndTime = hasEnd,
                            repeatDaily = repeat,
                            daysMask = daysMask
                        )
                    },
                    onOpenSystemSettings = viewModel::openAppDetailsSettings
                )
            }
        }
    }
}
