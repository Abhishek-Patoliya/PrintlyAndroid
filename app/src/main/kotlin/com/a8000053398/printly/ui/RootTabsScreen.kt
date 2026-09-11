package com.a8000053398.printly.ui

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.a8000053398.printly.model.TemplateCategory
import com.a8000053398.printly.ui.editor.EditorScreen
import com.a8000053398.printly.ui.export.ExportHistoryScreen
import com.a8000053398.printly.ui.home.HomeScreen
import com.a8000053398.printly.ui.poster.PosterTilingScreen
import com.a8000053398.printly.ui.settings.SettingsScreen
import com.a8000053398.printly.ui.templates.TemplatesScreen
import com.a8000053398.printly.viewmodel.HomeViewModel
import com.a8000053398.printly.viewmodel.ProjectEditorViewModel

private object Routes {
    const val HOME = "home"
    const val TEMPLATES = "templates"
    const val SETTINGS = "settings"
    const val EDITOR_NEW = "editor/new"
    const val EDITOR_EXISTING = "editor/{projectId}"
    const val EXPORT_HISTORY = "export_history"
    const val POSTER = "poster"
}

private data class TabItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, "Home", Icons.Filled.Home),
    TabItem(Routes.TEMPLATES, "Templates", Icons.Filled.GridView),
    TabItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

/** App root: a bottom navigation bar with Home/Templates/Settings, each
 * pushing into a shared Editor destination — the Android equivalent of the
 * iOS app's per-tab `NavigationStack`s (a single shared `NavHost` is the
 * idiomatic Android pattern here; reachability and back behavior are the same). */
@Composable
fun RootTabsScreen(deepLinkIntent: Intent?) {
    val navController = rememberNavController()
    val homeViewModel = androidx.lifecycle.viewmodel.compose.viewModel<HomeViewModel>()

    LaunchedEffect(deepLinkIntent) {
        val data = deepLinkIntent?.data ?: return@LaunchedEffect
        if (data.scheme == "printly" && data.host == "new-project") {
            val type = data.getQueryParameter("type")
            val project = when (type) {
                "passport" -> homeViewModel.newProject(homeViewModel.templates.first { it.category == TemplateCategory.ID_AND_DOCUMENTS })
                "photo_print" -> homeViewModel.newProject(homeViewModel.templates.first { it.category == TemplateCategory.PHOTO_PRINTS && it.name.contains("4 × 6") })
                "collage" -> homeViewModel.newProject(homeViewModel.templates.first { it.category == TemplateCategory.COLLAGES && it.name.startsWith("4-") })
                "label" -> homeViewModel.newLabelProject()
                else -> null
            }
            if (project != null) {
                EditorBridge.stage(project)
                navController.navigate(Routes.EDITOR_NEW)
            }
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == Routes.HOME || currentRoute == Routes.TEMPLATES || currentRoute == Routes.SETTINGS

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val contentPadding = if (showBottomBar) innerPadding else androidx.compose.foundation.layout.PaddingValues(0.dp)
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(contentPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onOpenProject = { project ->
                        EditorBridge.stage(project)
                        navController.navigate(Routes.EDITOR_NEW)
                    },
                    onNewFromQuickAction = { project, initialAction ->
                        EditorBridge.stage(project, initialAction)
                        navController.navigate(Routes.EDITOR_NEW)
                    },
                    onOpenTemplatesCategory = { category ->
                        navController.navigate("${Routes.TEMPLATES}?category=${category?.name ?: ""}")
                    },
                    onOpenPoster = { navController.navigate(Routes.POSTER) }
                )
            }
            composable("${Routes.TEMPLATES}?category={category}") { backStackEntryLocal ->
                val categoryName = backStackEntryLocal.arguments?.getString("category")
                val category = categoryName?.takeIf { it.isNotEmpty() }?.let { runCatching { TemplateCategory.valueOf(it) }.getOrNull() }
                TemplatesScreen(
                    homeViewModel = homeViewModel,
                    initialCategory = category,
                    onNewProject = { project ->
                        EditorBridge.stage(project)
                        navController.navigate(Routes.EDITOR_NEW)
                    }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onOpenExportHistory = { navController.navigate(Routes.EXPORT_HISTORY) })
            }
            composable(Routes.EXPORT_HISTORY) {
                ExportHistoryScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.POSTER) {
                PosterTilingScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.EDITOR_NEW) {
                val staged = EditorBridge.consume()
                val editorViewModel = androidx.lifecycle.viewmodel.compose.viewModel<ProjectEditorViewModel>(
                    factory = editorViewModelFactory(staged?.first)
                )
                EditorScreen(
                    viewModel = editorViewModel,
                    initialAction = staged?.second,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun editorViewModelFactory(project: com.a8000053398.printly.model.PrintProject?) =
    object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return ProjectEditorViewModel(project ?: com.a8000053398.printly.model.PrintProject()) as T
        }
    }
