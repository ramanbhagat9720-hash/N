package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.telephony.TelephonyRoleManager
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AICallAssistantTheme
import com.example.ui.viewmodel.MainViewModel

private sealed class NavDestination(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    data object Home : NavDestination("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home")
    data object Calls : NavDestination("calls", R.string.nav_calls, Icons.Filled.Call, Icons.Outlined.Call, "nav_calls")
    data object Assistant : NavDestination("ai_assistant", R.string.nav_assistant, Icons.Filled.Psychology, Icons.Outlined.Psychology, "nav_ai_assistant")
    data object History : NavDestination("history", R.string.nav_history, Icons.Filled.History, Icons.Outlined.History, "nav_history")
    data object Settings : NavDestination("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

private val TopDestinations = listOf(
    NavDestination.Home,
    NavDestination.Calls,
    NavDestination.Assistant,
    NavDestination.History,
    NavDestination.Settings
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AICallAssistantTheme {
                AICallAssistantApp()
            }
        }
    }
}

@Composable
fun AICallAssistantApp() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshSystemStatus()
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || result.resultCode == Activity.RESULT_CANCELED) {
            viewModel.refreshSystemStatus()
        }
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavDestination.Home.route

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TopDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelResId)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
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
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TopDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelResId)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = NavDestination.Home.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        composable(NavDestination.Home.route) {
                            HomeScreen(
                                uiState = uiState,
                                onToggleAssistant = viewModel::setAssistantEnabled,
                                onTriggerIncomingCall = viewModel::triggerIncomingCallGreeter,
                                onNavigateToCalls = { navigateTo(NavDestination.Calls.route) },
                                onNavigateToAssistant = { navigateTo(NavDestination.Assistant.route) },
                                onNavigateToHistory = { navigateTo(NavDestination.History.route) },
                                onNavigateToSettings = { navigateTo(NavDestination.Settings.route) }
                            )
                        }
                        composable(NavDestination.Calls.route) {
                            CallsScreen(
                                uiState = uiState,
                                onTriggerIncomingCall = viewModel::triggerIncomingCallGreeter,
                                onAnswerMyself = viewModel::answerMyself,
                                onLetAiTalk = viewModel::letAiTalk,
                                onRejectCall = viewModel::rejectCall,
                                onSilentMode = viewModel::silentMode,
                                onTakeOverCall = viewModel::takeOverCall,
                                onStopAi = viewModel::stopAi,
                                onToggleMuteAi = viewModel::toggleMuteAi,
                                onEndCall = viewModel::endCall,
                                onStartListening = viewModel::startCallerListening,
                                onSubmitCallerUtterance = viewModel::submitCallerSpeech,
                                onApprovePermission = viewModel::approvePermission,
                                onDenyPermission = viewModel::denyPermission,
                                onDismissSummary = viewModel::dismissCompletedSummary,
                                onDialCallback = { num ->
                                    TelephonyRoleManager.launchDialerCallback(context, num)
                                },
                                onSaveTrustedContact = viewModel::saveTrustedContact,
                                onDeleteTrustedContact = viewModel::deleteTrustedContact,
                                onLoadDeviceContacts = viewModel::loadDeviceContacts,
                                onBlockNumber = viewModel::blockNumber,
                                onUnblockNumber = viewModel::unblockNumber
                            )
                        }
                        composable(NavDestination.Assistant.route) {
                            AiAssistantScreen(
                                uiState = uiState,
                                onSaveConfig = { updated -> viewModel.updateConfig { updated } },
                                onTogglePreferredLanguage = viewModel::togglePreferredLanguage,
                                onSaveKnowledge = viewModel::saveKnowledge,
                                onDeleteKnowledge = viewModel::deleteKnowledge,
                                onUpdateVoiceTestInput = viewModel::updateVoiceTestInput,
                                onRunVoiceTest = viewModel::runVoiceTest,
                                onStartVoiceTestMic = viewModel::startVoiceTestMic,
                                onStopVoiceTestAudio = viewModel::stopVoiceTestAudio
                            )
                        }
                        composable(NavDestination.History.route) {
                            HistoryScreen(
                                uiState = uiState,
                                onToggleCallbackCompleted = viewModel::setCallbackCompleted,
                                onDeleteRecord = viewModel::deleteCallRecord,
                                onDialCallback = { num ->
                                    TelephonyRoleManager.launchDialerCallback(context, num)
                                }
                            )
                        }
                        composable(NavDestination.Settings.route) {
                            SettingsScreen(
                                uiState = uiState,
                                onToggleAssistant = viewModel::setAssistantEnabled,
                                onSetVoiceProfile = viewModel::setVoiceProfile,
                                onSetAnsweringMode = viewModel::setAnsweringMode,
                                onSaveConfig = { updated -> viewModel.updateConfig { updated } },
                                onRequestPermissions = {
                                    permissionsLauncher.launch(TelephonyRoleManager.requiredRuntimePermissions())
                                },
                                onRequestCallScreeningRole = {
                                    TelephonyRoleManager.createCallScreeningRoleIntent(context)?.let {
                                        roleLauncher.launch(it)
                                    }
                                },
                                onRequestDefaultDialerRole = {
                                    TelephonyRoleManager.createDefaultDialerRoleIntent(context)?.let {
                                        roleLauncher.launch(it)
                                    }
                                },
                                onLaunchMmiCode = { mmi ->
                                    TelephonyRoleManager.openDialerWithMmiCode(context, mmi)
                                },
                                onEnableRecordingWithConsent = viewModel::enableCallRecordingWithConsent,
                                onDisableRecording = viewModel::disableCallRecording,
                                onDeleteAllData = viewModel::deleteAllLocalData
                            )
                        }
                    }
                }
            }
        }
    }
}
