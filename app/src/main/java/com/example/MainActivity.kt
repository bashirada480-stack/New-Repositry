package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.NavigationTab
import com.example.ui.components.ActivityLogView
import com.example.ui.components.DeviceDetailDialog
import com.example.ui.components.RenameDeviceDialog
import com.example.ui.screens.ArchitectureGuideDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceAgentScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                // Ping message notification handler
                LaunchedEffect(uiState.pingNotificationMessage) {
                    uiState.pingNotificationMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.dismissPingMessage()
                    }
                }

                // Android Back button handling
                BackHandler(
                    enabled = uiState.selectedDeviceForDetails != null ||
                            uiState.deviceToRename != null ||
                            uiState.showArchitectureModal ||
                            uiState.activeTab != NavigationTab.DASHBOARD
                ) {
                    when {
                        uiState.selectedDeviceForDetails != null -> viewModel.closeDeviceDetails()
                        uiState.deviceToRename != null -> viewModel.closeRenameDialog()
                        uiState.showArchitectureModal -> viewModel.setShowArchitectureModal(false)
                        uiState.activeTab != NavigationTab.DASHBOARD -> viewModel.selectTab(NavigationTab.DASHBOARD)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(CyanPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "MyDevice",
                                            tint = CyanPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "MyDevice",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Control Center • Secure Telemetry",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            actions = {
                                // User account pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldAccent)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Owner",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.setShowArchitectureModal(true) },
                                    modifier = Modifier.testTag("topbar_specs_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Architecture Specs",
                                        tint = CyanPrimary
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_nav_bar"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = uiState.activeTab == NavigationTab.DASHBOARD,
                                onClick = { viewModel.selectTab(NavigationTab.DASHBOARD) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Dashboard,
                                        contentDescription = "Dashboard"
                                    )
                                },
                                label = { Text("Dashboard") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPrimary,
                                    indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_dashboard")
                            )

                            NavigationBarItem(
                                selected = uiState.activeTab == NavigationTab.DEVICE_AGENT,
                                onClick = { viewModel.selectTab(NavigationTab.DEVICE_AGENT) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = "Device Agent"
                                    )
                                },
                                label = { Text("Agent") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPrimary,
                                    indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_agent")
                            )

                            NavigationBarItem(
                                selected = uiState.activeTab == NavigationTab.AUDIT_LOGS,
                                onClick = { viewModel.selectTab(NavigationTab.AUDIT_LOGS) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (uiState.events.isNotEmpty()) {
                                                Badge { Text("${uiState.events.size.coerceAtMost(99)}") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ListAlt,
                                            contentDescription = "Audit Logs"
                                        )
                                    }
                                },
                                label = { Text("Audit Logs") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPrimary,
                                    indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_logs")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (uiState.activeTab) {
                            NavigationTab.DASHBOARD -> {
                                DashboardScreen(
                                    state = uiState,
                                    onViewDeviceDetails = { dev -> viewModel.openDeviceDetails(dev) },
                                    onPingDevice = { id -> viewModel.pingDevice(id) },
                                    onTriggerSync = { viewModel.triggerSync() },
                                    onOpenArchitectureModal = { viewModel.setShowArchitectureModal(true) }
                                )
                            }
                            NavigationTab.DEVICE_AGENT -> {
                                uiState.currentDevice?.let { dev ->
                                    DeviceAgentScreen(
                                        device = dev,
                                        isSyncing = uiState.isSyncing,
                                        onTriggerSync = { viewModel.triggerSync() },
                                        onToggleLocationSharing = { enabled -> viewModel.toggleLocationSharing(enabled) },
                                        onToggleAppUsageSharing = { enabled -> viewModel.toggleAppUsageSharing(enabled) },
                                        onToggleScreenSharing = { active -> viewModel.toggleScreenSharing(active) }
                                    )
                                }
                            }
                            NavigationTab.AUDIT_LOGS -> {
                                ActivityLogView(
                                    events = uiState.events,
                                    onClearLogs = { viewModel.clearEvents() },
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            NavigationTab.SECURITY_SPEC -> {
                                DashboardScreen(
                                    state = uiState,
                                    onViewDeviceDetails = { dev -> viewModel.openDeviceDetails(dev) },
                                    onPingDevice = { id -> viewModel.pingDevice(id) },
                                    onTriggerSync = { viewModel.triggerSync() },
                                    onOpenArchitectureModal = { viewModel.setShowArchitectureModal(true) }
                                )
                            }
                        }

                        // Device Detailed Dialog
                        uiState.selectedDeviceForDetails?.let { dev ->
                            DeviceDetailDialog(
                                device = dev,
                                onDismiss = { viewModel.closeDeviceDetails() },
                                onRename = { viewModel.openRenameDialog(dev) },
                                onSync = { viewModel.triggerSync() },
                                onPing = { viewModel.pingDevice(dev.deviceId) },
                                onRevoke = { viewModel.revokeDevice(dev.deviceId) }
                            )
                        }

                        // Rename Device Dialog
                        uiState.deviceToRename?.let { dev ->
                            RenameDeviceDialog(
                                device = dev,
                                onDismiss = { viewModel.closeRenameDialog() },
                                onConfirm = { newName -> viewModel.confirmRename(dev.deviceId, newName) }
                            )
                        }

                        // Architecture & Security Specs Dialog
                        if (uiState.showArchitectureModal) {
                            ArchitectureGuideDialog(
                                onDismiss = { viewModel.setShowArchitectureModal(false) }
                            )
                        }
                    }
                }
            }
        }
    }
}
