package com.itemfinder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.itemfinder.app.data.ItemEntity
import com.itemfinder.app.ui.screens.AddItemScreen
import com.itemfinder.app.ui.screens.HomeScreen
import com.itemfinder.app.ui.screens.ItemDetailScreen
import com.itemfinder.app.ui.theme.ItemFinderTheme
import com.itemfinder.app.viewmodel.ItemViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ItemFinderTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val navController = rememberNavController()
    val viewModel: ItemViewModel = viewModel()
    val items by viewModel.items.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val navigateToHome by viewModel.navigateToHome.collectAsState()

    // Store current item for detail screen
    var currentItem by remember { mutableStateOf<ItemEntity?>(null) }

    // Listen for navigation events from ViewModel (survives activity recreation)
    LaunchedEffect(navigateToHome) {
        if (navigateToHome) {
            navController.navigate("home") {
                popUpTo("home") {
                    inclusive = true
                }
                launchSingleTop = true
            }
            viewModel.onNavigatedToHome()
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                items = items,
                searchQuery = searchQuery,
                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                onItemClick = { itemId ->
                    currentItem = items.find { it.id == itemId }
                    navController.navigate("detail")
                },
                onAddClick = { navController.navigate("add") },
                onDeleteItem = { viewModel.deleteItem(it) }
            )
        }

        composable("add") {
            AddItemScreen(
                onSave = { name, location, itemPhotoPath, locationPhotoPaths, onSuccess, onError ->
                    viewModel.saveItem(
                        name = name,
                        location = location,
                        itemPhotoPath = itemPhotoPath,
                        locationPhotoPaths = locationPhotoPaths,
                        onSuccess = onSuccess,
                        onError = onError
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("detail") {
            ItemDetailScreen(
                item = currentItem,
                onBack = { navController.popBackStack() },
                onDelete = {
                    currentItem?.let { item ->
                        viewModel.deleteItem(item)
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}
