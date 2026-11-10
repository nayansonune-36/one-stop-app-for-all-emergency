package com.sentinel.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.sentinel.app.R

object NavRoutes {
    const val HOME = "home"
    const val MAP = "map"
    const val FIRST_AID = "firstaid"
    const val BOT = "bot"
    const val SETTINGS = "settings"
    const val MEDICAL = "medical"
    const val SOS = "sos"
    const val ONBOARDING = "onboarding"
    const val QR_SCANNER = "qr_scanner"
    const val OFFLINE_MAPS = "offline_maps"
}

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Home : BottomNavItem(NavRoutes.HOME, R.string.nav_home, Icons.Default.Home)
    object Map : BottomNavItem(NavRoutes.MAP, R.string.nav_map, Icons.Default.Map)
    object FirstAid : BottomNavItem(NavRoutes.FIRST_AID, R.string.nav_firstaid, Icons.Default.MedicalServices)
    object Bot : BottomNavItem(NavRoutes.BOT, R.string.nav_bot, Icons.AutoMirrored.Filled.Chat)
    object Settings : BottomNavItem(NavRoutes.SETTINGS, R.string.nav_settings, Icons.Default.Settings)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Map,
    BottomNavItem.FirstAid,
    BottomNavItem.Bot,
    BottomNavItem.Settings
)
