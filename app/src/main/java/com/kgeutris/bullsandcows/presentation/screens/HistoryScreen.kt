package com.kgeutris.bullsandcows.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kgeutris.bullsandcows.domain.model.GameMode
import com.kgeutris.bullsandcows.presentation.viewmodel.GameSessionUiItem
import com.kgeutris.bullsandcows.presentation.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val items by viewModel.sessions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("История пуста", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .statusBarsPadding(), // Добавить отступ сверху под статус-бар
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    when (item) {
                        is GameSessionUiItem.DateHeader -> {
                            item {  // ← теперь это item() из LazyListScope
                                Text(
                                    text = item.date,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        is GameSessionUiItem.SessionItem -> {
                            item {  // ← теперь это item() из LazyListScope
                                HistoryItem(session = item.session)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(session: com.kgeutris.bullsandcows.domain.model.GameSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (session.isWin) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Режим + длина
                Text(
                    text = "${session.mode.label} • ${session.numberLength} цифры",
                    style = MaterialTheme.typography.titleMedium
                )
                // Попыток
                Text(
                    text = "Ходов: ${session.attemptsCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Победитель (только для мультиплеера)
                if (session.mode == GameMode.MULTIPLAYER && session.winnerName != null) {
                    Text(
                        text = "Победитель: ${session.winnerName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Результат: ✅ / ❌
            Icon(
                imageVector = if (session.isWin) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (session.isWin) "Победа" else "Поражение",
                tint = if (session.isWin) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }
    }
}

private val GameMode.label: String
    get() = when (this) {
        GameMode.SINGLE -> "Одиночная"
        GameMode.AI_GUESSING -> "AI угадывает"
        GameMode.MULTIPLAYER -> "Мультиплеер"
    }