package com.jitelecom.productadviser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jitelecom.productadviser.domain.model.ProductSpec
import com.jitelecom.productadviser.ui.HomeViewModel

@Composable
fun HomeScreen(
    online: Boolean,
    onNavigate: (String) -> Unit,
    onSearch: (String) -> Unit,
    onProduct: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val software by viewModel.software.collectAsState()
    val version by viewModel.databaseVersion.collectAsState()
    val popular by viewModel.popular.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val favorites = remember(products, settings.favoriteProductIds) {
        settings.favoriteProductIds.mapNotNull { id -> products.firstOrNull { it.id == id } }
    }
    val recent = remember(products, settings.recentProductIds) {
        settings.recentProductIds.mapNotNull { id -> products.firstOrNull { it.id == id } }
    }
    var search by rememberSaveable { mutableStateOf("") }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(164.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("JI PRODUCT ADVISER", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Find the right device with confidence.", style = MaterialTheme.typography.headlineSmall)
                    }
                    OnlineBadge(online)
                }
                HomeHero(onNavigate = onNavigate)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ElevatedCard(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    placeholder = { Text("Search a product, CPU, model or feature") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = { onSearch(search) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Open search")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(search) })
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("Start here", style = MaterialTheme.typography.titleLarge)
                Text("Choose a tool for your customer conversation.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { HomeActionCard("Can it run?", "Check a device against software", Icons.Default.CheckCircle) { onNavigate("compatibility") } }
        item { HomeActionCard("Recommend", "Match needs, budget and use", Icons.Default.AutoAwesome) { onNavigate("recommend") } }
        item { HomeActionCard("Browse catalog", "Search the product lineup", Icons.Default.Search) { onNavigate("products") } }
        item { HomeActionCard("Compare", "Compare up to four products", Icons.AutoMirrored.Filled.CompareArrows) { onNavigate("compare") } }
        item(span = { GridItemSpan(maxLineSpan) }) {
            CatalogPulse(products.size, software.size, version, popular.map { it.key })
        }
        if (favorites.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { ProductShortcutRow("Saved products", favorites, true, onProduct) }
        }
        if (recent.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { ProductShortcutRow("Recently viewed", recent, false, onProduct) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            NoticeCard("The catalog and compatibility checks are available offline, so you can assist customers anywhere.")
        }
    }
}

@Composable
private fun HomeHero(onNavigate: (String) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF0A365A))))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = Color.White.copy(alpha = .14f), contentColor = Color.White, shape = MaterialTheme.shapes.extraSmall) {
                Text("BUILT FOR THE SALES FLOOR", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Text("Turn questions into clear product choices.", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text("Compare specs, check software fit and give better recommendations in one place.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .88f))
            Button(
                onClick = { onNavigate("compatibility") },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.small
            ) {
                Text("Check compatibility")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun HomeActionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 156.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp, pressedElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Surface(Modifier.size(46.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.align(Alignment.End).size(19.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CatalogPulse(productCount: Int, softwareCount: Int, version: String?, popular: List<String>) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(46.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.tertiary) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.onTertiary) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Your offline catalog", style = MaterialTheme.typography.titleMedium)
                Text("$productCount products · $softwareCount software profiles", style = MaterialTheme.typography.bodyMedium)
                if (popular.isNotEmpty()) Text("Popular: ${popular.take(2).joinToString()}", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            version?.let { Text("v$it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer) }
        }
    }
}

@Composable
private fun ProductShortcutRow(title: String, products: List<ProductSpec>, favorite: Boolean, onProduct: (Long) -> Unit) {
    ElevatedCard(shape = MaterialTheme.shapes.medium, colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.History, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(7.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(9.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(products, key = { it.id }) { product ->
                    AssistChip(onClick = { onProduct(product.id) }, label = { Text(product.displayName, maxLines = 1) })
                }
            }
        }
    }
}

