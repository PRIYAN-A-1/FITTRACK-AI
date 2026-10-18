package com.fittrack.ai.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.ai.core.theme.*

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun OnboardingScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }

    val pages = listOf(
        OnboardingPage(
            title = "Snap. Estimate. Track.",
            subtitle = "Point your camera at any meal plate. Our multimodal AI identifies ingredients and estimates portion weight in seconds.",
            icon = Icons.Default.CameraAlt,
            accentColor = EmeraldPrimary
        ),
        OnboardingPage(
            title = "Authentic Regional & Global Foods",
            subtitle = "From South Indian Idlis, Dosas, and Sambar to Chicken Biryani and Protein Bowls—verified IFCT & USDA nutrition.",
            icon = Icons.Default.Restaurant,
            accentColor = AmberAccent
        ),
        OnboardingPage(
            title = "Precision Macro Coaching",
            subtitle = "Track daily calorie budgets, protein goals, hydration, and chat with your AI nutritionist anytime.",
            icon = Icons.Default.AutoAwesome,
            accentColor = MacroProtein
        )
    )

    val page = pages[currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onNavigateToLogin) {
                    Text("Skip", color = TextSecondaryDark, fontSize = 14.sp)
                }
            }

            // Center Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(
                            brush = Brush.radialGradient(listOf(page.accentColor.copy(alpha = 0.8f), page.accentColor.copy(alpha = 0.2f))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = page.title,
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = page.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = page.subtitle,
                    fontSize = 14.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Bottom Navigation & Indicators
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dot Indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 28.dp)
                ) {
                    pages.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .height(6.dp)
                                .width(if (index == currentPage) 24.dp else 6.dp)
                                .background(
                                    color = if (index == currentPage) EmeraldLight else Color(0xFF334155),
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }

                if (currentPage < pages.lastIndex) {
                    Button(
                        onClick = { currentPage++ },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = onNavigateToRegister,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Already have an account? ", color = TextSecondaryDark, fontSize = 13.sp)
                    Text(
                        text = "Log In",
                        color = EmeraldLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}
