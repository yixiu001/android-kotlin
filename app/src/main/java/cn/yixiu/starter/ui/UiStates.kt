package cn.yixiu.starter.ui

import cn.yixiu.starter.R

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable fun LoadingState(message: String) {
    InfoCard {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable fun EmptyState(title: String, description: String) {
    InfoCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun ErrorState(message: String, retryLabel: String? = null, retry: (() -> Unit)? = null) {
    val copy = LocalResources.current
    InfoCard {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        if (retry != null) OutlinedButton(onClick = retry) { Text(retryLabel ?: copy.getString(R.string.retry)) }
    }
}
