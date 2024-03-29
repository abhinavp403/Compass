package dev.abhinav.compass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import dev.abhinav.compass.ui.theme.CompassTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompassTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = Color.White) {
                    CompassScreen()
                }
            }
        }
    }
}