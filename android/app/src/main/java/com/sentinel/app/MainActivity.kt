package com.sentinel.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sentinel.app.core.medical.DefaultMedicalSummaryProvider
import com.sentinel.app.core.mesh.MeshManager
import com.sentinel.app.core.network.NetworkConnectivityObserver
import com.sentinel.app.navigation.NavRoutes
import com.sentinel.app.navigation.SentinelBottomBar
import com.sentinel.app.navigation.SentinelNavHost
import com.sentinel.app.ui.theme.SentinelTheme

class MainActivity : AppCompatActivity() {

    private lateinit var connectivityObserver: NetworkConnectivityObserver
    private lateinit var meshManager: MeshManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectivityObserver = NetworkConnectivityObserver(applicationContext)
        meshManager = MeshManager(applicationContext, connectivityObserver)
        val medicalSummaryProvider = DefaultMedicalSummaryProvider()

        setContent {
            SentinelTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in listOf(
                    NavRoutes.HOME,
                    NavRoutes.MAP,
                    NavRoutes.FIRST_AID,
                    NavRoutes.BOT,
                    NavRoutes.SETTINGS
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            SentinelBottomBar(
                                navController = navController,
                                currentRoute = currentRoute
                            )
                        }
                    }
                ) { innerPadding ->
                    SentinelNavHost(
                        navController = navController,
                        connectivityObserver = connectivityObserver,
                        meshManager = meshManager,
                        medicalSummaryProvider = medicalSummaryProvider,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        meshManager.stopMesh()
    }
}
