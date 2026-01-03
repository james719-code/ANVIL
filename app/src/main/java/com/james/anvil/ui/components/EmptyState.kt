package com.james.anvil.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock

@Composable
fun EmptyState(
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =============================================
// Preview Functions (Removed in Release Builds)
// =============================================

@Preview(name = "Empty State - Tasks", showBackground = true)
@Composable
private fun EmptyStateTasksPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = false) {
        EmptyState(
            message = "No pending tasks. You are free.",
            icon = Icons.Default.Check
        )
    }
}

@Preview(name = "Empty State - Blocked Links", showBackground = true)
@Composable
private fun EmptyStateLinksPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = false) {
        EmptyState(
            message = "No blocked links.",
            icon = Icons.Default.Lock
        )
    }
}

@Preview(name = "Empty State - Dark", showBackground = true)
@Composable
private fun EmptyStateDarkPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = true) {
        EmptyState(
            message = "No items to display",
            icon = Icons.Default.Check
        )
    }
}
