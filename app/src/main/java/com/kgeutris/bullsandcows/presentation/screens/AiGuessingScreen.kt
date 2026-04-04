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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kgeutris.bullsandcows.presentation.components.BullCowInputSelector
import com.kgeutris.bullsandcows.presentation.components.NumericKeypad
import com.kgeutris.bullsandcows.presentation.viewmodel.AiGameState
import com.kgeutris.bullsandcows.presentation.viewmodel.AiGuessingPhase
import com.kgeutris.bullsandcows.presentation.viewmodel.AiGuessingViewModel
import com.kgeutris.bullsandcows.presentation.viewmodel.InputType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGuessingScreen(
    viewModel: AiGuessingViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()

    // Запуск новой игры при входе
    LaunchedEffect(Unit) {
        if (uiState.currentGuess == null) {
            viewModel.startNewGame(4)
        }
    }

    // Автоскрытие ошибки
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            delay(4000)
            viewModel.clearError()
        }
    }

    // Автоскролл истории
    LaunchedEffect(uiState.history.size) {
        if (uiState.history.isNotEmpty()) {
            delay(100)
            lazyListState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState.phase) {
                            AiGuessingPhase.SET_SECRET -> "Загадайте число"
                            AiGuessingPhase.AI_GUESSING -> "AI угадывает • ${uiState.numberLength} цифры"
                            AiGuessingPhase.FINISHED -> "Игра завершена"
                        }
                    )
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(4.dp) // Уменьшил отступ
        ) {
            // Верхняя часть с контентом
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 4.dp), // Уменьшил вертикальный отступ
                verticalArrangement = Arrangement.spacedBy(8.dp) // Уменьшил отступ между элементами
            ) {
                // Ошибка
                uiState.error?.let { errorMsg ->
                    Snackbar(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(errorMsg, style = MaterialTheme.typography.bodySmall)
                    }
                    LaunchedEffect(errorMsg) {
                        delay(2000)
                        viewModel.clearError()
                    }
                }

                // Текущая попытка AI
                when (uiState.phase) {
                    AiGuessingPhase.SET_SECRET -> {
                        // ЭКРАН ВВОДА ЧИСЛА
                        SecretInputScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    AiGuessingPhase.AI_GUESSING -> {
                        // Уменьшенный блок с текущим ходом AI
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp), // Уменьшил padding
                                verticalArrangement = Arrangement.spacedBy(8.dp) // Уменьшил отступ
                            ) {
                                // AI ЧИСЛО - УПРОЩЕННАЯ ВЕРСИЯ
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            "🤖 AI:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = uiState.currentGuess ?: "—",
                                            style = MaterialTheme.typography.titleLarge, // Уменьшил размер
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }

                                    // ВАШЕ ЧИСЛО - УПРОЩЕННАЯ ВЕРСИЯ
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            "👤 Вы:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = uiState.secret,
                                            style = MaterialTheme.typography.titleLarge, // Уменьшил размер
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                }

                                Text(
                                    "Сравните и укажите быков/коров:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Выбор активного поля
                        BullCowInputSelector(
                            bullsInput = uiState.bullsInput,
                            cowsInput = uiState.cowsInput,
                            activeInput = uiState.activeInput,
                            numberLength = uiState.numberLength,
                            onBullsClick = { viewModel.setActiveInput(InputType.BULLS) },
                            onCowsClick = { viewModel.setActiveInput(InputType.COWS) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // История с заголовком
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
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
                                    "Попыток: ${uiState.history.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (uiState.history.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    state = lazyListState,
                                    verticalArrangement = Arrangement.spacedBy(2.dp), // Уменьшил отступ
                                    reverseLayout = true
                                ) {
                                    items(uiState.history.reversed()) { (guess, feedback) ->
                                        val (bulls, cows) = feedback
                                        HistoryItem(
                                            guess = guess,
                                            bulls = bulls,
                                            cows = cows,
                                            numberLength = uiState.numberLength
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
                                        "Здесь будет история попыток AI",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    AiGuessingPhase.FINISHED -> {
                        // ЭКРАН ЗАВЕРШЕНИЯ ИГРЫ
                        GameFinishedScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onBack = onBack
                        )
                    }
                }
            }

            // Клавиатура - ВАЖНО: alwaysEnableDigits = true для режима AI!
            when (uiState.phase) {
                AiGuessingPhase.SET_SECRET -> {
                    // Клавиатура для ввода числа
                    NumericKeypad(
                        onDigit = { viewModel.addDigitToSecret(it) },
                        onDelete = { viewModel.deleteSecretDigit() },
                        onConfirm = { viewModel.confirmSecret() },
                        currentGuess = uiState.secret,
                        maxLength = uiState.numberLength,
                        alwaysEnableDigits = false,
                        currentDigitsCount = uiState.secret.length,
                        confirmButtonText = "✓",
                        isError = uiState.error != null,
                        isConfirmEnabled = uiState.secret.length == uiState.numberLength,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AiGuessingPhase.AI_GUESSING -> {
                    // Существующая клавиатура для быков/коров
                    NumericKeypad(
                        onDigit = { viewModel.addDigitToInput(it) },
                        onDelete = { viewModel.deleteLastDigit() },
                        onConfirm = {
                            if (uiState.canSubmitFeedback) {
                                viewModel.submitFeedback()
                            }
                        },
                        currentGuess = when (uiState.activeInput) {
                            InputType.BULLS -> uiState.bullsInput
                            InputType.COWS -> uiState.cowsInput
                        },
                        maxLength = 1,
                        currentDigitsCount = when (uiState.activeInput) {
                            InputType.BULLS -> uiState.bullsInput.length
                            InputType.COWS -> uiState.cowsInput.length
                        },
                        confirmButtonText = if (uiState.canSubmitFeedback) "✓" else "×",
                        isError = uiState.error != null,
                        isConfirmEnabled = uiState.canSubmitFeedback,
                        alwaysEnableDigits = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AiGuessingPhase.FINISHED -> {
                    // Без клавиатуры
                }
            }
        }
    }
}

// Новая компактная карточка для истории
@Composable
private fun HistoryItem(
    guess: String,
    bulls: Int,
    cows: Int,
    numberLength: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp), // Очень маленький отступ
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), // Уменьшил padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Число AI
            Text(
                guess,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

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
private fun SecretInputScreen(
    uiState: AiGameState,
    viewModel: AiGuessingViewModel
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    "Загадайте число",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Text(
                    "AI будет пытаться его угадать",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Отображение вводимого числа
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        uiState.secret.ifEmpty { "Введите ${uiState.numberLength} цифр" },
                        style = MaterialTheme.typography.displayMedium,
                        color = if (uiState.secret.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }

                // Правила
                Text(
                    "• ${uiState.numberLength} неповторяющихся цифр\n• Например: 1234, 5678",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GameFinishedScreen(
    uiState: AiGameState,
    viewModel: AiGuessingViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Иконка результата
                Icon(
                    if (uiState.isVictory) Icons.Default.EmojiEvents else Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (uiState.isVictory)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )

                // Заголовок
                Text(
                    if (uiState.isVictory) "🎉 AI угадал!" else "⏰ Время вышло",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )

                // Статистика
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (uiState.isVictory)
                            "За ${uiState.history.size} попыток"
                        else
                            "За ${uiState.history.size} попыток не угадал",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "Ваше число: ${uiState.secret}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Кнопки действий
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Новая игра с тем же числом
            Button(
                onClick = { viewModel.startNewGameWithSameSecret() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("🔄 Новая игра с этим числом")
            }

            // 2. Новая игра с новым числом
            Button(
                onClick = { viewModel.resetGame() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("🎮 Новая игра с другим числом")
            }

            // 3. Выйти в меню
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🏠 Выйти в меню")
            }
        }
    }
}