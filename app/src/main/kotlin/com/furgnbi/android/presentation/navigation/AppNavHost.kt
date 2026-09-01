package com.furgnbi.android.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.furgnbi.android.presentation.config.ConfigScreen
import com.furgnbi.android.presentation.config.ConfigViewModel
import com.furgnbi.android.presentation.hmi.HmiScreen
import com.furgnbi.android.presentation.hmi.HmiViewModel
import com.furgnbi.android.presentation.parameters.ParametersScreen
import com.furgnbi.android.presentation.parameters.ParametersViewModel

object NavRoutes {
    const val HMI = "hmi"
    const val PARAMETERS = "parameters"
    const val CONFIG = "config"
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavRoutes.HMI
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavRoutes.HMI) {
            val viewModel: HmiViewModel = hiltViewModel()
            HmiScreen(
                viewModel = viewModel,
                onNavigateToParameters = { navController.navigate(NavRoutes.PARAMETERS) },
                onNavigateToConfig = { navController.navigate(NavRoutes.CONFIG) }
            )
        }

        composable(NavRoutes.PARAMETERS) {
            val viewModel: ParametersViewModel = hiltViewModel()
            ParametersScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.CONFIG) {
            val viewModel: ConfigViewModel = hiltViewModel()
            ConfigScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
