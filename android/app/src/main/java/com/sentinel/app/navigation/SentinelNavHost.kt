package com.sentinel.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sentinel.app.core.medical.DefaultMedicalSummaryProvider
import com.sentinel.app.core.medical.MedicalSummaryProvider
import com.sentinel.app.core.mesh.MeshManager
import com.sentinel.app.core.network.ConnectivityObserver
import com.sentinel.app.feature.bot.BotScreen
import com.sentinel.app.feature.bot.BotViewModel
import com.sentinel.app.feature.firstaid.FirstAidScreen
import com.sentinel.app.feature.firstaid.FirstAidViewModel
import com.sentinel.app.feature.home.HomeScreen
import com.sentinel.app.feature.home.HomeViewModel
import com.sentinel.app.feature.map.MapScreen
import com.sentinel.app.feature.map.MapViewModel
import com.sentinel.app.feature.medical.MedicalScreen
import com.sentinel.app.feature.medical.MedicalViewModel
import com.sentinel.app.feature.medical.QrScanScreen
import com.sentinel.app.feature.onboarding.OnboardingScreen
import com.sentinel.app.feature.onboarding.OnboardingViewModel
import com.sentinel.app.feature.settings.SettingsScreen
import com.sentinel.app.feature.settings.SettingsViewModel
import com.sentinel.app.feature.sos.SosScreen
import com.sentinel.app.feature.sos.SosViewModel

@Composable
fun SentinelNavHost(
    navController: NavHostController,
    connectivityObserver: ConnectivityObserver,
    meshManager: MeshManager,
    medicalSummaryProvider: MedicalSummaryProvider = DefaultMedicalSummaryProvider(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
        modifier = modifier.fillMaxSize()
    ) {
        composable(NavRoutes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.provideFactory(context, connectivityObserver)
            )
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToMap = { navController.navigate(NavRoutes.MAP) },
                onNavigateToMedical = { navController.navigate(NavRoutes.MEDICAL) },
                onNavigateToBot = { navController.navigate(NavRoutes.BOT) },
                onNavigateToSos = { navController.navigate(NavRoutes.SOS) },
                onNavigateToFirstAid = { navController.navigate(NavRoutes.FIRST_AID) },
                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) }
            )
        }

        composable(NavRoutes.MAP) {
            val mapViewModel: MapViewModel = viewModel(
                factory = MapViewModel.provideFactory(context)
            )
            MapScreen(viewModel = mapViewModel)
        }

        composable(NavRoutes.MEDICAL) {
            val medicalViewModel: MedicalViewModel = viewModel(
                factory = MedicalViewModel.provideFactory(context)
            )
            MedicalScreen(
                viewModel = medicalViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScanner = { navController.navigate(NavRoutes.QR_SCANNER) }
            )
        }

        composable(NavRoutes.QR_SCANNER) {
            val medicalViewModel: MedicalViewModel = viewModel(
                factory = MedicalViewModel.provideFactory(context)
            )
            QrScanScreen(
                onNavigateBack = { navController.popBackStack() },
                onProfileImported = { profile ->
                    medicalViewModel.importProfile(profile)
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoutes.BOT) {
            val botViewModel: BotViewModel = viewModel(
                factory = BotViewModel.provideFactory(context, connectivityObserver)
            )
            BotScreen(
                viewModel = botViewModel,
                onNavigateToSos = { navController.navigate(NavRoutes.SOS) },
                onNavigateToFirstAid = { navController.navigate(NavRoutes.FIRST_AID) }
            )
        }

        composable(NavRoutes.SOS) {
            val sosViewModel: SosViewModel = viewModel(
                factory = SosViewModel.provideFactory(context, meshManager, medicalSummaryProvider)
            )
            SosScreen(viewModel = sosViewModel)
        }

        composable(NavRoutes.FIRST_AID) {
            val firstAidViewModel: FirstAidViewModel = viewModel(
                factory = FirstAidViewModel.provideFactory(context)
            )
            FirstAidScreen(
                viewModel = firstAidViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ONBOARDING) {
            val onboardingViewModel: OnboardingViewModel = viewModel()
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onComplete = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
    }
}
