package com.kaafisir.auramic.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kaafisir.auramic.domain.model.AppLanguage
import com.kaafisir.auramic.domain.model.UserSettings
import com.kaafisir.auramic.ui.AppViewModel
import com.kaafisir.auramic.ui.home.HomeScreen
import com.kaafisir.auramic.ui.lab.LabScreen
import com.kaafisir.auramic.ui.language.LanguageScreen
import com.kaafisir.auramic.ui.permissions.PermissionsScreen
import com.kaafisir.auramic.ui.settings.SettingsScreen
import com.kaafisir.auramic.ui.tour.TourScreen

object Routes {
    const val LANGUAGE = "language"
    const val TOUR = "tour"
    const val TOUR_REPLAY = "tour_replay"
    const val PERMISSIONS = "permissions"          // first run: with showcase, continues to Home
    const val PERMISSIONS_FIX = "permissions_fix"  // from Home: no showcase, returns
    const val HOME = "home"
    const val LAB = "lab"
    const val SETTINGS = "settings"
}

private const val DUR = 320

@Composable
fun AppNavHost(settings: UserSettings, vm: AppViewModel, nav: NavHostController = rememberNavController()) {
    val start = remember {
        when {
            settings.language == null -> Routes.LANGUAGE
            !settings.tourCompleted -> Routes.TOUR
            else -> Routes.HOME
        }
    }
    val deviceLang = LocalConfiguration.current.locales[0].language

    // Once a language is chosen the language screen must never be shown: this also repairs a navigation state that was
    // restored from before the choice (e.g. after a process or configuration change).
    val backEntry by nav.currentBackStackEntryAsState()
    LaunchedEffect(settings.language, settings.tourCompleted, backEntry?.destination?.route) {
        if (backEntry?.destination?.route == Routes.LANGUAGE && settings.language != null) {
            nav.navigate(if (settings.tourCompleted) Routes.HOME else Routes.TOUR) { popUpTo(Routes.LANGUAGE) { inclusive = true } }
        }
    }

    NavHost(
        navController = nav,
        startDestination = start,
        enterTransition = { slideInHorizontally(tween(DUR)) { it / 6 } + fadeIn(tween(DUR)) },
        exitTransition = { fadeOut(tween(DUR / 2)) },
        popEnterTransition = { fadeIn(tween(DUR)) },
        popExitTransition = { slideOutHorizontally(tween(DUR)) { it / 6 } + fadeOut(tween(DUR)) },
    ) {
        composable(Routes.LANGUAGE) {
            LanguageScreen(initial = AppLanguage.suggestedFor(deviceLang)) { lang ->
                // Persist; MainActivity re-provides the language instantly and the effect below moves on.
                vm.chooseLanguage(lang) {}
            }
        }
        composable(Routes.TOUR) {
            TourScreen(onFinished = {
                vm.completeTour()
                nav.navigate(Routes.PERMISSIONS) { popUpTo(Routes.TOUR) { inclusive = true } }
            })
        }
        composable(Routes.TOUR_REPLAY) { TourScreen(onFinished = { nav.popBackStack() }) }
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(showcase = true, onDone = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.PERMISSIONS) { inclusive = true } }
            })
        }
        composable(Routes.PERMISSIONS_FIX) { PermissionsScreen(showcase = false, onDone = { nav.popBackStack() }) }
        composable(Routes.HOME) {
            HomeScreen(
                onSetup = { nav.navigate(Routes.PERMISSIONS_FIX) },
                onLab = { nav.navigate(Routes.LAB) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onReplayTour = { nav.navigate(Routes.TOUR_REPLAY) },
            )
        }
        composable(Routes.LAB) { LabScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
