package com.james.anvil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.ui.theme.GradientStart
import com.james.anvil.ui.theme.GradientEnd
import com.james.anvil.ui.theme.ProgressTrackLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MotivationCard(
    dailyProgress: Float,
    pendingCount: Int,
    quote: String,
    modifier: Modifier = Modifier
) {
    // Modern Date Format
    val dateString = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    val gradient = Brush.linearGradient(
        colors = listOf(GradientStart, GradientEnd)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp) // Slightly taller for better spacing
            .clip(RoundedCornerShape(28.dp)) // More rounded
            .background(gradient)
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column {
                Text(
                    text = dateString.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Progress Summary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${(dailyProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.displaySmall.copy(
                            color = Color.White,
                            fontSize = 32.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Complete",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                }
            }

            // Quote Section
            // Using a slightly smaller font for the quote to ensuring it fits
            Text(
                text = "\"$quote\"",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    lineHeight = 24.sp
                ),
                maxLines = 3,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tasks Pending",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.7f))
                    )
                    Text(
                        text = "$pendingCount",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { dailyProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color.White,
                    trackColor = ProgressTrackLight,
                )
            }
        }
    }
}

// =============================================
// Preview Functions
// =============================================

@Preview(name = "Motivation Card - Preview", showBackground = true)
@Composable
private fun MotivationCardPreview() {
    com.james.anvil.ui.theme.ANVILTheme {
        MotivationCard(
            dailyProgress = 0.65f,
            pendingCount = 5,
            quote = "Obsession is going to beat talent every time."
        )
    }
}
