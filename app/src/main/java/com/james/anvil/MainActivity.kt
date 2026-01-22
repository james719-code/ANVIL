package com.james.anvil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import kotlin.math.roundToInt


import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.james.anvil.ui.TaskViewModel
import com.james.anvil.ui.navigation.NavItem
import com.james.anvil.ui.DashboardScreen
import com.james.anvil.ui.TasksScreen
import com.james.anvil.ui.BudgetScreen
import com.james.anvil.ui.BlocklistScreen
import com.james.anvil.ui.SettingsScreen
import com.james.anvil.ui.SplashScreen
import com.james.anvil.ui.AboutScreen
import android.content.Intent

import com.james.anvil.ui.theme.ANVILTheme
import com.james.anvil.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import com.james.anvil.ui.components.PermissionCheckManager
import com.james.anvil.util.ShortcutProvider
import com.james.anvil.util.WorkerScheduler

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Use centralized utilities
        WorkerScheduler.scheduleAllWorkers(this)
        ShortcutProvider.setupShortcuts(this)
        handleIntent(intent)
        
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            ANVILTheme(darkTheme = isDarkTheme) {
                var showSettings by remember { mutableStateOf(false) }
                var showAbout by remember { mutableStateOf(false) }
                var showSplash by remember { mutableStateOf(true) }
                var loadingProgress by remember { mutableFloatStateOf(0f) }
                var isLoading by remember { mutableStateOf(true) }
                val scope = rememberCoroutineScope()
                
                
                // Handle back button when settings or about is open
                BackHandler(enabled = showAbout) {
                    showAbout = false
                }
                BackHandler(enabled = showSettings && !showAbout) {
                    showSettings = false
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main App Content - Always composed to enable preloading
                        // It stays underneath the Splash screen while loading
                        MainScreen(
                            viewModel = viewModel, 
                            onNavigateToSettings = { showSettings = true },
                            onPagesPreloaded = {
                                if (isLoading) {
                                    scope.launch {
                                        // Update progress bar as pages load
                                        delay(300)
                                        loadingProgress = 0.4f
                                        delay(400)
                                        loadingProgress = 0.7f
                                        delay(400)
                                        loadingProgress = 1.0f
                                        delay(200)
                                        isLoading = false
                                    }
                                }
                            }
                        )
                        
                        // Settings Overlay - Slides over the main content
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showSettings,
                            enter = slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(DesignTokens.AnimDurationSlow)
                            ),
                            exit = slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(DesignTokens.AnimDurationSlow)
                            )
                        ) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { showSettings = false },
                                onNavigateToAbout = { showAbout = true }
                            )
                        }
                        
                        // About Overlay - Slides over settings
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showAbout,
                            enter = slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(DesignTokens.AnimDurationSlow)
                            ),
                            exit = slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(DesignTokens.AnimDurationSlow)
                            )
                        ) {
                            AboutScreen(
                                onBack = { showAbout = false }
                            )
                        }
                        
                        // Splash Screen Overlay
                        if (showSplash) {
                            SplashScreen(
                                progress = loadingProgress,
                                isLoading = isLoading,
                                onSplashComplete = {
                                    showSplash = false
                                }
                            )
                        }
                    }
                }

                // Consolidated permission check manager (show after splash)
                if (!showSplash) {
                    PermissionCheckManager()
                }
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // Handle other intents if any
    }
}

@Composable
fun MainScreen(
    viewModel: TaskViewModel, 
    onNavigateToSettings: () -> Unit,
    onPagesPreloaded: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navItems = NavItem.bottomNavItems
    val pagerState = rememberPagerState(pageCount = { 4 }) // Home, Tasks, Budget, Blocklist
    val scope = rememberCoroutineScope()
    
    // Notify parent when this composable is first composed (pages are being preloaded)
    LaunchedEffect(Unit) {
        // Small delay to ensure Compose has started laying out pages
        delay(50)
        onPagesPreloaded()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                SlidingNavigationBar(
                    pagerState = pagerState,
                    navItems = navItems,
                    onItemClick = { index ->
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                beyondViewportPageCount = 3
            ) { page ->
                when (page) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToPage = { targetPage ->
                            // Map existing indices if needed, or handle special cases
                            if (targetPage == 5) {
                                onNavigateToSettings()
                            } else if (targetPage == 4) {
                                // Bonus tasks is now a tab in Tasks (index 1)
                                scope.launch { 
                                    pagerState.animateScrollToPage(1)
                                }
                            } else {
                                scope.launch { pagerState.animateScrollToPage(targetPage) }
                            }
                        }
                    )

                    1 -> TasksScreen(viewModel = viewModel, snackbarHostState = snackbarHostState)
                    2 -> BudgetScreen(viewModel = viewModel)
                    3 -> BlocklistScreen(viewModel = viewModel)
                }
            }
        }

        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        )
    }
}

@Composable
fun SlidingNavigationBar(
    pagerState: PagerState,
    navItems: List<NavItem<*>>,
    onItemClick: (Int) -> Unit
) {
    val density = LocalDensity.current
    var itemWidth by remember { mutableStateOf(0.dp) }
    var barWidth by remember { mutableStateOf(0.dp) }
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingMd)
            .navigationBarsPadding()
    ) {
        // Main navigation surface
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(DesignTokens.RadiusLarge))
                .onGloballyPositioned { coordinates ->
                    barWidth = with(density) { coordinates.size.width.toDp() }
                    itemWidth = barWidth / navItems.size
                },
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = DesignTokens.ElevationMedium,
            shadowElevation = DesignTokens.ElevationMedium
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                // Direct offset calculation for smooth 1:1 gesture tracking
                val indicatorOffset = with(density) {
                    val currentPage = pagerState.currentPage
                    val offset = pagerState.currentPageOffsetFraction
                    ((currentPage + offset) * itemWidth.toPx()).roundToInt()
                }
                
                // Full-height pill background for selected item
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffset, 0) }
                        .width(itemWidth)
                        .fillMaxHeight()
                        .background(
                            color = primaryColor,
                            shape = RoundedCornerShape(DesignTokens.RadiusMedium)
                        )
                )
                
                // Navigation items
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    navItems.forEachIndexed { index, item ->
                        val isSelected = pagerState.currentPage == index
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) { onItemClick(index) },
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            // Animate icon/text color
                            val tint by androidx.compose.animation.animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                animationSpec = tween(DesignTokens.AnimDurationMedium),
                                label = "navTint"
                            )

                            Column(
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    tint = tint,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingXxs))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = tint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
