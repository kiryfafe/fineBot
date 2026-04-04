package com.kgeutris.bullsandcows.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kgeutris.bullsandcows.presentation.components.NumericKeypad
import com.kgeutris.bullsandcows.presentation.viewmodel.GameViewModel
import com.kgeutris.bullsandcows.util.vibrateError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit

@ExperimentalMaterial3Api
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var previousHistorySize by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        viewModel.initSound(context)
        onDispose {
            viewModel.releaseSound()
        }
    }

    var showConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isVictory) {
        if (uiState.isVictory) {
            showConfetti = true
            delay(3000)
            showConfetti = false
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (uiState.secret.isEmpty()) {
            viewModel.startNewGame()
        }
    }

    // Автоматический скролл к новым элементам (сверху)
    LaunchedEffect(uiState.history.size) {
        if (uiState.history.isNotEmpty() && uiState.history.size > previousHistorySize) {
            delay(50)
            lazyListState.animateScrollToItem(0)
        }
        previousHistorySize = uiState.history.size
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Одиночная игра • ${uiState.secret.length} цифры") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Верхняя часть с историей
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.error?.let { errorMessage ->
                        Snackbar(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) {
                            Text(errorMessage, style = MaterialTheme.typography.bodySmall)
                        }
                        LaunchedEffect(errorMessage) {
                            delay(2000)
                            viewModel.clearError()
                        }
                    }

                    // Заголовок и статистика истории
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
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            reverseLayout = true
                        ) {
                            items(uiState.history.reversed()) { (guess, feedback) ->
                                val (bulls, cows) = feedback
                                val attemptNumber = uiState.history.size - uiState.history.reversed().indexOf(guess to feedback)
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
                                "Здесь будет история ваших попыток",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Клавиатура
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NumericKeypad(
                        onDigit = { digit ->
                            if (uiState.currentGuess.length < uiState.secret.length) {
                                val newGuess = uiState.currentGuess + digit

                                // Проверка на первое число 0 (если длина > 1)
                                if (newGuess.length == 1 && digit == '0' && uiState.secret.length > 1) {
                                    context.vibrateError()
                                    return@NumericKeypad
                                }

                                // Проверка на повторяющиеся цифры
                                if (newGuess.toSet().size != newGuess.length) {
                                    context.vibrateError()
                                    return@NumericKeypad
                                }

                                viewModel.updateCurrentGuess(newGuess)
                            }
                        },
                        onDelete = {
                            viewModel.updateCurrentGuess(uiState.currentGuess.dropLast(1))
                        },
                        onConfirm = {
                            if (uiState.currentGuess.length == uiState.secret.length) {
                                viewModel.makeGuess(uiState.currentGuess)
                            } else {
                                context.vibrateError()
                            }
                        },
                        currentGuess = uiState.currentGuess,
                        maxLength = uiState.secret.length,
                        currentDigitsCount = uiState.currentGuess.length,
                        isError = uiState.error != null,
                        confirmButtonText = "Проверить",
                        alwaysEnableDigits = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                LaunchedEffect(uiState.error) {
                    if (uiState.error != null) {
                        delay(500)
                    }
                }
            }
        }

        // Кнопка для скролла вниз (если пользователь прокрутил вверх)
        if (uiState.history.isNotEmpty() && lazyListState.firstVisibleItemIndex > 2) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.ArrowDownward, "К новым попыткам")
            }
        }

        // 🎊 Конфетти
        if (showConfetti) {
            val party = Party(
                speed = 0f,
                maxSpeed = 30F,
                damping = 0.9f,
                angle = 45,
                spread = 270,
                position = Position.Relative(0.5, 0.1),
                colors = listOf(
                    0xFFFF4081.toInt(),
                    0xFF4CAF50.toInt(),
                    0xFFFFEB3B.toInt(),
                    0xFF2196F3.toInt(),
                    0xFF9C27B0.toInt()
                ),
                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                shapes = listOf(Shape.Square, Shape.Circle),
                size = listOf(Size(8), Size(12), Size(16))
            )
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(party)
            )
        }
    }
}

// Компонент для отображения истории попыток (как в AI-версии)
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