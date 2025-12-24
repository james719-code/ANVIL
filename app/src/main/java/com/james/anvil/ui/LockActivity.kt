package com.james.anvil.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.core.PenaltyManager
import com.james.anvil.ui.theme.ANVILTheme
import kotlinx.coroutines.delay

class LockActivity : ComponentActivity() {

    private lateinit var penaltyManager: PenaltyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        penaltyManager = PenaltyManager(this)
        
        
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        enableEdgeToEdge()
        setContent {
            ANVILTheme {
                LockScreen(penaltyManager)
            }
        }
    }
    
    
    override fun onPause() {
        super.onPause()
    }
}

@Composable
fun LockScreen(penaltyManager: PenaltyManager) {
    var timeLeft by remember { mutableStateOf("...") }
    val isPenaltyActive = remember { penaltyManager.isPenaltyActive() }
    val penaltyEndTime = remember { penaltyManager.getPenaltyEndTime() }
    
    
    LaunchedEffect(key1 = penaltyEndTime, key2 = isPenaltyActive) {
        if (!isPenaltyActive) {
            timeLeft = "--:--:--"
            return@LaunchedEffect
        }
        while (true) {
            val now = System.currentTimeMillis()
            val diff = penaltyEndTime - now
            if (diff <= 0) {
                timeLeft = "00:00:00"
            } else {
                val hours = diff / (1000 * 60 * 60)
                val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
                val seconds = (diff % (1000 * 60)) / 1000
                timeLeft = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }
            delay(1000)
        }
    }

    
    BackHandler(enabled = true) {
        
    }

    val backgroundColor = if (isPenaltyActive) Color.Black else Color.DarkGray
    val iconColor = if (isPenaltyActive) Color.Red else Color.Cyan
    val titleText = if (isPenaltyActive) "SYSTEM LOCKED" else "FOCUS MODE"
    val subtitleText = if (isPenaltyActive) "Penalty Active" else "Pending Tasks"
    val messageText = if (isPenaltyActive) "Complete your tasks to earn your freedom." else "Distractions blocked. Complete tasks to unlock."

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = iconColor,
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = titleText,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = subtitleText,
                color = iconColor,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
            
            if (isPenaltyActive) {
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    text = timeLeft,
                    color = Color.White,
                    fontSize = 48.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = messageText,
                color = Color.Gray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
