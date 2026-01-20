package com.james.anvil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.data.BlockScheduleType
import com.james.anvil.data.DayOfWeekMask

/**
 * Dialog for editing the schedule of a blocked app or link.
 * Allows selecting:
 * - Schedule type (Everyday, Weekdays, Custom)
 * - Custom days when type is Custom
 * - Time range (start and end time)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditDialog(
    title: String,
    currentScheduleType: BlockScheduleType,
    currentDayMask: Int,
    currentStartMinutes: Int,
    currentEndMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (BlockScheduleType, Int, Int, Int) -> Unit
) {
    var scheduleType by remember { mutableStateOf(currentScheduleType) }
    var dayMask by remember { mutableIntStateOf(currentDayMask) }
    var startMinutes by remember { mutableIntStateOf(currentStartMinutes) }
    var endMinutes by remember { mutableIntStateOf(currentEndMinutes) }
    
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Schedule Type Selection
                SectionHeader("Schedule Type")
                
                ScheduleTypeSelector(
                    selectedType = scheduleType,
                    onTypeSelected = { type ->
                        scheduleType = type
                        when (type) {
                            BlockScheduleType.EVERYDAY -> dayMask = DayOfWeekMask.ALL_DAYS
                            BlockScheduleType.WEEKDAYS -> dayMask = DayOfWeekMask.WEEKDAYS_MASK
                            BlockScheduleType.CUSTOM -> { /* Keep current */ }
                        }
                    }
                )

                // Custom Day Selection (only when Custom is selected)
                if (scheduleType == BlockScheduleType.CUSTOM) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Select Days")
                        DaySelector(
                            selectedMask = dayMask,
                            onDayToggle = { day ->
                                dayMask = dayMask xor day
                            }
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )

                // Time Range Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Time Range")
                    
                    // All Day Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                val isAllDay = startMinutes == 0 && endMinutes == 1439
                                if (!isAllDay) {
                                    startMinutes = 0
                                    endMinutes = 1439
                                } else {
                                    startMinutes = 8 * 60  // 8:00 AM
                                    endMinutes = 18 * 60   // 6:00 PM
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Block all day",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = startMinutes == 0 && endMinutes == 1439,
                            onCheckedChange = { allDay ->
                                if (allDay) {
                                    startMinutes = 0
                                    endMinutes = 1439
                                } else {
                                    startMinutes = 8 * 60
                                    endMinutes = 18 * 60
                                }
                            }
                        )
                    }

                    // Time Pickers (only visible when not all day)
                    if (!(startMinutes == 0 && endMinutes == 1439)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start Time
                            TimePickerButton(
                                label = "From",
                                time = formatMinutesToTime(startMinutes),
                                onClick = { showStartTimePicker = true },
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "→",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // End Time
                            TimePickerButton(
                                label = "Until",
                                time = formatMinutesToTime(endMinutes),
                                onClick = { showEndTimePicker = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDayMask = if (scheduleType == BlockScheduleType.CUSTOM && dayMask == 0) {
                        DayOfWeekMask.ALL_DAYS
                    } else dayMask
                    onSave(scheduleType, finalDayMask, startMinutes, endMinutes)
                },
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Save", modifier = Modifier.padding(horizontal = 16.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )

    // Time Picker Dialogs
    if (showStartTimePicker) {
        TimePickerDialog(
            title = "Block From",
            initialHour = startMinutes / 60,
            initialMinute = startMinutes % 60,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                startMinutes = hour * 60 + minute
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            title = "Block Until",
            initialHour = endMinutes / 60,
            initialMinute = endMinutes % 60,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                endMinutes = hour * 60 + minute
                showEndTimePicker = false
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ScheduleTypeSelector(
    selectedType: BlockScheduleType,
    onTypeSelected: (BlockScheduleType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScheduleTypeOption(
            label = "Everyday",
            description = "Block every day of the week",
            isSelected = selectedType == BlockScheduleType.EVERYDAY,
            onClick = { onTypeSelected(BlockScheduleType.EVERYDAY) }
        )
        ScheduleTypeOption(
            label = "Weekdays",
            description = "Monday through Friday",
            isSelected = selectedType == BlockScheduleType.WEEKDAYS,
            onClick = { onTypeSelected(BlockScheduleType.WEEKDAYS) }
        )
        ScheduleTypeOption(
            label = "Custom",
            description = "Choose specific days",
            isSelected = selectedType == BlockScheduleType.CUSTOM,
            onClick = { onTypeSelected(BlockScheduleType.CUSTOM) }
        )
    }
}

@Composable
private fun ScheduleTypeOption(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DaySelector(
    selectedMask: Int,
    onDayToggle: (Int) -> Unit
) {
    val daysRow1 = listOf(
        Triple("Sun", DayOfWeekMask.SUNDAY, true),
        Triple("Mon", DayOfWeekMask.MONDAY, false),
        Triple("Tue", DayOfWeekMask.TUESDAY, false),
        Triple("Wed", DayOfWeekMask.WEDNESDAY, false)
    )
    val daysRow2 = listOf(
        Triple("Thu", DayOfWeekMask.THURSDAY, false),
        Triple("Fri", DayOfWeekMask.FRIDAY, false),
        Triple("Sat", DayOfWeekMask.SATURDAY, true)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // First row: Sun, Mon, Tue, Wed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            daysRow1.forEach { (label, dayMask, isWeekend) ->
                val isSelected = (selectedMask and dayMask) != 0
                DayChip(
                    label = label,
                    isSelected = isSelected,
                    onClick = { onDayToggle(dayMask) },
                    isWeekend = isWeekend,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Second row: Thu, Fri, Sat (centered with spacer)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add flexible spacer at start to center the 3 items
            Spacer(modifier = Modifier.weight(0.5f))
            daysRow2.forEach { (label, dayMask, isWeekend) ->
                val isSelected = (selectedMask and dayMask) != 0
                DayChip(
                    label = label,
                    isSelected = isSelected,
                    onClick = { onDayToggle(dayMask) },
                    isWeekend = isWeekend,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

@Composable
private fun DayChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isWeekend: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isWeekend -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimePickerButton(
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("OK", modifier = Modifier.padding(horizontal = 16.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

private fun formatMinutesToTime(minutes: Int): String {
    val hour = minutes / 60
    val minute = minutes % 60
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%d:%02d %s", displayHour, minute, period)
}
