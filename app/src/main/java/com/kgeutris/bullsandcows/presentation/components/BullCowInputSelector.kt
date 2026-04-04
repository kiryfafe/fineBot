package com.kgeutris.bullsandcows.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kgeutris.bullsandcows.presentation.viewmodel.InputType

@Composable
fun BullCowInputSelector(
    bullsInput: String,
    cowsInput: String,
    activeInput: InputType,
    numberLength: Int,
    onBullsClick: () -> Unit,
    onCowsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Ввод оценки:", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Быки
            BullCowInputCard(
                title = "Быки",
                value = bullsInput,
                emoji = "🐂",
                isActive = activeInput == InputType.BULLS,
                onClick = onBullsClick,
                maxValue = numberLength,
                modifier = Modifier.weight(1f)
            )

            // Коровы
            BullCowInputCard(
                title = "Коровы",
                value = cowsInput,
                emoji = "🐄",
                isActive = activeInput == InputType.COWS,
                onClick = onCowsClick,
                maxValue = numberLength,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BullCowInputCard(
    title: String,
    value: String,
    emoji: String,
    isActive: Boolean,
    onClick: () -> Unit,
    maxValue: Int,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isActive)
                        Icons.Default.CheckCircle
                    else Icons.Default.Circle,
                    contentDescription = null,
                    tint = if (isActive)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Text(
                text = value.ifEmpty { "" },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Text("(0-$maxValue)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}