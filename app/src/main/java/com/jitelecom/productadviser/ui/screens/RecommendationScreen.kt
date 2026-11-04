package com.jitelecom.productadviser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jitelecom.productadviser.domain.model.ProductCategory
import com.jitelecom.productadviser.domain.model.RecommendationResult
import com.jitelecom.productadviser.ui.RecommendationViewModel

private val profiles = listOf("Student", "Teacher", "Office Worker", "Business Owner", "Engineering Student", "Architecture Student", "Programmer", "Graphic Designer", "Video Editor", "Gamer", "Content Creator")
private val deviceTypes = listOf(ProductCategory.LAPTOP, ProductCategory.SMARTPHONE, ProductCategory.TABLET)
private val priorityChoices = listOf("Performance", "Battery", "Portability", "RAM / storage")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendationScreen(onProduct: (Long) -> Unit, viewModel: RecommendationViewModel = hiltViewModel()) {
    val budget by viewModel.budget.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val category by viewModel.category.collectAsState()
    val software by viewModel.applicableSoftware.collectAsState()
    val selected by viewModel.selectedSoftware.collectAsState()
    val priorities by viewModel.priorities.collectAsState()
    val brand by viewModel.brand.collectAsState()
    val above by viewModel.aboveBudget.collectAsState()
    val results by viewModel.results.collectAsState()
    val message by viewModel.message.collectAsState()
    val quickRequestMessage by viewModel.quickRequestMessage.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val products by viewModel.products.collectAsState()
    var query by remember { mutableStateOf("") }
    var softwareQuery by remember { mutableStateOf("") }
    var showMoreOptions by remember { mutableStateOf(false) }

    val brands = remember(products, category) { products.filter { it.category == category }.map { it.brand }.distinct().sorted() }
    val filteredSoftware = remember(software, softwareQuery) {
        software.filter { it.displayName.contains(softwareQuery, true) || it.category.contains(softwareQuery, true) }
    }
    val resetAll = {
        viewModel.reset()
        query = ""
        softwareQuery = ""
        showMoreOptions = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Recommend a Gadget", "Find a good fit from the current product catalog") }

        item {
            ElevatedCard {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Describe what they need", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Optional quick request") },
                        placeholder = { Text("₱30,000 laptop for architecture and AutoCAD") },
                        supportingText = { Text("You can also leave this blank and use the choices below.") },
                        minLines = 1,
                        maxLines = 2,
                    )
                    Button(onClick = { viewModel.recommendFromRequest(query) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("FIND MY MATCHES")
                    }
                    quickRequestMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            ElevatedCard {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Your essentials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("What kind of device?", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(deviceTypes) { type ->
                            FilterChip(
                                selected = type == category,
                                onClick = { viewModel.setCategory(type) },
                                label = { Text(type.name.lowercase().replaceFirstChar(Char::uppercase)) },
                                leadingIcon = if (type == category) ({ Icon(Icons.Default.Check, null) }) else null,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { viewModel.budget.value = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Maximum budget (₱)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Default.Payments, null) },
                    )
                    Selector("Main use", profiles, profile, { it }) { viewModel.profile.value = it }
                }
            }
        }

        item {
            OutlinedButton(onClick = { showMoreOptions = !showMoreOptions }, modifier = Modifier.fillMaxWidth()) {
                Icon(if (showMoreOptions) Icons.Default.ExpandLess else Icons.Default.Tune, null)
                Spacer(Modifier.width(8.dp))
                Text(if (showMoreOptions) "HIDE EXTRA PREFERENCES" else "ADD PREFERENCES (OPTIONAL)")
            }
        }

        if (showMoreOptions) {
            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Fine-tune the results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OptionalSelector("Preferred brand", brands, brand.takeIf { it.isNotBlank() }, { it }, noneText = "Any brand") {
                            viewModel.brand.value = it.orEmpty()
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Include slightly higher prices", fontWeight = FontWeight.SemiBold)
                                Text("Up to ${settings.allowAboveBudgetPercent}% over budget", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = above, onCheckedChange = { viewModel.aboveBudget.value = it })
                        }
                        HorizontalDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Apps or games they need", fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = softwareQuery,
                                onValueChange = { softwareQuery = it },
                                label = { Text("Search apps and games") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (filteredSoftware.isEmpty()) {
                                Text("No matching apps for this device type.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    filteredSoftware.forEach { app ->
                                        FilterChip(selected = app.id in selected, onClick = { viewModel.toggleSoftware(app.id) }, label = { Text(app.name) })
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Prioritize", fontWeight = FontWeight.SemiBold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                items(priorityChoices) { priority ->
                                    FilterChip(
                                        selected = priority in priorities,
                                        onClick = { viewModel.priorities.value = if (priority in priorities) priorities - priority else priorities + priority },
                                        label = { Text(priority) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            TextButton(onClick = resetAll, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(6.dp))
                Text("RESET ALL CHOICES")
            }
        }

        message?.let { value ->
            item {
                Text(value, color = if (results.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }

        if (results.isNotEmpty()) {
            item { Text("Best matches", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            itemsIndexed(results, key = { _, result -> result.product.id }) { index, result ->
                RecommendationCard(index + 1, result, software.associateBy { it.id }) { onProduct(result.product.id) }
            }
            item {
                OutlinedButton(onClick = resetAll, modifier = Modifier.fillMaxWidth()) { Text("START ANOTHER CUSTOMER") }
            }
        }

        item {
            NoticeCard("Scores compare the available matches; they are not a probability or a performance guarantee.")
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun RecommendationCard(
    rank: Int,
    result: RecommendationResult,
    software: Map<Long, com.jitelecom.productadviser.domain.model.SoftwareSpec>,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row {
                Text("$rank. ${result.product.displayName}", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("${result.internalScore}/100", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            Text(peso(result.product.effectivePrice), style = MaterialTheme.typography.titleMedium)
            Text(result.explanation, style = MaterialTheme.typography.bodySmall)
            result.compatibility.forEach { (id, compatibility) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(software[id]?.name ?: "Selected app", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    StatusBadge(compatibility.status)
                }
            }
            Text("Strengths: ${result.strengths.joinToString()}", style = MaterialTheme.typography.bodySmall)
            if (result.limitations.isNotEmpty()) {
                Text("Limitations: ${result.limitations.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
