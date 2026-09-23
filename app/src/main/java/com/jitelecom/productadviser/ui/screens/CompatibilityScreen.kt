package com.jitelecom.productadviser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jitelecom.productadviser.domain.model.*
import com.jitelecom.productadviser.ui.CompatibilityViewModel

@Composable
fun CompatibilityScreen(initialProductId: Long? = null, viewModel: CompatibilityViewModel = hiltViewModel()) {
    val products by viewModel.products.collectAsState()
    val software by viewModel.software.collectAsState()
    val compatibleSoftware by viewModel.compatibleSoftware.collectAsState()
    val productId by viewModel.selectedProduct.collectAsState()
    val softwareId by viewModel.selectedSoftware.collectAsState()
    val result by viewModel.result.collectAsState()
    val showUnavailable by viewModel.showUnavailableApps.collectAsState()
    val availableCount by viewModel.availableSoftwareCount.collectAsState()
    val selectedProduct = products.firstOrNull { it.id == productId }
    val selectedApp = software.firstOrNull { it.id == softwareId }

    LaunchedEffect(initialProductId, products) {
        if (initialProductId != null && products.any { it.id == initialProductId } && productId != initialProductId) {
            viewModel.selectProduct(initialProductId)
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .22f))
    ) {
        val wide = maxWidth >= 760.dp
        val pagePadding = if (wide) 24.dp else 16.dp
        Column(
            Modifier.fillMaxSize().padding(horizontal = pagePadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompatibilityHero(selectedProduct?.operatingSystemForCompatibility(), wide)
            if (wide) {
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    LazyColumn(
                        Modifier.widthIn(min = 320.dp, max = 390.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SetupPanel(
                                products, compatibleSoftware, selectedProduct, selectedApp,
                                viewModel::selectProduct, viewModel::selectSoftware, viewModel::evaluate,
                                showUnavailable, viewModel::setShowUnavailable, availableCount, software.size
                            )
                        }
                        item { PlatformNotice(selectedProduct, availableCount, software.size, showUnavailable) }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                    ResultsColumn(result, selectedProduct, selectedApp, true, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        SetupPanel(
                            products, compatibleSoftware, selectedProduct, selectedApp,
                            viewModel::selectProduct, viewModel::selectSoftware, viewModel::evaluate,
                            showUnavailable, viewModel::setShowUnavailable, availableCount, software.size
                        )
                    }
                    item { PlatformNotice(selectedProduct, availableCount, software.size, showUnavailable) }
                    if (result == null) {
                        item { EmptyResultsCard() }
                    } else {
                        item { ResultSummary(result!!, selectedProduct, selectedApp) }
                        items(result!!.components, key = { it.component }) { ComponentResultCard(it) }
                        item { DisclaimerCard(result!!.disclaimer) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompatibilityHero(operatingSystem: String?, wide: Boolean) {
    val gradient = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(red = .08f, green = .27f, blue = .42f))
    )
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Row(
            Modifier.fillMaxWidth().background(gradient).padding(horizontal = if (wide) 24.dp else 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(if (wide) 58.dp else 48.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = .14f)
            ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.FactCheck, null, tint = Color.White, modifier = Modifier.size(30.dp)) } }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Can It Run?", color = Color.White, style = if (wide) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Choose a device and see only apps made for its platform.", color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.bodyMedium)
            }
            if (wide && operatingSystem != null) {
                Surface(color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(50.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(platformIcon(operatingSystem), null, Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(7.dp))
                        Text(platformLabel(operatingSystem), color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupPanel(
    products: List<ProductSpec>,
    compatibleSoftware: List<SoftwareSpec>,
    selectedProduct: ProductSpec?,
    selectedApp: SoftwareSpec?,
    onProduct: (Long) -> Unit,
    onSoftware: (Long) -> Unit,
    onEvaluate: () -> Unit,
    showUnavailable: Boolean,
    onShowUnavailable: (Boolean) -> Unit,
    availableCount: Int,
    totalSoftware: Int
) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Set up your check", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            StepLabel(1, "Choose a device")
            SearchableSelector("Device", products, selectedProduct, { "${it.displayName} • ${peso(it.effectivePrice)}" }, onSelected = { onProduct(it.id) })
            selectedProduct?.let { DeviceSnapshot(it) }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
            StepLabel(2, "Choose an app")
            if (selectedProduct != null) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Show unavailable apps",fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.bodyMedium);Text("$availableCount of $totalSoftware apps match this platform",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(showUnavailable,onShowUnavailable)}
            if (selectedProduct == null) {
                Text("Select a device first so the app list can match its operating system.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (compatibleSoftware.isEmpty()) {
                NoticeCard("No platform-matched apps are stored for this device yet.", warning = true)
            } else {
                SearchableSelector("App or game", compatibleSoftware, selectedApp, { app -> if(app.supportsOperatingSystem(selectedProduct.operatingSystemForCompatibility())) app.displayName else "${app.displayName} • Not available" }, onSelected = { onSoftware(it.id) })
                selectedApp?.let { SoftwareSnapshot(it) }
            }

            Button(
                onClick = onEvaluate,
                enabled = selectedProduct != null && selectedApp != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Rule, null)
                Spacer(Modifier.width(9.dp))
                Text("CHECK COMPATIBILITY", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StepLabel(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
            Text(number.toString(), Modifier.padding(horizontal = 9.dp, vertical = 5.dp), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(9.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DeviceSnapshot(product: ProductSpec) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .48f)) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(platformIcon(product.operatingSystemForCompatibility()), null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(7.dp))
                Text(product.operatingSystemForCompatibility() ?: "Operating system unknown", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                listOfNotNull(product.processor?.model, product.ramGB?.let { "$it GB RAM" }, product.gpu?.model).joinToString(" • ").ifBlank { "Hardware details are incomplete" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SoftwareSnapshot(app: SoftwareSpec) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
            Text(app.platform, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
        Spacer(Modifier.width(8.dp))
        Text(app.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlatformNotice(product: ProductSpec?, availableCount: Int, totalCount: Int, showUnavailable: Boolean) {
    val text = when {
        product == null -> "The catalog separates desktop and mobile apps. Pick a device to filter the list."
        showUnavailable -> "Showing all $totalCount apps. The $availableCount apps made for ${platformLabel(product.operatingSystemForCompatibility())} can be checked normally; unavailable apps receive a separate Not Available result."
        else -> "Showing $availableCount app${if (availableCount == 1) "" else "s"} available for ${platformLabel(product.operatingSystemForCompatibility())}. Enable Show unavailable apps to explain why another app cannot run on this platform."
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .55f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = .25f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.FilterAlt, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun ResultsColumn(
    result: CompatibilityResult?,
    product: ProductSpec?,
    app: SoftwareSpec?,
    grid: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        if (result == null) {
            item { EmptyResultsCard() }
        } else {
            item { ResultSummary(result, product, app) }
            if (grid) {
                items(result.components.chunked(2), key = { row -> row.joinToString { it.component } }) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { ComponentResultCard(it, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                items(result.components, key = { it.component }) { ComponentResultCard(it) }
            }
            item { DisclaimerCard(result.disclaimer) }
        }
    }
}

@Composable
private fun EmptyResultsCard() {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(72.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Devices, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.height(16.dp))
            Text("Your result will appear here", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Select a device and an app, then run the check.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ResultSummary(result: CompatibilityResult, product: ProductSpec?, app: SoftwareSpec?) {
    val color = statusColor(result.status)
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .10f)),
        border = BorderStroke(1.dp, color.copy(alpha = .32f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(48.dp), shape = RoundedCornerShape(16.dp), color = color.copy(alpha = .16f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(statusIcon(result.status), null, tint = color, modifier = Modifier.size(28.dp)) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(app?.displayName ?: "Compatibility result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(product?.displayName.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(result.status)
            }
            Text(result.explanation, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ComponentResultCard(result: ComponentCompatibilityResult, modifier: Modifier = Modifier) {
    val color = componentStatusColor(result.status)
    OutlinedCard(modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp), border = BorderStroke(1.dp, color.copy(alpha = .30f))) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(38.dp), shape = RoundedCornerShape(12.dp), color = color.copy(alpha = .12f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(componentIcon(result.component), null, Modifier.size(21.dp), tint = color) }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(result.component, fontWeight = FontWeight.Bold)
                    Text(componentStatusLabel(result.status), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                }
            }
            InfoRow("Device", result.actual)
            result.minimum?.let { InfoRow("Minimum", it) }
            result.recommended?.let { InfoRow("Recommended", it) }
            Text(result.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DisclaimerCard(text: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun platformIcon(value: String?): ImageVector = when {
    PlatformFamily.MACOS in platformFamilies(value) -> Icons.Default.LaptopMac
    PlatformFamily.ANDROID in platformFamilies(value) -> Icons.Default.Android
    PlatformFamily.IOS in platformFamilies(value) -> Icons.Default.PhoneIphone
    else -> Icons.Default.Computer
}

private fun componentIcon(component: String): ImageVector = when (component.lowercase()) {
    "cpu", "ram" -> Icons.Default.Memory
    "gpu" -> Icons.Default.DeveloperBoard
    "storage" -> Icons.Default.Storage
    "operating system" -> Icons.Default.Devices
    "architecture" -> Icons.Default.AccountTree
    else -> Icons.Default.Tune
}

private fun statusIcon(status: CompatibilityStatus): ImageVector = when (status) {
    CompatibilityStatus.MEETS_RECOMMENDED -> Icons.Default.CheckCircle
    CompatibilityStatus.MEETS_MINIMUM -> Icons.Default.Verified
    CompatibilityStatus.BELOW_MINIMUM -> Icons.Default.Error
    CompatibilityStatus.NOT_AVAILABLE -> Icons.Default.Block
    CompatibilityStatus.NOT_VERIFIED -> Icons.AutoMirrored.Filled.Help
}

private fun componentStatusLabel(status: ComponentStatus): String = when (status) {
    ComponentStatus.MEETS_RECOMMENDED -> "MEETS RECOMMENDED"
    ComponentStatus.MEETS_MINIMUM -> "MEETS MINIMUM"
    ComponentStatus.BELOW_MINIMUM -> "BELOW MINIMUM"
    ComponentStatus.NOT_AVAILABLE -> "NOT AVAILABLE"
    ComponentStatus.UNKNOWN -> "NEEDS INFORMATION"
    ComponentStatus.NOT_APPLICABLE -> "NOT REQUIRED"
}

private fun componentStatusColor(status: ComponentStatus) = when (status) {
    ComponentStatus.MEETS_RECOMMENDED -> Color(0xFF11845B)
    ComponentStatus.MEETS_MINIMUM -> Color(0xFFB26800)
    ComponentStatus.BELOW_MINIMUM -> Color(0xFFBA2D2D)
    ComponentStatus.NOT_AVAILABLE -> Color(0xFF7A3E9D)
    else -> Color(0xFF5E6472)
}

private fun statusColor(status: CompatibilityStatus) = when (status) {
    CompatibilityStatus.MEETS_RECOMMENDED -> Color(0xFF11845B)
    CompatibilityStatus.MEETS_MINIMUM -> Color(0xFFB26800)
    CompatibilityStatus.BELOW_MINIMUM -> Color(0xFFBA2D2D)
    CompatibilityStatus.NOT_AVAILABLE -> Color(0xFF7A3E9D)
    CompatibilityStatus.NOT_VERIFIED -> Color(0xFF5E6472)
}
