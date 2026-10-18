package com.fittrack.ai.core.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.ai.core.theme.*

@Composable
fun MacroProgressRing(
    caloriesConsumed: Float,
    caloriesTarget: Int,
    proteinGrams: Float,
    proteinTarget: Float,
    carbsGrams: Float,
    carbsTarget: Float,
    fatGrams: Float,
    fatTarget: Float,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    strokeWidth: Dp = 16.dp
) {
    val calorieProgress = if (caloriesTarget > 0) (caloriesConsumed / caloriesTarget).coerceIn(0f, 1.2f) else 0f
    val proteinProgress = if (proteinTarget > 0) (proteinGrams / proteinTarget).coerceIn(0f, 1.2f) else 0f
    val carbsProgress = if (carbsTarget > 0) (carbsGrams / carbsTarget).coerceIn(0f, 1.2f) else 0f
    val fatProgress = if (fatTarget > 0) (fatGrams / fatTarget).coerceIn(0f, 1.2f) else 0f

    val animCalorieProgress by animateFloatAsState(targetValue = calorieProgress, animationSpec = tween(900), label = "cal")
    val animProteinProgress by animateFloatAsState(targetValue = proteinProgress, animationSpec = tween(900), label = "prot")
    val animCarbsProgress by animateFloatAsState(targetValue = carbsProgress, animationSpec = tween(900), label = "carb")
    val animFatProgress by animateFloatAsState(targetValue = fatProgress, animationSpec = tween(900), label = "fat")

    val caloriesRemaining = (caloriesTarget - caloriesConsumed).coerceAtLeast(0f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val canvasSize = size.toPx()
            val arcSize = canvasSize - strokePx

            // Track Background
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(strokePx / 2, strokePx / 2),
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Animated Calorie Outer Ring (Emerald Gradient)
            val sweep = animCalorieProgress * 360f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(EmeraldPrimary, EmeraldLight, AmberAccent, EmeraldPrimary)
                ),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(strokePx / 2, strokePx / 2),
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Inner Accent Arc: Protein (Blue)
            val innerStrokePx = 6.dp.toPx()
            val innerSize1 = arcSize - 28.dp.toPx()
            drawArc(
                color = MacroProtein.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset((canvasSize - innerSize1) / 2, (canvasSize - innerSize1) / 2),
                size = Size(innerSize1, innerSize1),
                style = Stroke(width = innerStrokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = MacroProtein,
                startAngle = -90f,
                sweepAngle = animProteinProgress * 360f,
                useCenter = false,
                topLeft = Offset((canvasSize - innerSize1) / 2, (canvasSize - innerSize1) / 2),
                size = Size(innerSize1, innerSize1),
                style = Stroke(width = innerStrokePx, cap = StrokeCap.Round)
            )
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${caloriesRemaining.toInt()}",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "KCAL REMAINING",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${caloriesConsumed.toInt()} / $caloriesTarget kcal",
                fontSize = 12.sp,
                color = EmeraldLight
            )
        }
    }
}
