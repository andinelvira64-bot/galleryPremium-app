package com.assound.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.assound.ui.components.GlowingIconButton
import com.assound.ui.screens.*
import com.assound.ui.theme.*
import com.assound.ui.viewmodel.MainViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    data object Equalizer : Screen(
        route = "equalizer",
        title = "Equalizer",
        selectedIcon = Icons.Filled.GraphicEq,
        unselectedIcon = Icons.Outlined.GraphicEq
    )
    
    data object Profiles : Screen(
        route = "profiles",
        title = "Profiles",
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic
    )
    
    data object Advanced : Screen(
        route = "advanced",
        title = "Advanced",
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    )
    
    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Equalizer,
    Screen.Profiles,
    Screen.Advanced
)

@Composable
fun ASsoundNavHost(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }
    
    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(animationSpec = tween(300)) + 
                slideInHorizontally(animationSpec = tween(300)) { it / 4 }
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300)) +
                slideOutHorizontally(animationSpec = tween(300)) { -it / 4 }
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300)) +
                slideInHorizontally(animationSpec = tween(300)) { -it / 4 }
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300)) +
                slideOutHorizontally(animationSpec = tween(300)) { it / 4 }
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToEqualizer = { navController.navigateTo(Screen.Equalizer.route) },
                    onNavigateToProfiles = { navController.navigateTo(Screen.Profiles.route) },
                    onNavigateToAdvanced = { navController.navigateTo(Screen.Advanced.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            
            composable(Screen.Equalizer.route) {
                EqualizerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Profiles.route) {
                ProfilesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Advanced.route) {
                AdvancedScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        // Bottom Navigation
        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ASsoundBottomNavigation(
                navController = navController,
                currentRoute = currentRoute
            )
        }
    }
}

@Composable
private fun ASsoundBottomNavigation(
    navController: NavController,
    currentRoute: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .navigationBarsPadding()
    ) {
        // Glassmorphism background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassMedium,
                            GlassLight
                        )
                    )
                )
                .then(
                    Modifier
                        .blur(0.dp) // For border effect
                )
        ) {
            // Inner content with blur behind
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = NavBackground.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp)
                    )
            )
        }
        
        // Navigation items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { screen ->
                val isSelected = currentRoute == screen.route
                
                NavItem(
                    screen = screen,
                    isSelected = isSelected,
                    onClick = {
                        navController.navigateTo(screen.route)
                    }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = if (isSelected) screen.selectedIcon else screen.unselectedIcon
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.width(72.dp)
    ) {
        GlowingIconButton(
            icon = icon,
            onClick = onClick,
            isActive = isSelected,
            activeColor = ASsoundPrimary,
            inactiveColor = NavUnselected,
            size = 44.dp,
            iconSize = 24.dp
        )
        
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = screen.title,
                style = ASsoundTypography.labelSmall,
                color = ASsoundPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun NavController.navigateTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
