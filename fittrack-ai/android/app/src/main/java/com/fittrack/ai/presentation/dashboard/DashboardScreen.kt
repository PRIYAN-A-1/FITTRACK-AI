package com.fittrack.ai.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.ai.core.components.MacroProgressRing
import com.fittrack.ai.core.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToBarcode: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDiary: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // 1. Header with Greeting & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, ${uiState.userName} 👋",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                    }

                    // Profile Avatar Icon
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier
                            .size(46.dp)
                            .background(DarkSurfaceElevated, CircleShape)
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = EmeraldLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 2. Central Macro Progress Ring
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MacroProgressRing(
                        caloriesConsumed = uiState.caloriesConsumed,
                        caloriesTarget = uiState.caloriesTarget,
                        proteinGrams = uiState.protein.consumed,
                        proteinTarget = uiState.protein.target,
                        carbsGrams = uiState.carbs.consumed,
                        carbsTarget = uiState.carbs.target,
                        fatGrams = uiState.fat.consumed,
                        fatTarget = uiState.fat.target
                    )
                }
            }

            // 3. Macro Target Cards (Protein, Carbs, Fat)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MacroMetricCard(
                        title = "Protein",
                        consumed = uiState.protein.consumed,
                        target = uiState.protein.target,
                        color = MacroProtein,
                        modifier = Modifier.weight(1f)
                    )
                    MacroMetricCard(
                        title = "Carbs",
                        consumed = uiState.carbs.consumed,
                        target = uiState.carbs.target,
                        color = MacroCarbs,
                        modifier = Modifier.weight(1f)
                    )
                    MacroMetricCard(
                        title = "Fat",
                        consumed = uiState.fat.consumed,
                        target = uiState.fat.target,
                        color = MacroFat,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Quick Actions Row (Camera AI Scan, Voice Log, Barcode Scan)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "AI Scan",
                        icon = Icons.Default.CameraAlt,
                        color = EmeraldPrimary,
                        onClick = onNavigateToScan,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        title = "Voice Log",
                        icon = Icons.Default.Mic,
                        color = MacroProtein,
                        onClick = onNavigateToVoice,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        title = "Barcode",
                        icon = Icons.Default.QrCodeScanner,
                        color = AmberAccent,
                        onClick = onNavigateToBarcode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Daily Nutritional Insight Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(EmeraldPrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI NUTRITION INSIGHT", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = EmeraldLight)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(uiState.dailyInsight, fontSize = 13.sp, color = Color.White, lineHeight = 18.sp)
                        }
                    }
                }
            }

            // 6. Water Tracker Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DarkSurfaceElevated,
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
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MacroWater.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MacroWater, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Hydration", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                Text("${uiState.waterConsumedMl} / ${uiState.waterTargetMl} ml", fontSize = 12.sp, color = TextSecondaryDark)
                            }
                        }

                        Button(
                            onClick = { viewModel.logWaterGlass() },
                            colors = ButtonDefaults.buttonColors(containerColor = MacroWater.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MacroWater, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+250 ml", color = MacroWater, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 7. Today's Logged Meals Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Today's Meals (${uiState.meals.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    TextButton(onClick = onNavigateToDiary) {
                        Text("View Diary", color = EmeraldLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Logged Meals List
            if (uiState.meals.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurfaceElevated.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No meals logged yet today", color = TextSecondaryDark, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap 'AI Scan' to capture your plate", color = EmeraldLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                items(uiState.meals) { mealWithItems ->
                    val meal = mealWithItems.meal
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meal.mealTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${meal.mealType.replaceFirstChar { it.uppercase() }} • ${mealWithItems.items.size} items",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("P: ${meal.totalProteinG.toInt()}g", fontSize = 11.sp, color = MacroProtein, fontWeight = FontWeight.SemiBold)
                                    Text("C: ${meal.totalCarbsG.toInt()}g", fontSize = 11.sp, color = MacroCarbs, fontWeight = FontWeight.SemiBold)
                                    Text("F: ${meal.totalFatG.toInt()}g", fontSize = 11.sp, color = MacroFat, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${meal.totalCalories.toInt()} kcal",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = AmberAccent
                                )
                                IconButton(onClick = { viewModel.deleteMeal(meal.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMutedDark, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp)) // Padding for bottom bar
            }
        }
    }
}

@Composable
fun MacroMetricCard(
    title: String,
    consumed: Float,
    target: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurfaceElevated,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${consumed.toInt()}g",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "of ${target.toInt()}g",
                fontSize = 10.sp,
                color = TextMutedDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (target > 0) (consumed / target).coerceIn(0f, 1f) else 0f },
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurfaceElevated,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
