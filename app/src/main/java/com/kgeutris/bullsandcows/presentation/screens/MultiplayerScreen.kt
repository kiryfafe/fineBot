package com.kgeutris.bullsandcows.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kgeutris.bullsandcows.presentation.components.NumericKeypad
import com.kgeutris.bullsandcows.presentation.viewmodel.MultiplayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerScreen(
    viewModel: MultiplayerViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Состояние скролла для истории каждого игрока
    val player1ListState = rememberLazyListState()
    val player2ListState = rememberLazyListState()

    // Автоскрытие ошибки
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            delay(2000)
            viewModel.clearError()
        }
    }

    // Автоскролл к новым попыткам (к началу, так как reverseLayout = true)
    LaunchedEffect(uiState.historyP1.size) {
        if (uiState.historyP1.isNotEmpty()) {
            delay(100)
            player1ListState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(uiState.historyP2.size) {
        if (uiState.historyP2.isNotEmpty()) {
            delay(100)
            player2ListState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState.phase) {
                            MultiplayerViewModel.Phase.EnterNames -> "Мультиплеер"
                            MultiplayerViewModel.Phase.SelectLength -> "Длина числа"
                            MultiplayerViewModel.Phase.Player1SetSecret -> "${uiState.player1Name} загадывает"
                            MultiplayerViewModel.Phase.Player2SetSecret -> "${uiState.player2Name} загадывает"
                            MultiplayerViewModel.Phase.Player1Guessing -> "${uiState.player1Name} угадывает"
                            MultiplayerViewModel.Phase.Player2Guessing -> "${uiState.player2Name} угадывает"
                            MultiplayerViewModel.Phase.GameFinished -> "Игра завершена"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Верхняя часть с контентом
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.error?.let { errorMsg ->
                    Snackbar(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(errorMsg, style = MaterialTheme.typography.bodySmall)
                    }
                }

                when (uiState.phase) {
                    MultiplayerViewModel.Phase.EnterNames -> {
                        NameInputScreen(uiState, viewModel)
                    }

                    MultiplayerViewModel.Phase.SelectLength -> {
                        LengthSelectionScreen(uiState, viewModel)
                    }

                    MultiplayerViewModel.Phase.Player1SetSecret -> {
                        SecretInputScreen(
                            title = "${uiState.player1Name}: загадайте число",
                            uiState = uiState
                        )
                    }

                    MultiplayerViewModel.Phase.Player2SetSecret -> {
                        SecretInputScreen(
                            title = "${uiState.player2Name}: загадайте число",
                            uiState = uiState
                        )
                    }

                    MultiplayerViewModel.Phase.Player1Guessing -> {
                        // Используем адаптированный блок истории для игрока 1
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Заголовок и статистика
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "История",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Попыток: ${uiState.historyP1.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (uiState.historyP1.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    state = player1ListState,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    reverseLayout = true // Новые попытки будут вверху
                                ) {
                                    items(uiState.historyP1.reversed()) { (guess, feedback) ->
                                        val (bulls, cows) = feedback
                                        val attemptNumber = uiState.historyP1.size - uiState.historyP1.reversed().indexOf(guess to feedback)
                                        HistoryItem(
                                            guess = guess,
                                            bulls = bulls,
                                            cows = cows,
                                            attemptNumber = attemptNumber
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Здесь будет история попыток",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    MultiplayerViewModel.Phase.Player2Guessing -> {
                        // Используем адаптированный блок истории для игрока 2
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Заголовок и статистика
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "История",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Попыток: ${uiState.historyP2.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (uiState.historyP2.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    state = player2ListState,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    reverseLayout = true // Новые попытки будут вверху
                                ) {
                                    items(uiState.historyP2.reversed()) { (guess, feedback) ->
                                        val (bulls, cows) = feedback
                                        val attemptNumber = uiState.historyP2.size - uiState.historyP2.reversed().indexOf(guess to feedback)
                                        HistoryItem(
                                            guess = guess,
                                            bulls = bulls,
                                            cows = cows,
                                            attemptNumber = attemptNumber
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Здесь будет история попыток",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    MultiplayerViewModel.Phase.GameFinished -> {
                        GameFinishedScreen(uiState, viewModel)
                    }
                }
            }

            // Клавиатура внизу (только для нужных фаз)
            if (uiState.phase in setOf(
                    MultiplayerViewModel.Phase.Player1SetSecret,
                    MultiplayerViewModel.Phase.Player2SetSecret,
                    MultiplayerViewModel.Phase.Player1Guessing,
                    MultiplayerViewModel.Phase.Player2Guessing
                )) {
                NumericKeypad(
                    onDigit = { digit ->
                        viewModel.addDigit(digit)
                    },
                    onDelete = {
                        viewModel.deleteDigit()
                    },
                    onConfirm = {
                        when (uiState.phase) {
                            MultiplayerViewModel.Phase.Player1SetSecret,
                            MultiplayerViewModel.Phase.Player2SetSecret -> {
                                viewModel.confirmSecret()
                            }
                            MultiplayerViewModel.Phase.Player1Guessing,
                            MultiplayerViewModel.Phase.Player2Guessing -> {
                                viewModel.submitGuess()
                            }
                            else -> {}
                        }
                    },
                    currentGuess = uiState.currentGuess,
                    maxLength = uiState.numberLength,
                    alwaysEnableDigits = false,
                    currentDigitsCount = uiState.currentGuess.length,
                    confirmButtonText = when (uiState.phase) {
                        MultiplayerViewModel.Phase.Player1SetSecret,
                        MultiplayerViewModel.Phase.Player2SetSecret -> "✓"
                        MultiplayerViewModel.Phase.Player1Guessing,
                        MultiplayerViewModel.Phase.Player2Guessing -> "Проверить"
                        else -> "✓"
                    },
                    isError = uiState.error != null,
                    isConfirmEnabled = uiState.currentGuess.length == uiState.numberLength,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// Новая компактная карточка для истории (как в AI-версии)
@Composable
private fun HistoryItem(
    guess: String,
    bulls: Int,
    cows: Int,
    attemptNumber: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Номер попытки и число
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "#$attemptNumber",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(24.dp)
                )
                Text(
                    guess,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Результат
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Быки
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            "🐂",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "$bulls",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Коровы
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            "🐄",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "$cows",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NameInputScreen(
    uiState: MultiplayerViewModel.State,
    viewModel: MultiplayerViewModel
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Введите имена игроков", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = uiState.player1Name,
            onValueChange = viewModel::setPlayer1Name,
            label = { Text("Игрок 1") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.player2Name,
            onValueChange = viewModel::setPlayer2Name,
            label = { Text("Игрок 2") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = viewModel::confirmNames,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.player1Name.isNotBlank() && uiState.player2Name.isNotBlank()
        ) {
            Text("Далее", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LengthSelectionScreen(
    uiState: MultiplayerViewModel.State,
    viewModel: MultiplayerViewModel
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Выберите длину числа", style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (3..6).forEach { len ->
                FilterChip(
                    selected = uiState.numberLength == len,
                    onClick = { viewModel.setNumberLength(len) },
                    label = { Text("$len") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Button(
            onClick = viewModel::confirmLength,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Далее", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SecretInputScreen(
    title: String,
    uiState: MultiplayerViewModel.State
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = uiState.currentGuess.ifEmpty { "Введите число" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Число должно состоять из ${uiState.numberLength} неповторяющихся цифр",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun GuessingScreen(
    title: String,
    history: List<Pair<String, Pair<Int, Int>>>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    uiState: MultiplayerViewModel.State
) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = uiState.currentGuess.ifEmpty { "Введите число" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Попробуйте угадать число соперника",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // История попыток (старый вариант - можно удалить или оставить как альтернативу)
        if (history.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "История попыток:",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Кнопка скролла к новым попыткам
                        if (listState.firstVisibleItemIndex > 2) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "К новым попыткам",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "К новым",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        reverseLayout = true
                    ) {
                        items(history) { (guess, feedback) ->
                            val (bulls, cows) = feedback
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                shape = MaterialTheme.shapes.small,
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            guess,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "#${history.indexOf(guess to feedback) + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Text(
                                        "$bulls бык${if (bulls == 1) "" else "а/ов"}, $cows коров${if (cows in 2..4) "ы" else if (cows == 1) "а" else ""}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun GameFinishedScreen(
    uiState: MultiplayerViewModel.State,
    viewModel: MultiplayerViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "🏆 Победитель!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    uiState.winner ?: "Ничья",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                if (uiState.winner == uiState.player1Name) {
                    Text(
                        "Игрок 1: ${uiState.historyP2.size} попыток",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        "Игрок 2: ${uiState.historyP1.size} попыток",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = viewModel::resetGame,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Начать новую игру", style = MaterialTheme.typography.labelLarge)
        }
    }
}