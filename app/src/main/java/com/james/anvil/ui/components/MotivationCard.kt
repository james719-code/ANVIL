package com.james.anvil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.ui.theme.DeepSkyBlue
import com.james.anvil.ui.theme.ProgressTrackLight
import com.james.anvil.ui.theme.ProgressTrackDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MotivationCard(
    dailyProgress: Float,
    pendingCount: Int,
    quote: String,
    modifier: Modifier = Modifier
) {
    val dateString = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp), 
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSkyBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            Text(
                text = "Today, $dateString",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            )

            
            Text(
                text = "\"$quote\"",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                maxLines = 3
            )

            
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$pendingCount tasks pending",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f))
                    )
                    Text(
                        text = "${(dailyProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { dailyProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color.White,
                    trackColor = ProgressTrackLight,
                )
            }
        }
    }
}
