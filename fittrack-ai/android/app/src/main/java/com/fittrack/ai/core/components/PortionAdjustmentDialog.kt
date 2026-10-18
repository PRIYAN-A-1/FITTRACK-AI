package com.fittrack.ai.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fittrack.ai.core.theme.*

@Composable
fun PortionAdjustmentDialog(
    itemName: String,
    initialWeightGrams: Float,
    initialCookingMethod: String,
    initialOilAdjustment: String,
    caloriesPer100g: Float,
    proteinPer100g: Float,
    carbsPer100g: Float,
    fatPer100g: Float,
    onDismiss: () -> Unit,
    onConfirm: (weight: Float, cookingMethod: String, oilAdjustment: String) -> Unit
) {
    var weight by remember { mutableFloatStateOf(initialWeightGrams) }
    var cookingMethod by remember { mutableStateOf(initialCookingMethod) }
    var oilAdjustment by remember { mutableStateOf(initialOilAdjustment) }

    // Live formula calculation
    val ratio = weight / 100f
    var calcCalories = caloriesPer100g * ratio
    var calcFat = fatPer100g * ratio

    if (cookingMethod == "fried") {
        calcCalories *= 1.15f
        calcFat *= 1.25f
    }
    if (oilAdjustment == "low") {
        calcFat *= 0.85f
        calcCalories -= (calcFat * 0.15f * 9f)
    } else if (oilAdjustment == "high") {
        calcFat *= 1.25f
        calcCalories += (calcFat * 0.25f * 9f)
    }

    val calcProtein = proteinPer100g * ratio
    val calcCarbs = carbsPer100g * ratio

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Adjust Portion & Cooking",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = itemName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldLight
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Live Nutrition Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${calcCalories.toInt()} kcal", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AmberAccent)
                            Text(text = "Calories", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${String.format("%.1f", calcProtein)}g", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MacroProtein)
                            Text(text = "Protein", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${String.format("%.1f", calcCarbs)}g", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MacroCarbs)
                            Text(text = "Carbs", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${String.format("%.1f", calcFat)}g", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MacroFat)
                            Text(text = "Fat", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Weight Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Portion Mass", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(text = "${weight.toInt()} grams", fontWeight = FontWeight.Bold, color = EmeraldLight)
                }
                Slider(
                    value = weight,
                    onValueChange = { weight = it },
                    valueRange = 10f..800f,
                    steps = 79,
                    colors = SliderDefaults.colors(thumbColor = EmeraldPrimary, activeTrackColor = EmeraldLight)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Cooking Method Chips
                Text(text = "Cooking Method", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("standard", "boiled", "fried", "grilled").forEach { method ->
                        val isSelected = cookingMethod == method
                        FilterChip(
                            selected = isSelected,
                            onClick = { cookingMethod = method },
                            label = { Text(method.replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cooking Oil Adjustment
                Text(text = "Oil / Ghee Level", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("none", "low", "normal", "high").forEach { oil ->
                        val isSelected = oilAdjustment == oil
                        FilterChip(
                            selected = isSelected,
                            onClick = { oilAdjustment = oil },
                            label = { Text(oil.replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberAccent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(weight, cookingMethod, oilAdjustment) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
