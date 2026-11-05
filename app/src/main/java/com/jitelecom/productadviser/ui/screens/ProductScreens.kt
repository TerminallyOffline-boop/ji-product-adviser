package com.jitelecom.productadviser.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jitelecom.productadviser.domain.model.ProductSpec
import com.jitelecom.productadviser.ui.ProductDetailViewModel
import com.jitelecom.productadviser.ui.ProductSort
import com.jitelecom.productadviser.ui.ProductsViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun ProductsScreen(onProduct: (Long) -> Unit, viewModel: ProductsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val query by viewModel.query.collectAsState()
    val products by viewModel.products.collectAsState()
    val minPrice by viewModel.minimumPrice.collectAsState()
    val maxPrice by viewModel.maximumPrice.collectAsState()
    val category by viewModel.category.collectAsState()
    val brand by viewModel.brand.collectAsState()
    val availableOnly by viewModel.availableOnly.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val brands by viewModel.brands.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val online by viewModel.online.collectAsState()
    val filtered = minPrice.isNotBlank() || maxPrice.isNotBlank() || category != null || brand != null || availableOnly || sort != ProductSort.NAME

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader("Browse products", "Search your inventory or look up current models online.")
        }
        item {
            ElevatedCard(shape = MaterialTheme.shapes.medium, elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    placeholder = { Text("Search brand, model, CPU, GPU, RAM or storage") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotBlank()) IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Default.Close, "Clear search") }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.submitSearch() })
                )
            }
        }
        item {
            ElevatedCard(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Find a current model", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        OnlineBadge(online)
                    }
                    Text(
                        if (online) "Search the web or phone specifications using your query above."
                        else "Connect to Wi-Fi or mobile data to search current products online.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val url = Uri.parse("https://www.google.com/search").buildUpon()
                                    .appendQueryParameter("q", "$query phone tablet laptop specifications")
                                    .build()
                                context.startActivity(Intent(Intent.ACTION_VIEW, url))
                            },
                            enabled = online && query.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Public, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Search web")
                        }
                        OutlinedButton(
                            onClick = {
                                val url = Uri.parse("https://www.gsmarena.com/results.php3").buildUpon()
                                    .appendQueryParameter("sQuickSearch", "yes")
                                    .appendQueryParameter("sName", query)
                                    .build()
                                context.startActivity(Intent(Intent.ACTION_VIEW, url))
                            },
                            enabled = online && query.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhoneAndroid, null)
                            Spacer(Modifier.width(6.dp))
                            Text("GSMArena")
                        }
                    }
                    Text(
                        "Online listings are reference information. Confirm actual stock and price before advising.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            ProductFilters(
                categories = categories,
                category = category,
                brands = brands,
                brand = brand,
                sort = sort,
                minPrice = minPrice,
                maxPrice = maxPrice,
                availableOnly = availableOnly,
                onCategory = { viewModel.category.value = it },
                onBrand = { viewModel.brand.value = it },
                onSort = { viewModel.sort.value = it },
                onMinPrice = { viewModel.minimumPrice.value = it.filter(Char::isDigit) },
                onMaxPrice = { viewModel.maximumPrice.value = it.filter(Char::isDigit) },
                onAvailableOnly = { viewModel.availableOnly.value = it },
                onReset = viewModel::resetFilters,
                showReset = filtered
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${products.size} result${if (products.size == 1) "" else "s"}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                if (filtered) TextButton(onClick = viewModel::resetFilters) { Text("Clear filters") }
            }
        }
        if (products.isEmpty()) {
            item { EmptyState(Icons.Default.SearchOff, "No products found", "Try a shorter search or clear your filters.") }
        } else {
            items(products, key = { it.id }) { product -> ProductCard(product = product, onClick = { onProduct(product.id) }) }
        }
    }
}

