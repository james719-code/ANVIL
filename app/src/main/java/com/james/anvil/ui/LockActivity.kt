package com.james.anvil.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.core.PenaltyManager
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.ui.theme.ANVILTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class LockActivity : ComponentActivity() {

    private lateinit var penaltyManager: PenaltyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        penaltyManager = PenaltyManager(this)
        val database = AnvilDatabase.getDatabase(this)
        val taskDao = database.taskDao()
        
        // Prevent screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        enableEdgeToEdge()
        setContent {
            ANVILTheme {
                LockScreen(penaltyManager, taskDao)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
    }
}

// Premium color palette
private val DeepNavy = Color(0xFF0A0E21)
private val DarkSlate = Color(0xFF1A1F3A)
private val AccentBlue = Color(0xFF4FC3F7)
private val AccentTeal = Color(0xFF00BFA5)
private val PenaltyRed = Color(0xFFFF5252)
private val PenaltyDarkRed = Color(0xFF2D0A0A)
private val GlassWhite = Color(0x22FFFFFF)
private val GlassBorder = Color(0x33FFFFFF)

@Composable
fun LockScreen(penaltyManager: PenaltyManager, taskDao: com.james.anvil.data.TaskDao? = null) {
    var timeLeft by remember { mutableStateOf("...") }
    val isPenaltyActive = remember { penaltyManager.isPenaltyActive() }
    val penaltyEndTime = remember { penaltyManager.getPenaltyEndTime() }
    var blockingTaskNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var blockingReason by remember { mutableStateOf("") }

    // Fetch blocking tasks
    LaunchedEffect(taskDao) {
        taskDao?.let { dao ->
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val hardnessViolations = dao.getTasksViolatingHardness(now)
                if (hardnessViolations.isNotEmpty()) {
                    blockingTaskNames = hardnessViolations.take(3).map { it.title }
                    blockingReason = "Hardness deadline approaching"
                } else {
                    val overdueTasks = dao.getOverdueIncomplete(now)
                    if (overdueTasks.isNotEmpty()) {
                        blockingTaskNames = overdueTasks.take(3).map { it.title }
                        blockingReason = "Overdue tasks"
                    }
                }
            }
        }
    }
    
    // Pulsing animation for icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )
    
    // Glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    // Timer update
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

    // Block back button
    BackHandler(enabled = true) { }

    val accentColor = if (isPenaltyActive) PenaltyRed else AccentTeal
    val gradientColors = if (isPenaltyActive) {
        listOf(PenaltyDarkRed, DeepNavy, DarkSlate)
    } else {
        listOf(DeepNavy, DarkSlate, Color(0xFF0D1B2A))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
    ) {
        // Subtle gradient orb in background
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-100).dp, y = (-150).dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated lock icon with glow
            Box(contentAlignment = Alignment.Center) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(scale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = glowAlpha * 0.4f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                // Glass circle background
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(GlassWhite)
                        .border(1.dp, GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = accentColor,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Title
            Text(
                text = if (isPenaltyActive) "SYSTEM LOCKED" else "FOCUS MODE",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Subtitle badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = accentColor.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPenaltyActive) "Penalty Active" else "Tasks Pending",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Blocking reason card (non-penalty mode)
            if (!isPenaltyActive && blockingTaskNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    shape = RoundedCornerShape(16.dp),
                    color = GlassWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = blockingReason,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        blockingTaskNames.forEach { taskName ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(accentColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = taskName,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (blockingTaskNames.size >= 3) {
                            Text(
                                text = "+ more tasks...",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Timer (only in penalty mode)
            if (isPenaltyActive) {
                Spacer(modifier = Modifier.height(48.dp))
                
                // Glassmorphism timer card
                Surface(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    shape = RoundedCornerShape(24.dp),
                    color = GlassWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Time Remaining",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = timeLeft,
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Message
            Text(
                text = if (isPenaltyActive) 
                    "Complete your tasks to earn your freedom."
                else 
                    "Complete your pending tasks to unlock distractions.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tip
            Text(
                text = "💡 Open ANVIL to manage your tasks",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
