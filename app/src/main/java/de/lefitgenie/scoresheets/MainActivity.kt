package de.lefitgenie.scoresheets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.lefitgenie.scoresheets.ui.AppNavigation
import de.lefitgenie.scoresheets.ui.theme.ScoreSheetsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScoreSheetsTheme {
                AppNavigation()
            }
        }
    }
}
