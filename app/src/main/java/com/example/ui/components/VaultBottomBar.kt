package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.viewmodel.Screen

@Composable
fun VaultBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("vault_bottom_bar"),
        containerColor = VaultSurface,
        windowInsets = WindowInsets.navigationBars
    ) {
        val items = listOf(
            Triple(Screen.DASHBOARD, Icons.Filled.Home to Icons.Outlined.Home, "Home"),
            Triple(Screen.DOCUMENTS, Icons.Filled.Description to Icons.Outlined.Description, "Docs"),
            Triple(Screen.PRIVATE_VAULT_HOME, Icons.Filled.Lock to Icons.Outlined.Lock, "Private"),
            Triple(Screen.SEARCH, Icons.Filled.Search to Icons.Outlined.Search, "Search"),
            Triple(Screen.SETTINGS, Icons.Filled.Settings to Icons.Outlined.Settings, "Settings")
        )

        items.forEach { (screen, icons, label) ->
            val isSelected = currentScreen == screen ||
                (screen == Screen.DOCUMENTS && (currentScreen == Screen.ADD_DOCUMENT || currentScreen == Screen.DOCUMENT_DETAIL || currentScreen == Screen.DOCUMENT_SCANNER)) ||
                (screen == Screen.PRIVATE_VAULT_HOME && (currentScreen == Screen.PHOTO_VIEWER || currentScreen == Screen.ALBUMS || currentScreen == Screen.PRIVATE_LOCK || currentScreen == Screen.SETUP_PRIVATE_PIN))

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) icons.first else icons.second,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF003822),
                    selectedTextColor = EmeraldPrimary,
                    indicatorColor = EmeraldPrimary,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("nav_item_${label.lowercase()}")
            )
        }
    }
}
