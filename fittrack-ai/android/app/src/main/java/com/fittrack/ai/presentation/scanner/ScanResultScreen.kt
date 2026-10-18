package com.fittrack.ai.presentation.scanner

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.fittrack.ai.data.remote.dto.DetectedItemDto
import com.fittrack.ai.core.components.PlateFoodItemCard
import com.fittrack.ai.core.components.PortionAdjustmentDialog
import com.fittrack.ai.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedItemForEdit by remember { mutableStateOf<DetectedItemDto?>(null) }
    var mealType by remember { mutableStateOf("lunch") }

    LaunchedEffect(uiState) {
        if (uiState is ScanUiState.Saved) {
            viewModel.reset()
            onSaved()
        }
    }

    val reviewState = uiState as? ScanUiState.Review

    if (reviewState == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkObsidian),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No scan in progress", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldTeal)
                ) {
                    Text("Go to Scanner")
                }
            }
        }
        return
    }

    Scaffold(
        containerColor = DarkObsidian,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI Food Review",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "${reviewState.confidencePct}% Recognition Confidence",
                            style = MaterialTheme.typography.bodySmall,
                            color = MintAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkObsidian)
            )
        },
        bottomBar = {
            Surface(
                color = CardBackground,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveMeal(
                                mealType = mealType,
                                mealTitle = reviewState.mealTitle
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldTeal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Confirm & Save to Diary",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Meal type selection chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("breakfast", "lunch", "dinner", "snack").forEach { type ->
                        val isSelected = mealType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { mealType = type },
                            label = { Text(type.replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldTeal,
                                selectedLabelColor = Color.White,
                                containerColor = CardBackground,
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }

            // Big Nutritional Summary Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Total Energy",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                                Text(
                                    "${reviewState.totalCalories.toInt()} kcal",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Surface(
                                color = MintAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = MintAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Verified Nutrients",
                                        color = MintAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Macros row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MacroStat("Protein", "${String.format("%.1f", reviewState.totalProtein)}g", ProteinBlue)
                            MacroStat("Carbs", "${String.format("%.1f", reviewState.totalCarbs)}g", CarbsAmber)
                            MacroStat("Fat", "${String.format("%.1f", reviewState.totalFat)}g", FatRose)
                            MacroStat("Fiber", "${String.format("%.1f", reviewState.totalFiber)}g", MintAccent)
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Identified Food Items (${reviewState.items.size})",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Tap item to adjust",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            // Detected items list
            items(reviewState.items, key = { it.itemId }) { item ->
                PlateFoodItemCard(
                    name = item.name,
                    category = "Plate Item",
                    weightGrams = item.estimatedWeightG,
                    unit = item.servingUnit,
                    calories = item.nutrients.calories,
                    proteinGrams = item.nutrients.proteinG,
                    carbsGrams = item.nutrients.carbsG,
                    fatGrams = item.nutrients.fatG,
                    confidencePct = (item.confidence * 100).toInt(),
                    cookingMethod = item.cookingMethod,
                    oilAdjustment = item.oilAdjustment,
                    onAdjustClick = { selectedItemForEdit = item },
                    onQuickWeightChange = { newW ->
                        viewModel.updateItemPortion(item.itemId, newW, item.cookingMethod, item.oilAdjustment)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Portion adjustment modal
    selectedItemForEdit?.let { item ->
        val per100Ratio = if (item.estimatedWeightG > 0) item.estimatedWeightG / 100f else 1f
        PortionAdjustmentDialog(
            itemName = item.name,
            initialWeightGrams = item.estimatedWeightG,
            initialCookingMethod = item.cookingMethod,
            initialOilAdjustment = item.oilAdjustment,
            caloriesPer100g = item.nutrients.calories / per100Ratio,
            proteinPer100g = item.nutrients.proteinG / per100Ratio,
            carbsPer100g = item.nutrients.carbsG / per100Ratio,
            fatPer100g = item.nutrients.fatG / per100Ratio,
            onDismiss = { selectedItemForEdit = null },
            onConfirm = { newWeight, newMethod, newOil ->
                viewModel.updateItemPortion(
                    itemId = item.itemId,
                    newWeightGrams = newWeight,
                    cookingMethod = newMethod,
                    oilAdjustment = newOil
                )
                selectedItemForEdit = null
            }
        )
    }
}

@Composable
private fun MacroStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
