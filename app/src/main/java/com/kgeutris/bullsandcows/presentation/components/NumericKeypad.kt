package com.kgeutris.bullsandcows.presentation.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kgeutris.bullsandcows.util.vibrateError
import com.kgeutris.bullsandcows.util.vibrateShort

@Composable
fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    currentGuess: String,
    maxLength: Int,
    currentDigitsCount: Int,
    confirmButtonText: String = "Проверить",
    isError: Boolean = false,
    isConfirmEnabled: Boolean = true,
    alwaysEnableDigits: Boolean = false,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Текущий ввод + индикатор заполнения
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentGuess.ifEmpty { "0" },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            // Индикатор заполнения (опционально)
            if (maxLength > 1) {
                Text(
                    text = "$currentDigitsCount/$maxLength",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Кнопки 1-9 в компактном виде
        for (row in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                for (col in 0..2) {
                    val digit = row * 3 + col + 1
                    TextButton(
                        onClick = {
                            // Проверка для режима AI (alwaysEnableDigits = true)
                            val canPress = if (alwaysEnableDigits) {
                                !isError // В режиме AI всегда можно нажать, если нет ошибки
                            } else {
                                currentDigitsCount < maxLength && !isError
                            }

                            if (canPress) {
                                onDigit(digit.toString()[0])
                            }
                        },
                        enabled = if (alwaysEnableDigits) {
                            !isError // В режиме AI всегда активно
                        } else {
                            currentDigitsCount < maxLength && !isError
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp), // Уменьшенная высота
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (isError)
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = digit.toString(),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }

        // Нижний ряд - еще более компактный
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // Кнопка 0
            TextButton(
                onClick = {
                    // Проверка для режима AI
                    val canPress = if (alwaysEnableDigits) {
                        !isError && currentGuess.isNotEmpty()
                    } else {
                        currentDigitsCount < maxLength && currentGuess.isNotEmpty() && !isError
                    }

                    if (canPress) {
                        onDigit('0')
                    }
                },
                enabled = if (alwaysEnableDigits) {
                    !isError && currentGuess.isNotEmpty() // В режиме AI можно если есть ввод
                } else {
                    currentDigitsCount < maxLength && currentGuess.isNotEmpty() && !isError
                },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text("0", style = MaterialTheme.typography.titleLarge)
            }

            // Кнопка удаления
            IconButton(
                onClick = {
                    if (currentDigitsCount > 0) {
                        onDelete()
                        if (!isError) context.vibrateShort()
                    }
                },
                enabled = currentGuess.isNotEmpty() && !isError,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Удалить",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Кнопка подтверждения
            IconButton(
                onClick = {
                    if (isConfirmEnabled && currentDigitsCount == maxLength) {
                        onConfirm()
                    } else {
                        context.vibrateError()
                    }
                },
                enabled = isConfirmEnabled &&
                        currentDigitsCount == maxLength &&
                        !isError,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isConfirmEnabled && !isError)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (isConfirmEnabled && !isError)
                        MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (confirmButtonText.length > 4) "✓" else confirmButtonText,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1
                )
            }
        }
    }
}