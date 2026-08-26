package com.encarox.kodo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.encarox.kodo.ui.accounts.AccountsScreen
import com.encarox.kodo.ui.inbox.InboxScreen
import com.encarox.kodo.ui.onboarding.OnboardingScreen
import com.encarox.kodo.ui.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val INBOX = "inbox"
    const val ACCOUNTS = "accounts"
    const val SETTINGS = "settings"
}

@Composable
fun KodoNavHost(
    rootViewModel: RootViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController()
) {
    val onboarded by rootViewModel.onboarded.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = if (onboarded) Routes.INBOX else Routes.ONBOARDING
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onGetStarted = {
                rootViewModel.completeOnboarding()
                navController.navigate(Routes.ACCOUNTS) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.INBOX) {
            InboxScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) }
            )
        }
        composable(Routes.ACCOUNTS) {
            AccountsScreen(onBack = {
                if (!navController.popBackStack()) {
                    navController.navigate(Routes.INBOX)
                }
            })
        }
    }
}