@Composable
private fun ProductFilters(
    categories: List<com.jitelecom.productadviser.domain.model.ProductCategory>,
    category: com.jitelecom.productadviser.domain.model.ProductCategory?,
    brands: List<String>,
    brand: String?,
    sort: ProductSort,
    minPrice: String,
    maxPrice: String,
    availableOnly: Boolean,
    onCategory: (com.jitelecom.productadviser.domain.model.ProductCategory?) -> Unit,
    onBrand: (String?) -> Unit,
    onSort: (ProductSort) -> Unit,
    onMinPrice: (String) -> Unit,
    onMaxPrice: (String) -> Unit,
    onAvailableOnly: (Boolean) -> Unit,
    onReset: () -> Unit,
    showReset: Boolean
) {
    ElevatedCard(shape = MaterialTheme.shapes.medium, elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Filter and sort", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                if (showReset) TextButton(onClick = onReset) { Text("Reset") }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = category == null, onClick = { onCategory(null) }, label = { Text("All categories") }) }
                items(categories) { type ->
                    FilterChip(selected = category == type, onClick = { onCategory(type) }, label = { Text(type.label()) })
                }
            }
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionalSelector("Brand", brands, brand, { it }, Modifier.weight(1f), onSelected = onBrand)
                Selector("Sort", ProductSort.entries, sort, { it.label }, Modifier.weight(1f), onSelected = onSort)
            }
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(minPrice, onMinPrice, label = { Text("Min ₱") }, modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.small)
                OutlinedTextField(maxPrice, onMaxPrice, label = { Text("Max ₱") }, modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.small)
            }
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("In-stock only", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Hide products with current unavailable or stale stock data", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = availableOnly, onCheckedChange = onAvailableOnly)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    onBack: () -> Unit,
    onCompatibility: (Long) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val product by viewModel.product.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val settings by viewModel.settings.collectAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Product details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    product?.let { p ->
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                if (p.id in settings.favoriteProductIds) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                if (p.id in settings.favoriteProductIds) "Remove from saved products" else "Save product"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        product?.let { p ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { ProductHero(p) }
                item {
                    Button(onClick = { onCompatibility(p.id) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Check software compatibility")
                    }
                }
                item { SpecSection(p) }
                p.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    item {
                        ElevatedCard(shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("Specification notes", style = MaterialTheme.typography.titleMedium)
                                Text(notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (loading) CircularProgressIndicator() else EmptyState(Icons.Default.SearchOff, "Product not found", "It may have been removed from the local catalog.")
        }
    }
}

@Composable
private fun ProductHero(product: ProductSpec) {
    val ageDays = product.lastUpdated?.let { value ->
        runCatching { ChronoUnit.DAYS.between(LocalDate.parse(value), LocalDate.now()) }.getOrNull()
            ?: runCatching { ChronoUnit.DAYS.between(java.time.YearMonth.parse(value).atEndOfMonth(), LocalDate.now()) }.getOrNull()
    }
    val stockStatus = product.currentAvailability()
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(76.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary) {
                Box(contentAlignment = Alignment.Center) { Icon(productIcon(product.category), null, Modifier.size(37.dp), tint = MaterialTheme.colorScheme.onPrimary) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(product.category.label(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(product.displayName, style = MaterialTheme.typography.titleLarge)
                product.sku.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(peso(product.effectivePrice), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Text(
                    when {
                        ageDays == null -> "Specification update date unavailable"
                        ageDays > 90 -> "Specifications last updated $ageDays days ago"
                        else -> "Specifications updated ${product.lastUpdated}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ageDays == null || ageDays > 90) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Stock: ${stockStatus.name.lowercase().replace('_', ' ')}${product.availabilityUpdatedAt?.let { " • checked $it" } ?: " • no recent check"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (stockStatus == com.jitelecom.productadviser.domain.model.AvailabilityStatus.UNKNOWN) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Catalog price is not live; confirm before quoting.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SpecSection(product: ProductSpec) {
    ElevatedCard(shape = MaterialTheme.shapes.medium, elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Technical specifications", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(7.dp))
            InfoRow("Processor", product.processor?.displayName ?: "Not listed")
            InfoRow("CPU performance tier", product.processor?.performanceTier?.toString() ?: "Not listed")
            InfoRow("Graphics", product.gpu?.displayName ?: "Not listed")
            InfoRow("GPU performance tier", product.gpu?.performanceTier?.toString() ?: "Not listed")
            InfoRow("Memory", product.ramGB?.let { "$it GB ${product.ramType.orEmpty()}" } ?: "Not listed")
            InfoRow("Storage", product.storageGB?.let { "$it GB ${product.storageType.orEmpty()}" } ?: "Not listed")
            InfoRow("Display", listOfNotNull(product.displaySize?.let { "$it in" }, product.displayResolution, product.displayRefreshRate?.let { "$it Hz" }).joinToString(" · ").ifBlank { "Not listed" })
            InfoRow("Operating system", product.operatingSystem ?: "Not listed")
            product.ramUpgradeable?.let { InfoRow("Memory upgrade", if (it) "Supported" else "Onboard memory") }
            product.maximumRamGB?.let { InfoRow("Maximum memory", "$it GB") }
            product.additionalStorageSupport?.let { InfoRow("Storage expansion", it) }
            product.batteryCapacityWh?.let { InfoRow("Battery", "$it Wh") }
            product.weightKg?.let { InfoRow("Weight", "$it kg") }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.width(132.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

