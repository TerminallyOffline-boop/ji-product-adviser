package com.jitelecom.productadviser

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.jitelecom.productadviser.ui.*
import com.jitelecom.productadviser.ui.screens.*
import com.jitelecom.productadviser.ui.theme.JITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val settings by appViewModel.settings.collectAsState()
            JITheme(settings.darkMode) { AdviserApp() }
        }
    }
}

private data class Destination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val primaryDestinations = listOf(
    Destination("home", "Home", Icons.Default.Home), Destination("products", "Products", Icons.Default.Search),
    Destination("compatibility", "Can It Run", Icons.Default.CheckCircle), Destination("recommend", "Recommend", Icons.Default.Star),
    Destination("more", "More", Icons.Default.MoreHoriz)
)

@Composable
fun AdviserApp(appViewModel: AppViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val baseRoute = route.substringBefore('?')
    val online by appViewModel.online.collectAsState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tablet = maxWidth >= 600.dp
        val navigate: (String) -> Unit = { target ->
            if (baseRoute != target) {
                nav.navigate(target) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
        if (tablet) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                    Spacer(Modifier.height(16.dp))
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(Icons.Default.Storefront, "JI Telecom", Modifier.padding(12.dp), tint=MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("JI", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    primaryDestinations.forEach { destination ->
                        NavigationRailItem(selected=destinationSelected(baseRoute, destination.route), onClick={navigate(destination.route)}, icon={Icon(destination.icon, destination.label)}, label={Text(destination.label)})
                    }
                }
                AppNavHost(nav, online, Modifier.weight(1f))
            }
        } else {
            Scaffold(bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    primaryDestinations.forEach { destination ->
                        NavigationBarItem(selected=destinationSelected(baseRoute, destination.route), onClick={navigate(destination.route)}, icon={Icon(destination.icon, destination.label)}, label={Text(destination.label)})
                    }
                }
            }) { padding -> AppNavHost(nav, online, Modifier.padding(padding)) }
        }
    }
}

@Composable
private fun AppNavHost(nav: androidx.navigation.NavHostController, online: Boolean, modifier: Modifier = Modifier) {
    NavHost(nav, startDestination="home", modifier=modifier) {
        composable("home") {
            HomeScreen(
                online = online,
                onNavigate = { target -> nav.navigate(target) { launchSingleTop = true } },
                onSearch = { query -> nav.navigate("products?query=${Uri.encode(query)}") },
                onProduct = { id -> nav.navigate("product/$id") }
            )
        }
        composable(
            route = "products?query={query}",
            arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })
        ) { ProductsScreen(onProduct={nav.navigate("product/$it")}) }
        composable("product/{id}") { ProductDetailScreen(onBack={nav.popBackStack()}, onCompatibility={ id -> nav.navigate("compatibility?productId=$id") }) }
        composable(
            route = "compatibility?productId={productId}",
            arguments = listOf(navArgument("productId") { type = NavType.LongType; defaultValue = -1L })
        ) { entry -> CompatibilityScreen(initialProductId=entry.arguments?.getLong("productId")?.takeIf { it > 0 }) }
        composable("recommend") { RecommendationScreen(onProduct={nav.navigate("product/$it")}) }
        composable("more") { MoreScreen(onNavigate=nav::navigate) }
        composable("compare") { CompareScreen(onProduct={nav.navigate("product/$it")}) }
        composable("software") { SoftwareScreen() }
        composable("admin") { AdminScreen(online=online) }
        composable("settings") { SettingsScreen() }
        composable("database") { DatabaseInfoScreen(online=online, onAdmin={nav.navigate("admin")}) }
    }
}

internal fun destinationSelected(current: String, destination: String): Boolean = when (destination) {
    "products" -> current == "products" || current == "product/{id}"
    "more" -> current in setOf("more", "compare", "software", "database", "settings", "admin")
    else -> current == destination
}
