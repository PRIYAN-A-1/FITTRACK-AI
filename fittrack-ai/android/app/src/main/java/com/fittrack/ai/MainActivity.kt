package com.fittrack.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fittrack.ai.core.navigation.FitTrackNavGraph
import com.fittrack.ai.core.theme.FitTrackAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitTrackAITheme {
                FitTrackNavGraph()
            }
        }
    }
}
