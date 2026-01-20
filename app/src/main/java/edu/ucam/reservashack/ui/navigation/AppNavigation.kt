package edu.ucam.reservashack.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import edu.ucam.reservashack.ui.theme.UcamGold
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.ucam.reservashack.R
import edu.ucam.reservashack.ui.screens.login.LoginScreen
import edu.ucam.reservashack.ui.screens.profile.ProfileScreen
import edu.ucam.reservashack.ui.screens.mybookings.MyBookingsScreen
import edu.ucam.reservashack.ui.screens.home.HomeScreen

sealed class Screen(val route: String, val titleResId: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_search, Icons.Default.AddCircle)
    object MyBookings : Screen("my_bookings", R.string.nav_my_bookings, Icons.AutoMirrored.Filled.List)
    object Profile : Screen("profile", R.string.nav_profile, Icons.Default.AccountCircle)
}

// Rutas que no están en la BottomBar
object OtherRoutes {
    const val ADD_ACCOUNT_WEBVIEW = "add_account_webview/{mode}"
    const val RELOGIN_ACCOUNT_WEBVIEW = "relogin_account_webview?accountId={accountId}&mode=relogin_account"

    fun getAddAccountRoute(mode: String = "add_account") = "add_account_webview/$mode"
    fun getReloginRoute(accountId: String) = "relogin_account_webview?accountId=$accountId&mode=relogin_account"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomNavPagerContent(
    pagerState: androidx.compose.foundation.pager.PagerState,
    navController: androidx.navigation.NavHostController
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true
    ) { page ->
        when (page) {
            0 -> HomeScreen()
            1 -> MyBookingsScreen()
            2 -> ProfileScreen(
                navController = navController,
                onAddAccountClick = {
                    navController.navigate(OtherRoutes.getAddAccountRoute("add_account"))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainAppScaffold() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.MyBookings,
        Screen.Profile
    )

    // Estado del pager para swipe gestures
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { items.size })
    val scope = rememberCoroutineScope()

    // Determinamos si debemos mostrar la BottomBar (ocultarla en WebView)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute?.startsWith("add_account_webview") != true

    // Sincronizar el pager con la navegación del bottom bar
    LaunchedEffect(currentRoute) {
        when (currentRoute) {
            Screen.Home.route -> if (pagerState.currentPage != 0) pagerState.animateScrollToPage(0)
            Screen.MyBookings.route -> if (pagerState.currentPage != 1) pagerState.animateScrollToPage(1)
            Screen.Profile.route -> if (pagerState.currentPage != 2) pagerState.animateScrollToPage(2)
        }
    }

    // Sincronizar la navegación con el swipe del pager
    LaunchedEffect(pagerState.currentPage) {
        val targetRoute = items[pagerState.currentPage].route
        if (currentRoute != targetRoute && !pagerState.isScrollInProgress) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    modifier = Modifier.drawBehind {
                        // Línea inferior dorada para separar el top bar
                        val strokeWidth = 5.dp.toPx()
                        drawLine(
                            color = UcamGold,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = strokeWidth
                        )
                    },
                    title = {
                        AnimatedContent(
                            targetState = pagerState.currentPage,
                            transitionSpec = {
                                (fadeIn() + slideInHorizontally { width -> 
                                    if (targetState > initialState) width else -width 
                                }).togetherWith(
                                    fadeOut() + slideOutHorizontally { width -> 
                                        if (targetState > initialState) -width else width 
                                    }
                                )
                            },
                            label = "titleAnimation"
                        ) { page ->
                            val currentScreenTitle = when (page) {
                                0 -> stringResource(Screen.Home.titleResId)
                                1 -> stringResource(Screen.MyBookings.titleResId)
                                2 -> stringResource(Screen.Profile.titleResId)
                                else -> stringResource(R.string.app_title)
                            }
                            Text(
                                currentScreenTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                CustomBottomNavigationBar(
                    items = items,
                    currentPage = pagerState.currentPage,
                    currentPageOffset = pagerState.currentPageOffsetFraction,
                    onItemClick = { index ->
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Las tres pantallas del bottom nav usan el mismo composable con HorizontalPager
            composable(Screen.Home.route) {
                BottomNavPagerContent(
                    pagerState = pagerState,
                    navController = navController
                )
            }
            composable(Screen.MyBookings.route) {
                BottomNavPagerContent(
                    pagerState = pagerState,
                    navController = navController
                )
            }
            composable(Screen.Profile.route) {
                BottomNavPagerContent(
                    pagerState = pagerState,
                    navController = navController
                )
            }
            
            // Ruta del WebView
            composable(
                route = OtherRoutes.ADD_ACCOUNT_WEBVIEW,
                arguments = listOf(
                    navArgument("mode") { 
                        type = NavType.StringType
                        defaultValue = "add_account"
                    }
                )
            ) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.popBackStack()
                    }
                )
            }

            // Ruta para re-login de cuenta con sesión expirada
            composable(
                route = OtherRoutes.RELOGIN_ACCOUNT_WEBVIEW,
                arguments = listOf(
                    navArgument("accountId") {
                        type = NavType.StringType
                    },
                    navArgument("mode") {
                        type = NavType.StringType
                        defaultValue = "relogin_account"
                    }
                )
            ) {
                // Los argumentos llegarán al ViewModel a través de SavedStateHandle
                LoginScreen(
                    onLoginSuccess = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun CustomBottomNavigationBar(
    items: List<Screen>,
    currentPage: Int,
    currentPageOffset: Float,
    onItemClick: (Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val itemWidth = screenWidth / items.size

    // Calcular el offset animado de la línea indicadora
    val indicatorOffset by animateDpAsState(
        targetValue = itemWidth * (currentPage + currentPageOffset),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicatorOffset"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val isDarkTheme = isSystemInDarkTheme()
        val accentColor = if (isDarkTheme) UcamGold else MaterialTheme.colorScheme.primary

        Column(modifier = Modifier.fillMaxWidth()) {
            // Línea indicadora en la parte superior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(3.dp)
                        .offset(x = indicatorOffset)
                        .background(accentColor)
                )
            }

            // Items del NavigationBar
            NavigationBar(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp
            ) {
                items.forEachIndexed { index, screen ->
                    val selected = currentPage == index
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "iconScale"
                    )

                    NavigationBarItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = null,
                                modifier = Modifier.scale(scale)
                            )
                        },
                        label = { Text(stringResource(screen.titleResId), style = MaterialTheme.typography.labelSmall) },
                        selected = selected,
                        onClick = { onItemClick(index) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accentColor,
                            selectedTextColor = accentColor,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent  // Quitamos el panel gris
                        )
                    )
                }
            }
        }
    }
}
