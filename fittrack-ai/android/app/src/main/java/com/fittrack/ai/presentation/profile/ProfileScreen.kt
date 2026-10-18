package com.fittrack.ai.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.ai.data.local.datastore.SessionManager
import com.fittrack.ai.core.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    sessionManager: SessionManager,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var healthConnectEnabled by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkObsidian,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "User Profile & Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkObsidian)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Header Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(EmeraldTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("FitTrack Athlete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("athlete@fittrack.ai", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = EmeraldTeal.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "PRO MEMBER",
                                    color = EmeraldTeal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Health Goals Section
            item {
                Text("Nutrition Goals", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        GoalRow("Daily Calorie Target", "2,100 kcal")
                        Divider(color = Color(0xFF334155).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))
                        GoalRow("Daily Protein Target", "140 g")
                        Divider(color = Color(0xFF334155).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))
                        GoalRow("Daily Hydration Target", "2,500 ml")
                        Divider(color = Color(0xFF334155).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))
                        GoalRow("Target Weight", "70.0 kg")
                    }
                }
            }

            // Integrations
            item {
                Text("Integrations & Devices", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = FatRose, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Android Health Connect", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Sync steps, burned calories & workouts", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = healthConnectEnabled,
                            onCheckedChange = { healthConnectEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldTeal, checkedTrackColor = EmeraldTeal.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            // Privacy & Security
            item {
                Text("Security & Privacy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SettingItem(Icons.Default.Lock, "Change Password") {}
                        SettingItem(Icons.Default.CloudSync, "Offline Data Sync & Cache") {}
                        SettingItem(Icons.Default.PrivacyTip, "Privacy Policy & Data Export") {}
                    }
                }
            }

            // Danger Zone & Logout
            item {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            sessionManager.clearSession()
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            item {
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Account & Cloud Data", color = Color(0xFFEF4444), fontSize = 13.sp)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?", color = Color.White) },
            text = { Text("This will permanently remove your profile, food scans, and meal history in accordance with privacy laws. This action cannot be undone.", color = Color(0xFF94A3B8)) },
            containerColor = CardBackground,
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        coroutineScope.launch {
                            sessionManager.clearSession()
                            onLogout()
                        }
                    }
                ) {
                    Text("Delete Permanently", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun GoalRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 14.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun SettingItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = EmeraldTeal, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, color = Color.White, fontSize = 14.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
    }
}
