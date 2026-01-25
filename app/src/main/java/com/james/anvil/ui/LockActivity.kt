package com.james.anvil.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.james.anvil.service.BlockType

class LockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BLOCK_TYPE = "extra_block_type"
        const val EXTRA_BLOCKED_TARGET = "extra_blocked_target"
    }

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
        
        // Get block type from intent
        val blockTypeName = intent.getStringExtra(EXTRA_BLOCK_TYPE)
        val blockType = try {
            BlockType.valueOf(blockTypeName ?: BlockType.PENALTY.name)
        } catch (e: Exception) {
            BlockType.PENALTY
        }
        
        // Get blocked target from intent
        val blockedTarget = intent.getStringExtra(EXTRA_BLOCKED_TARGET) ?: "Unknown"
        
        enableEdgeToEdge()
        setContent {
            ANVILTheme {
                LockScreen(penaltyManager, taskDao, blockType, blockedTarget)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
    }
}

// Stark, serious color palette
private val PureBlack = Color(0xFF000000)
private val DarkGray = Color(0xFF0A0A0A)
private val MediumGray = Color(0xFF1A1A1A)
private val BorderGray = Color(0xFF2A2A2A)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF808080)
private val TextDimGray = Color(0xFF505050)
private val WarningRed = Color(0xFFCC0000)
private val CautionAmber = Color(0xFFCC8800)

@Composable
fun LockScreen(
    penaltyManager: PenaltyManager, 
    taskDao: com.james.anvil.data.TaskDao? = null,
    blockType: BlockType = BlockType.PENALTY,
    blockedTarget: String = "Unknown"
) {
    var hoursLeft by remember { mutableStateOf("00") }
    var minutesLeft by remember { mutableStateOf("00") }
    var secondsLeft by remember { mutableStateOf("00") }
    val isPenaltyActive = remember { penaltyManager.isPenaltyActive() }
    val penaltyEndTime = remember { penaltyManager.getPenaltyEndTime() }
    var blockingTaskNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var blockingReason by remember { mutableStateOf("") }
    
    // Determine if this is a schedule-based block (separate from penalty mode)
    val isScheduleBlock = blockType == BlockType.SCHEDULE

    // Fetch blocking tasks
    LaunchedEffect(taskDao) {
        taskDao?.let { dao ->
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val hardnessViolations = dao.getTasksViolatingHardness(now)
                if (hardnessViolations.isNotEmpty()) {
                    blockingTaskNames = hardnessViolations.take(3).map { it.title }
                    blockingReason = "DEADLINE VIOLATION"
                } else {
                    val overdueTasks = dao.getOverdueIncomplete(now)
                    if (overdueTasks.isNotEmpty()) {
                        blockingTaskNames = overdueTasks.take(3).map { it.title }
                        blockingReason = "OVERDUE TASKS"
                    }
                }
            }
        }
    }
    
    // Timer update
    LaunchedEffect(key1 = penaltyEndTime, key2 = isPenaltyActive) {
        if (!isPenaltyActive) {
            hoursLeft = "--"
            minutesLeft = "--"
            secondsLeft = "--"
            return@LaunchedEffect
        }
        while (true) {
            val now = System.currentTimeMillis()
            val diff = penaltyEndTime - now
            if (diff <= 0) {
                hoursLeft = "00"
                minutesLeft = "00"
                secondsLeft = "00"
            } else {
                val h = diff / (1000 * 60 * 60)
                val m = (diff % (1000 * 60 * 60)) / (1000 * 60)
                val s = (diff % (1000 * 60)) / 1000
                hoursLeft = String.format("%02d", h)
                minutesLeft = String.format("%02d", m)
                secondsLeft = String.format("%02d", s)
            }
            delay(1000)
        }
    }

    // Block back button
    BackHandler(enabled = true) { }

    // Color depends on block type: schedule blocks use amber, penalty uses red
    val accentColor = when {
        isPenaltyActive -> WarningRed
        isScheduleBlock -> CautionAmber
        else -> CautionAmber
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkGray, RoundedCornerShape(4.dp))
                    .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUS",
                        color = TextDimGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(accentColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isPenaltyActive -> "LOCKED"
                                isScheduleBlock -> "SCHEDULED"
                                else -> "BLOCKED"
                            },
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Block icon - static, no animation
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(DarkGray)
                    .border(2.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Blocked",
                    tint = accentColor,
                    modifier = Modifier.size(52.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = when {
                    isPenaltyActive -> "ACCESS DENIED"
                    isScheduleBlock -> "ACCESS RESTRICTED"
                    else -> "ACCESS RESTRICTED"
                },
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Reason
            Text(
                text = when {
                    isPenaltyActive -> "Penalty enforcement active"
                    isScheduleBlock -> "Scheduled block active"
                    else -> "Task completion required"
                },
                color = TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            
            // Blocked Target Card - shows what was blocked
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MediumGray, RoundedCornerShape(4.dp))
                    .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BLOCKED TARGET",
                        color = TextDimGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = blockedTarget,
                        color = accentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Blocking reason card - only show for penalty/task-based blocks, not schedule blocks
            if (!isPenaltyActive && !isScheduleBlock && blockingTaskNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkGray, RoundedCornerShape(4.dp))
                        .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = CautionAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = blockingReason,
                                color = CautionAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        HorizontalDivider(color = BorderGray, thickness = 1.dp)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        blockingTaskNames.forEachIndexed { index, taskName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "[${index + 1}]",
                                    color = TextDimGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = taskName,
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        if (blockingTaskNames.size >= 3) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "...",
                                color = TextDimGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
            
            // Timer (only in penalty mode)
            if (isPenaltyActive) {
                Spacer(modifier = Modifier.height(40.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkGray, RoundedCornerShape(4.dp))
                        .border(1.dp, WarningRed.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(vertical = 24.dp, horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LOCKOUT REMAINING",
                            color = TextDimGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Timer display
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimerBlock(value = hoursLeft, label = "HRS")
                            TimerSeparator()
                            TimerBlock(value = minutesLeft, label = "MIN")
                            TimerSeparator()
                            TimerBlock(value = secondsLeft, label = "SEC")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Message
            Text(
                text = when {
                    isPenaltyActive -> "Complete outstanding tasks to restore access."
                    isScheduleBlock -> "This app/site is blocked during scheduled hours."
                    else -> "Complete pending tasks to unlock this device."
                },
                color = TextDimGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Footer
            Text(
                text = "ANVIL ENFORCEMENT SYSTEM",
                color = TextDimGray.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TimerBlock(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .background(PureBlack, RoundedCornerShape(4.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = value,
                color = TextWhite,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TextDimGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun TimerSeparator() {
    Text(
        text = ":",
        color = TextDimGray,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)
    )
}
