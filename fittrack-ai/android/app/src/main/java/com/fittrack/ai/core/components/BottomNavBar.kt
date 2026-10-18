package com.fittrack.ai.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fittrack.ai.core.navigation.Screen
import com.fittrack.ai.core.theme.EmeraldLight
import com.fittrack.ai.core.theme.EmeraldPrimary

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun FitTrackBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(Screen.Dashboard.route, "Home", Icons.Default.Home),
        NavItem(Screen.Diary.route, "Diary", Icons.Default.CalendarMonth),
        NavItem(Screen.Assistant.route, "AI Coach", Icons.Default.ChatBubble),
        NavItem(Screen.Analytics.route, "Insights", Icons.Default.BarChart)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(12.dp, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First 2 items
                items.take(2).forEach { item ->
                    val selected = currentRoute == item.route
                    IconButton(
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (selected) EmeraldLight else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Space for Center FAB
                Spacer(modifier = Modifier.width(56.dp))

                // Last 2 items
                items.takeLast(2).forEach { item ->
                    val selected = currentRoute == item.route
                    IconButton(
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (selected) EmeraldLight else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // Center Elevated Camera FAB
        FloatingActionButton(
            onClick = onCameraClick,
            shape = CircleShape,
            containerColor = EmeraldPrimary,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
            modifier = Modifier
                .size(60.dp)
                .offset(y = (-14).dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Scan Meal",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
