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
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import kotlin.math.roundToInt


import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import android.content.Intent

import com.james.anvil.ui.theme.ANVILTheme
import com.james.anvil.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
                
                // Handle back button when settings is open
                BackHandler(enabled = showSettings) {
                    showSettings = false
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main App Content - Stays alive in the background
                        MainScreen(
                            viewModel = viewModel, 
                            onNavigateToSettings = { showSettings = true }
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
                                onBack = { showSettings = false }
                            )
                        }
                    }
                }

                // Consolidated permission check manager
                PermissionCheckManager()
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
fun MainScreen(viewModel: TaskViewModel, onNavigateToSettings: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navItems = NavItem.bottomNavItems
    val pagerState = rememberPagerState(pageCount = { 4 }) // Home, Tasks, Budget, Blocklist
    val scope = rememberCoroutineScope()


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
    
    Box(
        modifier = Modifier
            .padding(horizontal = DesignTokens.SpacingLg, vertical = DesignTokens.SpacingMd)
            .navigationBarsPadding()
    ) {
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = DesignTokens.ElevationHigh,
                    shape = RoundedCornerShape(DesignTokens.RadiusRound),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = DesignTokens.AlphaHighlight)
                )
                .clip(RoundedCornerShape(DesignTokens.RadiusRound))
                .onGloballyPositioned { coordinates ->
                    barWidth = with(density) { coordinates.size.width.toDp() }
                    itemWidth = barWidth / navItems.size
                },
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = DesignTokens.ElevationNone
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DesignTokens.BottomNavHeight)
            ) {
                // Sliding indicator
                val indicatorOffset = with(density) {
                    val currentPage = pagerState.currentPage
                    val offset = pagerState.currentPageOffsetFraction
                    ((currentPage + offset) * itemWidth.toPx()).roundToInt()
                }
                
                // Sleek Pill Highlight
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffset, 0) }
                        .width(itemWidth)
                        .height(DesignTokens.BottomNavHeight)
                        .padding(horizontal = 10.dp, vertical = 14.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = DesignTokens.AlphaSelected),
                            shape = RoundedCornerShape(DesignTokens.RadiusXLarge)
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
                                    indication = null // Remove default ripple to prevent double highlight
                                ) { onItemClick(index) },
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            val tint by androidx.compose.animation.animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DesignTokens.AlphaMuted),
                                animationSpec = tween(DesignTokens.AnimDurationSlow),
                                label = "navTint"
                            )
                            
                            val scale by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isSelected) 1.1f else 1f,
                                animationSpec = tween(DesignTokens.AnimDurationSlow),
                                label = "navScale"
                            )

                            Column(
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.scale(scale)
                            ) {

                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    tint = tint,
                                    modifier = Modifier.size(DesignTokens.IconSizeMedium)
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        letterSpacing = 0.5.sp
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
