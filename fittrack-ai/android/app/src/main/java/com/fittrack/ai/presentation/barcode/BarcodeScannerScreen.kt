package com.fittrack.ai.presentation.barcode

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.ai.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    viewModel: BarcodeViewModel,
    onNavigateBack: () -> Unit,
    onProductSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var manualBarcodeInput by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf("snack") }

    LaunchedEffect(uiState) {
        if (uiState is BarcodeUiState.Saved) {
            viewModel.reset()
            onProductSaved()
        }
    }

    Scaffold(
        containerColor = DarkObsidian,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Barcode & Label Scanner",
                        style = MaterialTheme.typography.titleMedium,
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
            // Viewfinder Simulator Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Barcode reticle
                        Box(
                            modifier = Modifier
                                .width(240.dp)
                                .height(120.dp)
                                .border(2.dp, EmeraldTeal, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = EmeraldTeal,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Align UPC / EAN barcode here",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Quick Catalog Test Barcodes
            item {
                Column {
                    Text(
                        "Quick Test Barcodes (Composite Catalog):",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "8901262010015" to "Amul Taaza Toned Milk (200ml)",
                        "030000010204" to "Quaker Rolled Oats (40g)",
                        "8906070430018" to "Epigamia Greek Yogurt (100g)",
                        "8906009995551" to "RiteBite Max Protein Bar (67g)"
                    ).forEach { (code, title) ->
                        Surface(
                            color = CardBackground,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    manualBarcodeInput = code
                                    viewModel.lookupBarcode(code)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("UPC: $code", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                                Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldTeal, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Manual Barcode Input
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Enter Barcode Digits",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualBarcodeInput,
                                onValueChange = { manualBarcodeInput = it },
                                placeholder = { Text("e.g. 8901262010015", color = Color(0xFF64748B)) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = EmeraldTeal,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    cursorColor = EmeraldTeal
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.lookupBarcode(manualBarcodeInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldTeal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Lookup")
                            }
                        }
                    }
                }
            }

            // Product Result Card
            if (uiState is BarcodeUiState.ProductFound) {
                val product = uiState as BarcodeUiState.ProductFound
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF134E4A).copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EmeraldTeal.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        product.productName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        product.brand,
                                        color = MintAccent,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    "${product.calories.toInt()} kcal",
                                    color = EmeraldTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MacroPill("Serving", "${product.servingSizeG.toInt()}g", Color(0xFF94A3B8))
                                MacroPill("Protein", "${product.proteinG.toInt()}g", ProteinBlue)
                                MacroPill("Carbs", "${product.carbsG.toInt()}g", CarbsAmber)
                                MacroPill("Fat", "${product.fatG.toInt()}g", FatRose)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Meal Type Selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("breakfast", "lunch", "dinner", "snack").forEach { type ->
                                    FilterChip(
                                        selected = selectedMealType == type,
                                        onClick = { selectedMealType = type },
                                        label = { Text(type.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldTeal,
                                            selectedLabelColor = Color.White,
                                            containerColor = CardBackground,
                                            labelColor = Color(0xFF94A3B8)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { viewModel.saveProductAsMeal(selectedMealType) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldTeal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log to Food Diary", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (uiState is BarcodeUiState.Error) {
                item {
                    Text(
                        (uiState as BarcodeUiState.Error).message,
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MacroPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
