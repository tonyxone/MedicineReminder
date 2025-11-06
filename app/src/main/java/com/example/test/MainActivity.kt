package com.example.test

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.test.ui.screens.AddMedicineScreen
import com.example.test.ui.screens.IntakeHistoryScreen
import com.example.test.ui.screens.MedicineDetailScreen
import com.example.test.ui.screens.MedicineListScreen
import com.example.test.ui.theme.TestTheme
import com.example.test.ui.viewmodel.MedicineViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MedicineViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request battery optimization exemption for reliable alarms
        requestBatteryOptimizationExemption()

        // Show splash screen for 1 second
        Handler(Looper.getMainLooper()).postDelayed({
            setTheme(R.style.Theme_Test)
            enableEdgeToEdge()

            // Request notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            setContent {
                TestTheme {
                    MedicineApp(
                        viewModel = viewModel,
                        navigateToMedicineId = intent.getLongExtra("NAVIGATE_TO_MEDICINE_ID", -1L)
                    )
                }
            }
        }, 1000)
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val packageName = packageName

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to general battery optimization settings
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        startActivity(intent)
                    } catch (e: Exception) {
                        // Unable to open battery optimization settings
                    }
                }
            }
        }
    }
}

@Composable
fun MedicineApp(
    viewModel: MedicineViewModel,
    navigateToMedicineId: Long = -1L
) {
    val navController = rememberNavController()

    // Handle navigation from notification
    LaunchedEffect(navigateToMedicineId) {
        if (navigateToMedicineId > 0) {
            navController.navigate("medicine_detail/$navigateToMedicineId")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "medicine_list",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("medicine_list") {
            MedicineListScreen(
                viewModel = viewModel,
                onAddMedicine = {
                    navController.navigate("add_medicine")
                },
                onMedicineClick = { medicineId ->
                    navController.navigate("medicine_detail/$medicineId")
                },
                onViewHistory = {
                    navController.navigate("intake_history")
                }
            )
        }

        composable("add_medicine") {
            AddMedicineScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onMedicineAdded = { medicineId ->
                    navController.popBackStack()
                    navController.navigate("medicine_detail/$medicineId")
                }
            )
        }

        composable(
            route = "medicine_detail/{medicineId}",
            arguments = listOf(navArgument("medicineId") { type = NavType.LongType })
        ) { backStackEntry ->
            val medicineId = backStackEntry.arguments?.getLong("medicineId") ?: 0L
            MedicineDetailScreen(
                medicineId = medicineId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("intake_history") {
            IntakeHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}