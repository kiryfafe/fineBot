package com.kgeutris.bullsandcows.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Airplay
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.font.FontWeight
import com.kgeutris.bullsandcows.R

@Composable
fun MainScreen(
    onNavigateToGame: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToMultiplayer: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val mainMenuItems = listOf(
        MenuItem(
            title = "Одиночная игра",
            icon = Icons.Default.PlayCircle,
            onClick = onNavigateToGame
        ),
        MenuItem(
            title = "AI угадывает",
            icon = Icons.Default.Airplay,
            onClick = onNavigateToAi
        ),
        MenuItem(
            title = "Мультиплеер",
            icon = Icons.Default.People,
            onClick = onNavigateToMultiplayer
        ),
        MenuItem(
            title = "История",
            icon = Icons.Default.History,
            onClick = onNavigateToHistory
        ),
        MenuItem(
            title = "Настройки",
            icon = Icons.Default.Settings,
            onClick = onNavigateToSettings
        ),
        MenuItem(
            title = "Об игре",
            icon = Icons.Default.Description,
            onClick = onNavigateToAbout
        )
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(mainMenuItems) { item ->
                MenuItemCard(
                    title = item.title,
                    icon = item.icon,
                    onClick = item.onClick
                )
            }
        }
    }
}

// private val mainMenuItems = listOf(
//    MenuItem(
//        title = "Одиночная игра",
//        icon = Icons.Default.PlayCircle,
//        onClick = onNavigateToGame
//    ),
//    MenuItem(
//        title = "AI угадывает",
//        icon = Icons.Default.Airplay,
//        onClick = { /* will be provided */ }
//    ),
//    MenuItem(
//        title = "Мультиплеер",
//        onClick = { /* will be provided */ }
//    ),
//    MenuItem(
//        title = "История",
//        icon = Icons.Default.History,
//        onClick = { /* will be provided */ }
//    ),
//    MenuItem(
//        title = "Настройки",
//       icon = Icons.Default.Settings,
//        onClick = { /* will be provided */ }
//    )
//)

@Composable
private fun MenuItemCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

private data class MenuItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)