package com.james.anvil.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.data.FocusSession
import com.james.anvil.ui.theme.*
import com.james.anvil.ui.viewmodel.FocusPhase
import com.james.anvil.ui.viewmodel.FocusSessionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionScreen(
    viewModel: FocusSessionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val phase by viewModel.phase.collectAsState()
    val currentRound by viewModel.currentRound.collectAsState()
    val totalRounds by viewModel.totalRounds.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val totalSeconds by viewModel.totalSeconds.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val workMinutes by viewModel.workMinutes.collectAsState()
    val breakMinutes by viewModel.breakMinutes.collectAsState()

    val todayFocusMinutes by viewModel.todayFocusMinutes.collectAsState()
    val totalFocusMinutes by viewModel.totalFocusMinutes.collectAsState()
    val totalSessionCount by viewModel.totalSessionCount.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forge Session", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Timer card
            item {
                TimerCard(
                    phase = phase,
                    currentRound = currentRound,
                    totalRounds = totalRounds,
                    remainingSeconds = remainingSeconds,
                    totalSeconds = totalSeconds,
                    isRunning = isRunning,
                    workMinutes = workMinutes,
                    breakMinutes = breakMinutes,
                    onStart = { viewModel.startSession() },
                    onStop = { viewModel.stopSession() },
                    onReset = { viewModel.resetToIdle() }
                )
            }

            // Settings (only show when idle)
            if (phase == FocusPhase.IDLE) {
                item {
                    SettingsCard(
                        workMinutes = workMinutes,
                        breakMinutes = breakMinutes,
                        totalRounds = totalRounds,
                        onWorkChange = { viewModel.setWorkMinutes(it) },
                        onBreakChange = { viewModel.setBreakMinutes(it) },
                        onRoundsChange = { viewModel.setTotalRounds(it) }
                    )
                }
            }

            // Today's stats
            item {
                StatsRow(
                    todayMinutes = todayFocusMinutes,
                    totalMinutes = totalFocusMinutes,
                    totalSessions = totalSessionCount
                )
            }

            // Recent sessions
            if (recentSessions.isNotEmpty()) {
                item {
                    Text(
                        "Recent Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(recentSessions) { session ->
                    SessionHistoryItem(session)
                }
            }
        }
    }
}

@Composable
private fun TimerCard(
    phase: FocusPhase,
    currentRound: Int,
    totalRounds: Int,
    remainingSeconds: Long,
    totalSeconds: Long,
    isRunning: Boolean,
    workMinutes: Int,
    breakMinutes: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 1f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "timer_progress"
    )

    val phaseColor = when (phase) {
        FocusPhase.WORK -> ElectricBlue
        FocusPhase.BREAK -> Color(0xFF4CAF50)
        FocusPhase.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
        FocusPhase.FINISHED -> ForgedGold
    }

    val phaseGradient = when (phase) {
        FocusPhase.WORK -> Brush.linearGradient(listOf(ElectricBlue, Color(0xFF7C4DFF)))
        FocusPhase.BREAK -> Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF81C784)))
        FocusPhase.FINISHED -> Brush.linearGradient(listOf(ForgedGold, ForgedGoldLight))
        FocusPhase.IDLE -> Brush.linearGradient(listOf(
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        ))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Phase label
            Text(
                text = when (phase) {
                    FocusPhase.IDLE -> "Ready to Focus"
                    FocusPhase.WORK -> "FORGING"
                    FocusPhase.BREAK -> "COOLING DOWN"
                    FocusPhase.FINISHED -> "SESSION COMPLETE!"
                },
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                color = phaseColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Timer ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val arcSize = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Background ring
                    drawArc(
                        color = phaseColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress ring
                    if (totalSeconds > 0) {
                        drawArc(
                            brush = phaseGradient,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val displaySeconds = if (phase == FocusPhase.IDLE) {
                        workMinutes * 60L
                    } else {
                        remainingSeconds
                    }
                    val minutes = displaySeconds / 60
                    val secs = displaySeconds % 60

                    Text(
                        text = String.format("%02d:%02d", minutes, secs),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (phase != FocusPhase.IDLE && phase != FocusPhase.FINISHED) {
                        Text(
                            text = "Round $currentRound / $totalRounds",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (phase) {
                    FocusPhase.IDLE -> {
                        Button(
                            onClick = onStart,
                            modifier = Modifier.height(56.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Forging", fontWeight = FontWeight.Bold)
                        }
                    }
                    FocusPhase.WORK, FocusPhase.BREAK -> {
                        FilledTonalButton(
                            onClick = onStop,
                            modifier = Modifier.height(56.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop", fontWeight = FontWeight.Bold)
                        }
                    }
                    FocusPhase.FINISHED -> {
                        Button(
                            onClick = onReset,
                            modifier = Modifier.height(56.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForgedGold
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "New Session")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("New Session", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    workMinutes: Int,
    breakMinutes: Int,
    totalRounds: Int,
    onWorkChange: (Int) -> Unit,
    onBreakChange: (Int) -> Unit,
    onRoundsChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Session Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            SettingRow("Work", workMinutes, "min", 5, 60, onWorkChange)
            SettingRow("Break", breakMinutes, "min", 1, 30, onBreakChange)
            SettingRow("Rounds", totalRounds, "", 1, 8, onRoundsChange)
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: Int,
    unit: String,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (value > min) onChange(value - 1) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
            }

            Text(
                text = if (unit.isNotEmpty()) "$value $unit" else "$value",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 64.dp)
            )

            IconButton(
                onClick = { if (value < max) onChange(value + 1) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StatsRow(
    todayMinutes: Int,
    totalMinutes: Int,
    totalSessions: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FocusStatChip(
            label = "Today",
            value = "${todayMinutes}m",
            modifier = Modifier.weight(1f)
        )
        FocusStatChip(
            label = "Total",
            value = formatMinutesToHours(totalMinutes),
            modifier = Modifier.weight(1f)
        )
        FocusStatChip(
            label = "Sessions",
            value = "$totalSessions",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FocusStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ElectricBlue
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionHistoryItem(session: FocusSession) {
    val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (session.isCompleted)
                            ElectricBlue.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.errorContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (session.isCompleted) Icons.Default.CheckCircle else Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (session.isCompleted) ElectricBlue else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${session.totalFocusMinutes} min focus",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${session.sessionsCompleted} rounds • ${dateFormat.format(Date(session.startTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (session.isCompleted) {
                Text(
                    text = "+30 XP",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ForgedGold
                )
            }
        }
    }
}

private fun formatMinutesToHours(minutes: Int): String {
    return if (minutes >= 60) {
        val hours = minutes / 60
        val mins = minutes % 60
        if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
    } else {
        "${minutes}m"
    }
}
