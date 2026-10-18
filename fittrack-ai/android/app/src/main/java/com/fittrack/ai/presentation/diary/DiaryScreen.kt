package com.fittrack.ai.presentation.diary

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.ai.data.local.entities.MealItemEntity
import com.fittrack.ai.data.local.dao.MealWithItems
import com.fittrack.ai.core.components.PortionAdjustmentDialog
import com.fittrack.ai.core.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingItemInfo by remember { mutableStateOf<Triple<String, MealItemEntity, String>?>(null) } // mealId, item, mealTitle

    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    val isToday = uiState.selectedDate == LocalDate.now()
    val dateDisplay = when {
        isToday -> "Today, ${uiState.selectedDate.format(dateFormatter)}"
        uiState.selectedDate == LocalDate.now().minusDays(1) -> "Yesterday, ${uiState.selectedDate.format(dateFormatter)}"
        else -> uiState.selectedDate.format(dateFormatter)
    }

    Scaffold(
        containerColor = DarkObsidian,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Food Diary",
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
                actions = {
                    IconButton(onClick = onNavigateToScan) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan", tint = EmeraldTeal)
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
            // Date Switcher Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.previousDay() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = Color.White)
                        }

                        Text(
                            text = dateDisplay,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )

                        IconButton(onClick = { viewModel.nextDay() }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = Color.White)
                        }
                    }
                }
            }

            // Daily Macro Progress Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total Logged Energy",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            Text(
                                "${uiState.totalCalories.toInt()} kcal",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldTeal
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DailyMacroItem("Protein", "${uiState.totalProtein.toInt()}g", ProteinBlue)
                            DailyMacroItem("Carbs", "${uiState.totalCarbs.toInt()}g", CarbsAmber)
                            DailyMacroItem("Fat", "${uiState.totalFat.toInt()}g", FatRose)
                            DailyMacroItem("Fiber", "${uiState.totalFiber.toInt()}g", MintAccent)
                        }
                    }
                }
            }

            // Meal Items or Empty State
            if (uiState.meals.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No meals logged for this day",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Use AI Vision or Voice to log your plate instantly",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToScan,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Meal with AI", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                items(uiState.meals, key = { it.meal.id }) { mealWithItems ->
                    MealEntryCard(
                        mealWithItems = mealWithItems,
                        onItemClick = { item ->
                            editingItemInfo = Triple(mealWithItems.meal.id, item, mealWithItems.meal.mealTitle)
                        },
                        onDeleteMeal = {
                            viewModel.deleteMeal(mealWithItems.meal.id)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Edit Item Modal
    editingItemInfo?.let { (mealId, item, _) ->
        val ratio = if (item.servingWeightG > 0) item.servingWeightG / 100f else 1f
        PortionAdjustmentDialog(
            itemName = item.name,
            initialWeightGrams = item.servingWeightG,
            initialCookingMethod = item.cookingMethod,
            initialOilAdjustment = item.oilAdjustment,
            caloriesPer100g = item.calories / ratio,
            proteinPer100g = item.proteinG / ratio,
            carbsPer100g = item.carbsG / ratio,
            fatPer100g = item.fatG / ratio,
            onDismiss = { editingItemInfo = null },
            onConfirm = { newWeight, newMethod, newOil ->
                viewModel.updateMealItemPortion(
                    mealId = mealId,
                    itemId = item.id,
                    newWeightGrams = newWeight,
                    cookingMethod = newMethod,
                    oilAdjustment = newOil
                )
                editingItemInfo = null
            }
        )
    }
}

@Composable
private fun MealEntryCard(
    mealWithItems: MealWithItems,
    onItemClick: (MealItemEntity) -> Unit,
    onDeleteMeal: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Type badge, Title, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = EmeraldTeal.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = mealWithItems.meal.mealType.uppercase(),
                            color = EmeraldTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = mealWithItems.meal.mealTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                IconButton(
                    onClick = onDeleteMeal,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete Meal",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meal Items list
            for ((index, item) in mealWithItems.items.withIndex()) {
                if (index > 0) {
                    Divider(color = Color(0xFF334155).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${item.servingWeightG.toInt()}g • P: ${item.proteinG.toInt()}g • C: ${item.carbsG.toInt()}g • F: ${item.fatG.toInt()}g",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${item.calories.toInt()} kcal",
                            color = EmeraldTeal,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Portion",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyMacroItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
