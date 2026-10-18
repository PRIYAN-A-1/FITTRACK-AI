package com.fittrack.ai.presentation.voice

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.ai.core.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceLoggingScreen(
    viewModel: VoiceViewModel,
    onNavigateBack: () -> Unit,
    onMealSaved: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var transcriptInput by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf("lunch") }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                transcriptInput = spokenText
                viewModel.parseTranscript(spokenText, selectedMealType)
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is VoiceUiState.Saved) {
            viewModel.reset()
            onMealSaved()
        }
    }

    Scaffold(
        containerColor = DarkObsidian,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Voice Meal Logging",
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
            // Header instruction
            item {
                Text(
                    text = "Describe what you ate in English or Indian terms",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }

            // Meal Type Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("breakfast", "lunch", "dinner", "snack").forEach { type ->
                        FilterChip(
                            selected = selectedMealType == type,
                            onClick = { selectedMealType = type },
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

            // Big Microphone Button
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(pulseScale)
                            .border(3.dp, EmeraldTeal.copy(alpha = 0.5f), CircleShape)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldTeal)
                            .clickable {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your meal (e.g. 2 dosas with sambar)...")
                                }
                                try {
                                    speechRecognizerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    // Fallback if device has no speech recognition service
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Tap to speak",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Tap mic to speak",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            // Editable Transcript Field
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Meal Transcript / Text",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = transcriptInput,
                            onValueChange = { transcriptInput = it },
                            placeholder = { Text("e.g. I ate two dosas with sambar", color = Color(0xFF64748B)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = EmeraldTeal,
                                unfocusedBorderColor = Color(0xFF334155),
                                cursorColor = EmeraldTeal
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.parseTranscript(transcriptInput, selectedMealType) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldTeal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = transcriptInput.isNotBlank() && uiState !is VoiceUiState.Parsing
                        ) {
                            if (uiState is VoiceUiState.Parsing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Parsing Nutrition...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze & Extract Foods")
                            }
                        }
                    }
                }
            }

            // Quick Example Chips
            item {
                Column {
                    Text(
                        "Try sample voice phrases:",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "I ate two dosas with sambar",
                        "I had 200 grams chicken biryani",
                        "One banana, two eggs and one glass of milk",
                        "Add four teaspoons of ghee"
                    ).forEach { sample ->
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    transcriptInput = sample
                                    viewModel.parseTranscript(sample, selectedMealType)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = EmeraldTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(sample, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Parsed Result Review Card
            if (uiState is VoiceUiState.Parsed) {
                val parsed = uiState as VoiceUiState.Parsed
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF134E4A).copy(alpha = 0.3f)),
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
                                Text(
                                    "Extracted Foods (${parsed.result.parsedItems.size})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "${parsed.result.totalNutrients.calories.toInt()} kcal",
                                    color = EmeraldTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            for (food in parsed.result.parsedItems) {
                                Surface(
                                    color = CardBackground,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(food.foodName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(
                                                "${food.estimatedWeightG.toInt()}g • P: ${food.nutrients.proteinG.toInt()}g • C: ${food.nutrients.carbsG.toInt()}g • F: ${food.nutrients.fatG.toInt()}g",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(
                                            "${food.nutrients.calories.toInt()} kcal",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.saveMeal(selectedMealType) },
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

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
