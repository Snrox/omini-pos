package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.viewmodel.POSViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun POSAppContent(viewModel: POSViewModel) {
    val screen by viewModel.currentScreen.collectAsState()

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "ScreenTransition"
    ) { current ->
        when (current) {
            "SETUP" -> SetupScreen(viewModel)
            "LOGIN" -> LoginScreen(viewModel)
            "DASHBOARD" -> DashboardScreen(viewModel)
        }
    }
}

// --- 1. FIRST TIME SETUP SCREEN ---
@Composable
fun SetupScreen(viewModel: POSViewModel) {
    val scope = rememberCoroutineScope()
    var selectedMode by remember { mutableStateOf<DeviceMode?>(null) }
    var manualIp by remember { mutableStateOf("") }
    val discoveredHosts by viewModel.discoveredHosts.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .statusBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Canvas Drawing - Store Hub Logo
            Canvas(modifier = Modifier.size(100.dp)) {
                drawRoundRect(
                    color = Color(0xFF34D399),
                    topLeft = Offset(10f, 10f),
                    size = Size(200f, 200f),
                    cornerRadius = CornerRadius(40f, 40f),
                    style = Stroke(width = 12f)
                )
                drawCircle(
                    color = Color(0xFF6366F1),
                    radius = 35f,
                    center = Offset(110f, 110f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "OmniPOS Core Setup",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Configure terminal role. Master server maintains inventory database. Client terminals sync transaction queues completely offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SERVER CARD
                OutlinedCard(
                    onClick = { selectedMode = DeviceMode.SERVER_MAIN },
                    border = BorderStroke(
                        width = if (selectedMode == DeviceMode.SERVER_MAIN) 3.dp else 1.dp,
                        color = if (selectedMode == DeviceMode.SERVER_MAIN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp)
                        .testTag("mode_server_card"),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedMode == DeviceMode.SERVER_MAIN) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = "Server", modifier = Modifier.size(40.dp), tint = Color(0xFF6366F1))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Main Device", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Local DB Server Host", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                }

                // CLIENT CARD
                OutlinedCard(
                    onClick = { selectedMode = DeviceMode.CLIENT_CHILD },
                    border = BorderStroke(
                        width = if (selectedMode == DeviceMode.CLIENT_CHILD) 3.dp else 1.dp,
                        color = if (selectedMode == DeviceMode.CLIENT_CHILD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp)
                        .testTag("mode_client_card"),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedMode == DeviceMode.CLIENT_CHILD) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.TabletAndroid, contentDescription = "Client", modifier = Modifier.size(40.dp), tint = Color(0xFF34D399))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Child Device", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Cashier/Waiter Terminal", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (selectedMode == DeviceMode.CLIENT_CHILD) {
                // Client config: discovered hosts & IP entry
                Text("Discovered Servers on Local WiFi Network:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                if (discoveredHosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Listening for mDNS broadcasts on network...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    discoveredHosts.forEach { host ->
                        Card(
                            onClick = {
                                manualIp = host.host.hostAddress
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NetworkWifi, contentDescription = null, tint = Color(0xFF34D399))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(host.serviceName, fontWeight = FontWeight.Bold)
                                        Text("IP: ${host.host.hostAddress}:${host.port}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Icon(Icons.Default.Link, contentDescription = null)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = manualIp,
                    onValueChange = { manualIp = it },
                    label = { Text("Enter Server Host IP (or select discovered above)") },
                    modifier = Modifier.fillMaxWidth().testTag("server_ip_input"),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Router, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    selectedMode?.let { mode ->
                        if (mode == DeviceMode.CLIENT_CHILD) {
                            if (manualIp.isNotEmpty()) {
                                viewModel.pairWithServerHost(manualIp)
                                viewModel.selectDeviceMode(mode)
                            }
                        } else {
                            viewModel.selectDeviceMode(mode)
                        }
                    }
                },
                enabled = selectedMode != null && (selectedMode == DeviceMode.SERVER_MAIN || manualIp.isNotEmpty()),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("setup_continue_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm & Continue Setup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

// --- 2. SECURITY PIN-PAD LOGIN SCREEN ---
@Composable
fun LoginScreen(viewModel: POSViewModel) {
    val scope = rememberCoroutineScope()
    var enteredPin by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }
    val deviceMode by viewModel.deviceMode.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 360.dp)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // Minimal role indicator badge
            Box(
                modifier = Modifier
                    .background(
                        if (deviceMode == DeviceMode.SERVER_MAIN) Color(0xFF6366F1).copy(alpha = 0.15f) else Color(0xFF34D399).copy(alpha = 0.15f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (deviceMode == DeviceMode.SERVER_MAIN) "MASTER SERVER MODE" else "CLIENT CHILD TERMINAL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (deviceMode == DeviceMode.SERVER_MAIN) Color(0xFF818CF8) else Color(0xFF34D399)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Enter your PIN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Indicator Dots (Exactly like the screenshot: large dots with space)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..4) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                if (enteredPin.length >= i) Color.White else Color(0xFF2E2E3E)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (loginError) {
                Text(
                    text = "Incorrect access code. Try again.",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pin Pad Grid (1-9, . , 0, backspace)
            val keys = listOf(
                "1", "2", "3",
                "4", "5", "6",
                "7", "8", "9",
                ".", "0", "⌫"
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                items(keys) { key ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (key.isEmpty() || key == ".") Color.Transparent
                                else Color(0xFF1F1F2C)
                            )
                            .clickable(enabled = key.isNotEmpty() && key != ".") {
                                loginError = false
                                when (key) {
                                    "⌫" -> {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                        }
                                    }
                                    else -> {
                                        if (enteredPin.length < 4) {
                                            enteredPin += key
                                        }
                                        if (enteredPin.length == 4) {
                                            viewModel.loginWithPin(enteredPin) { success ->
                                                if (!success) {
                                                    loginError = true
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            .testTag("pin_key_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "⌫") {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "delete",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(
                onClick = { viewModel.setScreen("SETUP") },
                modifier = Modifier.testTag("change_device_role_btn")
            ) {
                Text(
                    text = "Change Terminal Mode / Host Configuration",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun NetworkManagerDialog(viewModel: POSViewModel, onDismiss: () -> Unit) {
    val syncState by viewModel.syncState.collectAsState()
    val discoveredHosts by viewModel.discoveredHosts.collectAsState()
    val discoveredClients by viewModel.discoveredClients.collectAsState()
    val pairedIp by viewModel.pairedIp.collectAsState()
    val deviceMode by viewModel.deviceMode.collectAsState()

    var broadcasting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("OmniPOS Wi-Fi Manager", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Terminal Mode: ${if (deviceMode == DeviceMode.SERVER_MAIN) "Main (Server)" else "Child (Client)"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Status: $syncState",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (pairedIp != null) {
                            Text(
                                text = "Paired Host IP: $pairedIp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (deviceMode == DeviceMode.CLIENT_CHILD) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Broadcast My Signal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Allows Main Server to locate this child terminal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = broadcasting,
                            onCheckedChange = {
                                broadcasting = it
                                if (it) {
                                    viewModel.startBroadcastingPresence()
                                }
                            }
                        )
                    }
                }

                Text("Available Servers (Hosts)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (discoveredHosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Scanning for host signals...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        discoveredHosts.forEach { host ->
                            val hostIp = host.host?.hostAddress ?: ""
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(host.serviceName ?: "Unknown Host", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("IP: $hostIp | Port: ${host.port}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                val isPaired = pairedIp == hostIp
                                Button(
                                    onClick = { if (!isPaired && hostIp.isNotEmpty()) viewModel.pairWithServerHost(hostIp) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPaired) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(if (isPaired) "Paired" else "Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (deviceMode == DeviceMode.SERVER_MAIN) {
                    Text("Discovered Child Terminals", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (discoveredClients.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Waiting for child broadcasts...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            discoveredClients.forEach { client ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(client.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("IP: ${client.ip}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF34D399).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Signal OK", fontSize = 10.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TopHeaderBar(viewModel: POSViewModel, isTablet: Boolean) {
    val syncState by viewModel.syncState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val deviceMode by viewModel.deviceMode.collectAsState()

    var showNetworkManager by remember { mutableStateOf(false) }

    val terminalModeName = if (deviceMode == DeviceMode.SERVER_MAIN) "Main Terminal 01" else "Child Terminal"
    val userInitials = remember(currentUser) {
        val name = currentUser?.name ?: "Cashier"
        name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
    }

    if (showNetworkManager) {
        NetworkManagerDialog(viewModel = viewModel, onDismiss = { showNetworkManager = false })
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                drawLine(
                    color = Color(0xFF2E2E3E),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2f
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF6366F1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Storefront,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = terminalModeName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (syncState.contains("ACTIVE") || syncState.contains("MASTER") || syncState == "MASTER_ONLINE") Color(0xFF10B981) else Color(0xFFF97316)
                            )
                    )
                    Text(
                        text = if (deviceMode == DeviceMode.SERVER_MAIN) "Local Server Active" else "Synced: $syncState",
                        fontSize = 11.sp,
                        maxLines = 1,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = { showNetworkManager = true },
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = "Wi-Fi signals",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    Icons.Default.LockOpen,
                    contentDescription = "Lock",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitials,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// --- 3. MASTER RESPONSIVE DASHBOARD FRAME ---
@Composable
fun DashboardScreen(viewModel: POSViewModel) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val subScreen by viewModel.subScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        if (isTablet) {
            // Elegant Side Navigation Panel (Tablet View)
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), RoundedCornerShape(0.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Canvas(modifier = Modifier.size(32.dp)) {
                        drawCircle(color = Color(0xFF34D399), radius = size.width / 2)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("OmniPOS Host", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(currentUser?.name ?: "Cashier", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Items
                val navItems = listOf(
                    NavItem("Retail checkout", "RETAIL", Icons.Default.ShoppingCart),
                    NavItem("Restaurant monitor", "RESTAURANT", Icons.Default.Restaurant),
                    NavItem("Kitchen Display", "KITCHEN_DISPLAY", Icons.Default.Monitor),
                    NavItem("Inventory stock", "INVENTORY", Icons.Default.Warehouse),
                    NavItem("Sales & Reports", "REPORTS", Icons.Default.BarChart),
                    NavItem("Terminal Receipt", "PRINTER", Icons.Default.Assignment),
                    NavItem("Control Settings", "SETTINGS", Icons.Default.Settings)
                )

                navItems.forEach { item ->
                    NavigationRow(
                        item = item,
                        isActive = subScreen == item.subName,
                        onClick = { viewModel.setSubScreen(item.subName) }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().testTag("nav_lock_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lock Terminal", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Sub Screen Area
        Column(modifier = Modifier.weight(1f)) {
            TopHeaderBar(viewModel = viewModel, isTablet = isTablet)

            Box(modifier = Modifier.weight(1f)) {
                when (subScreen) {
                    "RETAIL" -> RetailPOSScreen(viewModel)
                    "RESTAURANT" -> RestaurantScreen(viewModel)
                    "KITCHEN_DISPLAY" -> KitchenDisplayScreen(viewModel)
                    "INVENTORY" -> InventoryScreen(viewModel)
                    "REPORTS" -> ReportsScreen(viewModel)
                    "PRINTER" -> PrinterReceiptScreen(viewModel)
                    "SETTINGS" -> SettingsScreen(viewModel)
                }
            }

            if (!isTablet) {
                // Mobile Bottom Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = subScreen == "RETAIL",
                        onClick = { viewModel.setSubScreen("RETAIL") },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                        label = { Text("Checkout", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = subScreen == "RESTAURANT",
                        onClick = { viewModel.setSubScreen("RESTAURANT") },
                        icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                        label = { Text("Tables", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = subScreen == "KITCHEN_DISPLAY",
                        onClick = { viewModel.setSubScreen("KITCHEN_DISPLAY") },
                        icon = { Icon(Icons.Default.Monitor, contentDescription = null) },
                        label = { Text("Kitchen", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = subScreen == "REPORTS",
                        onClick = { viewModel.setSubScreen("REPORTS") },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                        label = { Text("Reports", fontSize = 10.sp) }
                    )
                }
            }
        }
    }
}

data class NavItem(val title: String, val subName: String, val icon: ImageVector)

@Composable
fun NavigationRow(item: NavItem, isActive: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("nav_${item.subName.lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.title,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
        }
    }
}

// --- 4. RETAIL POS MODULE CHECKOUT SCREEN ---
@Composable
fun RetailPOSScreen(viewModel: POSViewModel) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val category by viewModel.selectedCategory.collectAsState()
    val search by viewModel.searchQuery.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    val categories = listOf("All", "Coffee Beans", "Cold Beverages", "Bakery", "Teas", "Hot Dishes")
    val filteredProducts = products.filter {
        (category == "All" || it.category == category) &&
                (search.isEmpty() || it.name.contains(search, ignoreCase = true) || it.barcode == search)
    }

    if (isTablet) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Column: Catalog and category slider
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Search & Network Sync status bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search catalog or scan barcode...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("catalog_search_bar"),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Network Sync Status pill
                    Box(
                        modifier = Modifier
                            .background(
                                if (syncState.contains("ACTIVE") || syncState.contains("MASTER")) Color(0xFF34D399).copy(alpha = 0.15f)
                                else Color(0xFFF97316).copy(alpha = 0.15f),
                                CircleShape
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (syncState.contains("ACTIVE") || syncState.contains("MASTER")) Color(0xFF10B981) else Color(0xFFF97316)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = syncState,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (syncState.contains("ACTIVE") || syncState.contains("MASTER")) Color(0xFF059669) else Color(0xFFD97706)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Categories horizontal scroller
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { viewModel.setCategory(cat) },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("category_chip_$cat")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredProducts.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No matching products found", fontWeight = FontWeight.Bold)
                            Text("Verify category filter or scan code again", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    // Products Grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProducts) { item ->
                            ProductGridItem(
                                item = item,
                                quantityInCart = cart.find { it.name == item.name }?.quantity ?: 0,
                                onAdd = { viewModel.addToCart(item) },
                                onRemove = {
                                    val idx = cart.indexOfFirst { it.name == item.name }
                                    if (idx != -1) {
                                        viewModel.updateCartItemQty(idx, cart[idx].quantity - 1)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Right Column: Active Order Cart & Payment controls
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Active Ticket Items",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (cart.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Ticket is empty", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(cart) { idx, item ->
                            CartItemRow(idx = idx, item = item, onQtyChange = { qty -> viewModel.updateCartItemQty(idx, qty) })
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val subtotal = cart.sumOf { it.quantity * it.unitPrice }
                    val tax = subtotal * 0.10 // 10%
                    val total = subtotal + tax
                    val totalItemsCount = cart.sumOf { it.quantity }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header matching Geometric Balance Summary Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = totalItemsCount.toString(),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "ITEMS IN CART",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                        Text(
                                            text = String.format("Subtotal: $%.2f", subtotal),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "TOTAL AMOUNT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = String.format("$%.2f", total),
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                            // Quick breakups
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vat Tax (10%):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(String.format("$%.2f", tax), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Payment Actions (Cash and Card with the Geometric Balance style)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.checkout("CASH") },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("pay_cash_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("CASH", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.checkout("CARD") },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("pay_card_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("CARD", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // MOBILE / NARROW LAYOUT: Toggle tabs between CATALOG and CART
        var mobileTab by remember { mutableStateOf("CATALOG") } // CATALOG, CART

        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = if (mobileTab == "CATALOG") 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = mobileTab == "CATALOG",
                    onClick = { mobileTab = "CATALOG" },
                    text = { Text("Menu List", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = mobileTab == "CART",
                    onClick = { mobileTab = "CART" },
                    text = { Text("Active Cart (${cart.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (cart.isNotEmpty()) {
                                    Badge { Text(cart.sumOf { it.quantity }.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                )
            }

            if (mobileTab == "CATALOG") {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    // Compact search bar
                    OutlinedTextField(
                        value = search,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search products...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("catalog_search_bar"),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic category item count mapper
                    val categoryCounts = remember(products) {
                        categories.associateWith { cat ->
                            if (cat == "All") products.size else products.count { it.category == cat }
                        }
                    }

                    // Categories slider using custom interactive cards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        categories.forEach { cat ->
                            val count = categoryCounts[cat] ?: 0
                            CategoryCard(
                                name = cat,
                                isSelected = category == cat,
                                itemCount = count,
                                onClick = { viewModel.setCategory(cat) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (filteredProducts.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No products found", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 130.dp),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredProducts) { item ->
                                ProductGridItem(
                                    item = item,
                                    quantityInCart = cart.find { it.name == item.name }?.quantity ?: 0,
                                    onAdd = { viewModel.addToCart(item) },
                                    onRemove = {
                                        val idx = cart.indexOfFirst { it.name == item.name }
                                        if (idx != -1) {
                                            viewModel.updateCartItemQty(idx, cart[idx].quantity - 1)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Cart Tab content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Active Ticket Items",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (cart.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Your cart is empty", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(cart) { idx, item ->
                                CartItemRow(idx = idx, item = item, onQtyChange = { qty -> viewModel.updateCartItemQty(idx, qty) })
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val subtotal = cart.sumOf { it.quantity * it.unitPrice }
                        val tax = subtotal * 0.10 // 10%
                        val total = subtotal + tax
                        val totalItemsCount = cart.sumOf { it.quantity }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer), RoundedCornerShape(16.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = totalItemsCount.toString(),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "ITEMS IN CART",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                            Text(
                                                text = String.format("Subtotal: $%.2f", subtotal),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "TOTAL AMOUNT",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                        Text(
                                            text = String.format("$%.2f", total),
                                            fontWeight = FontWeight.Black,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Vat Tax (10%):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text(String.format("$%.2f", tax), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.checkout("CASH") },
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("pay_cash_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("CASH", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.checkout("CARD") },
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("pay_card_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("CARD", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    name: String,
    isSelected: Boolean,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) {
        when (name) {
            "Coffee Beans" -> Color(0xFF3E2F1F)
            "Cold Beverages" -> Color(0xFF1B2F3E)
            "Bakery" -> Color(0xFF3E301F)
            "Teas" -> Color(0xFF1E3524)
            "Hot Dishes" -> Color(0xFF3A1F24)
            else -> Color(0xFF2E2E3E)
        }
    } else {
        Color(0xFF1F1F2C)
    }

    val iconColor = if (isSelected) {
        when (name) {
            "Coffee Beans" -> Color(0xFFF59E0B)
            "Cold Beverages" -> Color(0xFF3B82F6)
            "Bakery" -> Color(0xFFF97316)
            "Teas" -> Color(0xFF10B981)
            "Hot Dishes" -> Color(0xFFEF4444)
            else -> Color.White
        }
    } else {
        Color(0xFF9090A1)
    }

    val categoryIcon = when (name) {
        "Coffee Beans" -> Icons.Default.Coffee
        "Cold Beverages" -> Icons.Default.LocalDrink
        "Bakery" -> Icons.Default.Cake
        "Teas" -> Icons.Default.EmojiFoodBeverage
        "Hot Dishes" -> Icons.Default.Restaurant
        else -> Icons.Default.Category
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(115.dp)
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = if (isSelected) BorderStroke(1.5.dp, iconColor) else BorderStroke(1.dp, Color(0xFF2E2E3E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isSelected) Color.White else Color(0xFF9090A1),
                    maxLines = 1
                )
                Text(
                    text = "$itemCount items",
                    fontSize = 9.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.6f) else Color(0xFF9090A1).copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun ProductGridItem(
    item: Product,
    quantityInCart: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F2C)),
        border = BorderStroke(1.dp, Color(0xFF2E2E3E))
    ) {
        Column(
            modifier = Modifier
                .clickable { onAdd() }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Elegant Category tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2E2E3E))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.category.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Color(0xFF9090A1)
                    )
                }
                
                // Item code index
                Text(
                    text = "#p${item.id}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF9090A1).copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "${item.stockQuantity.toInt()} in stock",
                    fontSize = 11.sp,
                    color = Color(0xFF9090A1)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("$%.2f", item.sellingPrice),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )

                if (quantityInCart > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .background(Color(0xFF2E2E3E), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF2E2E3E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "−",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier
                                .clickable { onRemove() }
                                .padding(horizontal = 4.dp)
                        )
                        Text(
                            text = quantityInCart.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "+",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier
                                .clickable { onAdd() }
                                .padding(horizontal = 4.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF2E2E3E), RoundedCornerShape(8.dp))
                            .clickable { onAdd() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "add",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(idx: Int, item: OrderItem, onQtyChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sequential dark-grey circle badge
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E2E3E)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (idx + 1).toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
            }

            Column {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Qty: ${item.quantity}",
                        fontSize = 12.sp,
                        color = Color(0xFF9090A1),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = Color(0xFF9090A1).copy(alpha = 0.5f)
                    )
                    Text(
                        text = String.format("$%.2f ea", item.unitPrice),
                        fontSize = 12.sp,
                        color = Color(0xFF9090A1)
                    )
                }
            }
        }

        // Elegant minimal minus/plus control buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .background(Color(0xFF1F1F2C), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF2E2E3E), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "−",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.clickable { onQtyChange(item.quantity - 1) }
                )
                Text(
                    text = item.quantity.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
                Text(
                    text = "+",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.clickable { onQtyChange(item.quantity + 1) }
                )
            }

            Text(
                text = String.format("$%.2f", item.quantity * item.unitPrice),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
        }
    }
}

// --- 5. RESTAURANT TABLE LAYOUT SCREEN ---
@Composable
fun RestaurantScreen(viewModel: POSViewModel) {
    val tables by viewModel.tables.collectAsState()
    val selectedTable by viewModel.selectedTable.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Restaurant Floor Layout Planner",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text("Dine-In orders are tracked with physical tables coordinates", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(tables) { table ->
                val color = when (table.status) {
                    TableStatus.AVAILABLE -> Color(0xFF10B981)
                    TableStatus.OCCUPIED -> Color(0xFFEF4444)
                    TableStatus.RESERVED -> Color(0xFF3B82F6)
                    TableStatus.DIRTY -> Color(0xFFF59E0B)
                }

                Card(
                    onClick = {
                        viewModel.selectTable(table)
                    },
                    modifier = Modifier
                        .height(120.dp)
                        .border(
                            width = if (selectedTable?.id == table.id) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("table_card_${table.id}"),
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(table.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${table.seats} Seats", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .background(color, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(table.status.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        selectedTable?.let { table ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Selected: ${table.name}", fontWeight = FontWeight.Bold)
                        Text("Current table status: ${table.status.name}", fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.setSubScreen("RETAIL")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("New Order Ticket")
                        }

                        if (table.status == TableStatus.OCCUPIED) {
                            Button(
                                onClick = {
                                    viewModel.completeTableBill(table)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Settle / Settle Receipt")
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 6. KITCHEN DISPLAY SCREEN (KDS) ---
@Composable
fun KitchenDisplayScreen(viewModel: POSViewModel) {
    val orders by viewModel.orders.collectAsState()

    // Kitchen displays only PENDING or PREPARING dine-in & take-away tickets
    val activeTickets = orders.filter { it.status == OrderStatus.PENDING || it.status == OrderStatus.PREPARING }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // High contrast dark canvas
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Kitchen Display Terminal (KDS)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text("Live active cooking logs from checkout terminals", color = Color.White.copy(alpha = 0.6f))
            }

            Box(
                modifier = Modifier
                    .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Live updates active", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (activeTickets.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No pending tickets active", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Orders placed at cash registers will instantly display here", color = Color.White.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(activeTickets) { ticket ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Ticket #${ticket.orderNumber}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (ticket.status == OrderStatus.PENDING) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(ticket.status.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                "Type: ${ticket.type.name} • ${ticket.tableName ?: "Counter"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

                            // Items to prepare
                            val itemsList = remember(ticket.itemsJson) {
                                // Basic manual parser for item json string
                                parseOrderItems(ticket.itemsJson)
                            }

                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                itemsList.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${item.quantity}x ${item.name}",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            if (ticket.kitchenNotes.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .background(Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text("Note: ${ticket.kitchenNotes}", color = Color(0xFFF97316), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (ticket.status == OrderStatus.PENDING) {
                                    Button(
                                        onClick = { viewModel.updateOrderStatus(ticket.id, OrderStatus.PREPARING) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                                    ) {
                                        Text("COOKING")
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.updateOrderStatus(ticket.id, OrderStatus.READY) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("READY FOR SERVING")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom manual parser helper to bypass Moshi reflection overloads inside layout thread
private fun parseOrderItems(json: String): List<OrderItem> {
    if (json.isEmpty() || json == "[]") return emptyList()
    val list = mutableListOf<OrderItem>()
    // Extract block matches
    val items = json.split("},{")
    items.forEach { item ->
        val nameMatch = "\"name\"\\s*:\\s*\"([^\"]*)\"".toRegex().find(item)
        val name = nameMatch?.groupValues?.get(1) ?: "Kitchen Item"
        val qtyMatch = "\"quantity\"\\s*:\\s*([0-9]+)".toRegex().find(item)
        val qty = qtyMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
        list.add(OrderItem("", name, qty, 0.0))
    }
    return list
}

// --- 7. INVENTORY ADJUSTMENT SCREEN ---
@Composable
fun InventoryScreen(viewModel: POSViewModel) {
    val products by viewModel.products.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Inventory Control Ledger",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("Realtime adjustments and expiration date control", style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Register Product")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Inventory Stock Warning Box
        val lowStockCount = products.count { it.stockQuantity <= it.lowStockAlertLevel }
        if (lowStockCount > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("LOW STOCK WARNING ACTIVE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("$lowStockCount catalog products are under secure threshold limit. Reorder immediately.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Product Name / SKU", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), fontSize = 13.sp)
                    Text("Category", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Text("Cost / Selling", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Text("Stock Status", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                }
                Divider()
            }

            items(products) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("SKU: ${item.sku} | Code: ${item.barcode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }

                    Text(item.category, modifier = Modifier.weight(1f), fontSize = 13.sp)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(String.format("Cost: $%.2f", item.costPrice), fontSize = 12.sp)
                        Text(String.format("Sell: $%.2f", item.sellingPrice), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF10B981))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentWidth()
                    ) {
                        val low = item.stockQuantity <= item.lowStockAlertLevel
                        Box(
                            modifier = Modifier
                                .background(
                                    if (low) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${item.stockQuantity.toInt()} units",
                                color = if (low) Color(0xFFEF4444) else Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }

    // Add Product Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var barcode by remember { mutableStateOf("") }
        var sku by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Coffee Beans") }
        var cost by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var stock by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Register New Product") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") })
                    OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode String") })
                    OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU Code") })
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
                    OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost Price ($)") })
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Selling Price ($)") })
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Initial Stock") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addNewProduct(
                            name, barcode, sku, category,
                            cost.toDoubleOrNull() ?: 0.0,
                            price.toDoubleOrNull() ?: 0.0,
                            stock.toDoubleOrNull() ?: 0.0
                        )
                        showAddDialog = false
                    }
                ) {
                    Text("Save To Database")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// --- 8. RICH METRICS & SALES REPORTS SCREEN ---
@Composable
fun ReportsScreen(viewModel: POSViewModel) {
    val orders by viewModel.orders.collectAsState()

    val totalSales = orders.sumOf { it.totalAmount }
    val netRevenue = orders.sumOf { it.subtotal }
    val totalTax = orders.sumOf { it.taxAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Enterprise Sales Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text("Realtime analytical transaction ledger summary", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(24.dp))

        // Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard("Today Sales", String.format("$%.2f", totalSales), Color(0xFF10B981), modifier = Modifier.weight(1f))
            MetricCard("Tax Collected", String.format("$%.2f", totalTax), Color(0xFF6366F1), modifier = Modifier.weight(1f))
            MetricCard("Total Tickets", orders.size.toString(), Color(0xFFF59E0B), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Interactive High-Fidelity Custom Chart Drawing (Compose Canvas)
        Text("Weekly Sales Analytics (Hourly Distribution)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Background grid lines
                val gridLines = 4
                val yStep = size.height / gridLines
                for (i in 0..gridLines) {
                    val y = i * yStep
                    drawLine(
                        color = Color(0xFF2E2E3E),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f
                    )
                }

                // Drawing sales vertical bars (with rounded tops)
                val points = listOf(110f, 180f, 140f, 220f, 170f, 240f, 210f) // Weekly coordinates
                val barCount = points.size
                val barSpacing = size.width / (barCount * 1.8f)
                val barWidth = size.width / (barCount * 1.5f)

                points.forEachIndexed { index, value ->
                    val x = (barSpacing / 2) + index * (barWidth + barSpacing)
                    val barHeight = (value / 250f) * size.height
                    val y = size.height - barHeight

                    drawRoundRect(
                        color = if (index == 5) Color(0xFFFF7B54) else Color(0xFF6366F1), // Highlight peak day in Coral, others in Indigo
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Export Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {},
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.FilePresent, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export PDF")
            }

            Button(
                onClick = {},
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.GridOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Excel / CSV")
            }
        }
    }
}

@Composable
fun MetricCard(title: String, valStr: String, indicatorColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(valStr, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// --- 9. RECEIPT & ESC/POS PRINTER PREVIEW SCREEN ---
@Composable
fun PrinterReceiptScreen(viewModel: POSViewModel) {
    val receiptText by viewModel.receiptPreview.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Terminal Receipt & ESC/POS Printer Simulator",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text("High fidelity layout preview mapping physical thermal printer parameters", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(24.dp))

        if (receiptText.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No checkout receipt loaded", fontWeight = FontWeight.Bold)
                    Text("Complete a retail order to populate this terminal screen preview", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), // Warm Paper color
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 420.dp)
                    .align(Alignment.CenterHorizontally)
                    .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(8.dp))
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = receiptText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color.Black,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {},
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulate BLE / USB Printing")
            }
        }
    }
}

// --- 10. SYSTEM SETTINGS SCREEN ---
@Composable
fun SettingsScreen(viewModel: POSViewModel) {
    val logs by viewModel.auditLogs.collectAsState()
    val syncQueue by viewModel.syncQueue.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Security Settings & Audit Trail",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Local Offline Synchronization Sync Queue (${syncQueue.size} pending items)", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            if (syncQueue.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("All offline sales transactions are fully synced!", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(12.dp)) {
                    items(syncQueue) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.action} -> ${item.entityType}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Pending Sync", fontSize = 11.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Security System Audit Trail Log (Latest Ops)", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(logs) { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(log.username, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(log.timestamp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Text("${log.action}: ${log.details}", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
