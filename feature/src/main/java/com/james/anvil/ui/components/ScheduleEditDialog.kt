package com.james.anvil.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    currentStartDayOfWeek: Int? = null,
    currentEndDayOfWeek: Int? = null,
    onDismiss: () -> Unit,
    onSave: (BlockScheduleType, Int, Int, Int, Int?, Int?) -> Unit
) {
    var scheduleType by remember { mutableStateOf(currentScheduleType) }
    var dayMask by remember { mutableIntStateOf(currentDayMask) }
    var startMinutes by remember { mutableIntStateOf(currentStartMinutes) }
    var endMinutes by remember { mutableIntStateOf(currentEndMinutes) }
    var startDayOfWeek by remember { mutableIntStateOf(currentStartDayOfWeek ?: java.util.Calendar.SUNDAY) }
    var endDayOfWeek by remember { mutableIntStateOf(currentEndDayOfWeek ?: java.util.Calendar.SATURDAY) }
    
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
                    contentDescription = "Schedule",
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
                SectionHeader("Schedule Type", Icons.Default.Today)
                
                ScheduleTypeSelector(
                    selectedType = scheduleType,
                    onTypeSelected = { type ->
                        scheduleType = type
                        when (type) {
                            BlockScheduleType.EVERYDAY -> dayMask = DayOfWeekMask.ALL_DAYS
                            BlockScheduleType.WEEKDAYS -> dayMask = DayOfWeekMask.WEEKDAYS_MASK
                            BlockScheduleType.CUSTOM -> { /* Keep current */ }
                            BlockScheduleType.CUSTOM_RANGE -> { /* Keep current */ }
                        }
                    }
                )

                // Custom Day Selection (only when Custom is selected)
                AnimatedVisibility(
                    visible = scheduleType == BlockScheduleType.CUSTOM,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Select Days", Icons.Default.ViewWeek)
                        DaySelector(
                            selectedMask = dayMask,
                            onDayToggle = { day ->
                                dayMask = dayMask xor day
                            }
                        )
                    }
                }

                // Custom Range Selection (only when Custom Range is selected)
                AnimatedVisibility(
                    visible = scheduleType == BlockScheduleType.CUSTOM_RANGE,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SectionHeader("Select Range", Icons.Default.DateRange)
                        
                        // From section
                        RangeCard(
                            title = "From",
                            selectedDay = startDayOfWeek,
                            onDaySelected = { startDayOfWeek = it },
                            time = formatMinutesToTime(startMinutes),
                            onTimeClick = { showStartTimePicker = true }
                        )
                        
                        // Arrow indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowForward,
                                        contentDescription = "to",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(2.dp)
                                    )
                                }
                            }
                        }
                        
                        // To section
                        RangeCard(
                            title = "To",
                            selectedDay = endDayOfWeek,
                            onDaySelected = { endDayOfWeek = it },
                            time = formatMinutesToTime(endMinutes),
                            onTimeClick = { showEndTimePicker = true }
                        )
                    }
                }

                // Time Range Section (only for non-CUSTOM_RANGE types)
                if (scheduleType != BlockScheduleType.CUSTOM_RANGE) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )

                    // Time Range Section
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Time Range", Icons.Default.AccessTime)
                        
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDayMask = if (scheduleType == BlockScheduleType.CUSTOM && dayMask == 0) {
                        DayOfWeekMask.ALL_DAYS
                    } else dayMask
                    
                    val finalStartDay = if (scheduleType == BlockScheduleType.CUSTOM_RANGE) startDayOfWeek else null
                    val finalEndDay = if (scheduleType == BlockScheduleType.CUSTOM_RANGE) endDayOfWeek else null
                    
                    onSave(scheduleType, finalDayMask, startMinutes, endMinutes, finalStartDay, finalEndDay)
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
private fun SectionHeader(text: String, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ScheduleTypeSelector(
    selectedType: BlockScheduleType,
    onTypeSelected: (BlockScheduleType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScheduleTypeOption(
            icon = Icons.Default.CalendarMonth,
            label = "Everyday",
            description = "Block every day of the week",
            isSelected = selectedType == BlockScheduleType.EVERYDAY,
            onClick = { onTypeSelected(BlockScheduleType.EVERYDAY) }
        )
        ScheduleTypeOption(
            icon = Icons.Default.WorkHistory,
            label = "Weekdays",
            description = "Monday through Friday",
            isSelected = selectedType == BlockScheduleType.WEEKDAYS,
            onClick = { onTypeSelected(BlockScheduleType.WEEKDAYS) }
        )
        ScheduleTypeOption(
            icon = Icons.Default.ViewWeek,
            label = "Custom",
            description = "Choose specific days",
            isSelected = selectedType == BlockScheduleType.CUSTOM,
            onClick = { onTypeSelected(BlockScheduleType.CUSTOM) }
        )
        ScheduleTypeOption(
            icon = Icons.Default.DateRange,
            label = "Custom Range",
            description = "From day+time to day+time",
            isSelected = selectedType == BlockScheduleType.CUSTOM_RANGE,
            onClick = { onTypeSelected(BlockScheduleType.CUSTOM_RANGE) }
        )
    }
}

@Composable
private fun ScheduleTypeOption(
    icon: ImageVector,
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(200),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(200),
        label = "bg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(200),
        label = "border"
    )
    
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "icon"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Custom styled radio indicator with icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = iconTint
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Selection indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = ButtonDefaults.outlinedButtonBorder,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDropdown(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val days = listOf(
        java.util.Calendar.SUNDAY to "Sunday",
        java.util.Calendar.MONDAY to "Monday",
        java.util.Calendar.TUESDAY to "Tuesday",
        java.util.Calendar.WEDNESDAY to "Wednesday",
        java.util.Calendar.THURSDAY to "Thursday",
        java.util.Calendar.FRIDAY to "Friday",
        java.util.Calendar.SATURDAY to "Saturday"
    )
    
    val selectedDayName = days.find { it.first == selectedDay }?.second ?: "Sunday"
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedDayName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = RoundedCornerShape(12.dp)
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            days.forEach { (dayValue, dayName) ->
                DropdownMenuItem(
                    text = { Text(dayName) },
                    onClick = {
                        onDaySelected(dayValue)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeCard(
    title: String,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    time: String,
    onTimeClick: () -> Unit
) {
    val days = listOf(
        java.util.Calendar.SUNDAY to "Sunday",
        java.util.Calendar.MONDAY to "Monday",
        java.util.Calendar.TUESDAY to "Tuesday",
        java.util.Calendar.WEDNESDAY to "Wednesday",
        java.util.Calendar.THURSDAY to "Thursday",
        java.util.Calendar.FRIDAY to "Friday",
        java.util.Calendar.SATURDAY to "Saturday"
    )
    
    val selectedDayName = days.find { it.first == selectedDay }?.second ?: "Sunday"
    var dayExpanded by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Day and Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day Dropdown
                ExposedDropdownMenuBox(
                    expanded = dayExpanded,
                    onExpandedChange = { dayExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .clickable { dayExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = selectedDayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded)
                        }
                    }
                    
                    ExposedDropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false }
                    ) {
                        days.forEach { (dayValue, dayName) ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = dayName,
                                        fontWeight = if (dayValue == selectedDay) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                },
                                onClick = {
                                    onDaySelected(dayValue)
                                    dayExpanded = false
                                },
                                leadingIcon = if (dayValue == selectedDay) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null,
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
                
                // Time Button
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = ButtonDefaults.outlinedButtonBorder,
                    onClick = onTimeClick
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Select time",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
