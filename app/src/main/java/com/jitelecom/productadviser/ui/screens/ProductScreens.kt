package com.jitelecom.productadviser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jitelecom.productadviser.domain.model.ProductSpec
import com.jitelecom.productadviser.domain.model.ProductCategory
import com.jitelecom.productadviser.ui.ProductDetailViewModel
import com.jitelecom.productadviser.ui.ProductsViewModel
import com.jitelecom.productadviser.ui.ProductSort

@Composable
fun ProductsScreen(onProduct:(Long)->Unit, viewModel:ProductsViewModel= hiltViewModel()){
    val query by viewModel.query.collectAsState();val products by viewModel.products.collectAsState();val minPrice by viewModel.minimumPrice.collectAsState();val maxPrice by viewModel.maximumPrice.collectAsState();val category by viewModel.category.collectAsState();val brand by viewModel.brand.collectAsState();val availableOnly by viewModel.availableOnly.collectAsState();val sort by viewModel.sort.collectAsState();val brands by viewModel.brands.collectAsState();val categories by viewModel.categories.collectAsState()
    val filtered=minPrice.isNotBlank()||maxPrice.isNotBlank()||category!=null||brand!=null||availableOnly||sort!=ProductSort.NAME
    Column(Modifier.fillMaxSize().padding(20.dp)){
        ScreenHeader("Find Product","Search and filter the offline catalog")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value=query,onValueChange=viewModel::setQuery,modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Brand, model, SKU, CPU, GPU, RAM or storage")},leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={if(query.isNotBlank())IconButton(onClick={viewModel.setQuery("")}){Icon(Icons.Default.Clear,"Clear search")}},keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={viewModel.submitSearch()}))
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){item{FilterChip(selected=category==null,onClick={viewModel.category.value=null},label={Text("All")})};items(categories){type->FilterChip(selected=category==type,onClick={viewModel.category.value=type},label={Text(type.displayLabel())})}}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OptionalSelector("Brand",brands,brand,{it},modifier=Modifier.weight(1f)){viewModel.brand.value=it};Selector("Sort",ProductSort.entries,sort,{it.label},modifier=Modifier.weight(1f)){viewModel.sort.value=it}}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(minPrice,{viewModel.minimumPrice.value=it.filter(Char::isDigit)},label={Text("Min ₱")},modifier=Modifier.weight(1f),singleLine=true);OutlinedTextField(maxPrice,{viewModel.maximumPrice.value=it.filter(Char::isDigit)},label={Text("Max ₱")},modifier=Modifier.weight(1f),singleLine=true)}
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Available products only",Modifier.weight(1f));Switch(availableOnly,{viewModel.availableOnly.value=it});if(filtered)TextButton(onClick=viewModel::resetFilters){Text("Reset filters")}}
        Text("${products.size} result${if(products.size==1)"" else "s"}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        if(products.isEmpty())EmptyState(Icons.Default.SearchOff,"No products found","Try a shorter search or reset the filters.")else LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){items(products,key={it.id}){product->ProductCard(product,{onProduct(product.id)})};item{Spacer(Modifier.height(16.dp))}}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(onBack:()->Unit,onCompatibility:(Long)->Unit,viewModel:ProductDetailViewModel=hiltViewModel()){
    val product by viewModel.product.collectAsState();val loading by viewModel.loading.collectAsState();val settings by viewModel.settings.collectAsState()
    Scaffold(topBar={TopAppBar(title={Text("Product details")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back")}},actions={product?.let{p->IconButton(onClick=viewModel::toggleFavorite){Icon(if(p.id in settings.favoriteProductIds)Icons.Default.Favorite else Icons.Default.FavoriteBorder,if(p.id in settings.favoriteProductIds)"Remove favorite" else "Add favorite")}}})}){padding->
        product?.let{p->LazyColumn(Modifier.padding(padding).fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            item{Spacer(Modifier.height(4.dp));Row(verticalAlignment=Alignment.Top){Surface(Modifier.size(84.dp),shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.primaryContainer){Box(contentAlignment=Alignment.Center){Icon(Icons.Default.Laptop,null,Modifier.size(42.dp))}};Spacer(Modifier.width(16.dp));Column(Modifier.weight(1f)){Text(p.displayName,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(p.sku,style=MaterialTheme.typography.bodySmall);Text(peso(p.effectivePrice),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary);VerificationBadge(p.verificationStatus)}}}
            item{Button(onClick={onCompatibility(p.id)},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.CheckCircle,null);Spacer(Modifier.width(8.dp));Text("CHECK SOFTWARE COMPATIBILITY")}}
            item{SpecSection(p)}
            if(p.ramUpgradeable!=null || p.additionalStorageSupport!=null) item { Card { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) { Text("Upgrade options",fontWeight=FontWeight.Bold);p.ramUpgradeable?.let { InfoRow("Memory upgrade",if(it) "Supported" else "Onboard memory") };p.maximumRamGB?.let { InfoRow("Maximum memory","$it GB") };p.additionalStorageSupport?.let { InfoRow("Storage slots",it) } } } }
            p.notes?.takeIf { it.isNotBlank() }?.let { notes -> item { Card { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text("Specification notes",fontWeight=FontWeight.Bold);Text(notes,style=MaterialTheme.typography.bodySmall) } } } }
            if(p.verificationStatus!=com.jitelecom.productadviser.domain.model.VerificationStatus.VERIFIED)item{NoticeCard("This product record is ${p.verificationStatus.name.lowercase().replace('_',' ')}. Confirm its specifications against the official source.",true)}
            item{Spacer(Modifier.height(24.dp))}
        }}?:Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.Center){if(loading)CircularProgressIndicator()else EmptyState(Icons.Default.SearchOff,"Product not found","It may have been archived or removed from the local catalog.")}
    }
}

private fun ProductCategory.displayLabel()=name.lowercase().replace('_',' ').replaceFirstChar(Char::uppercase)

@Composable private fun SpecSection(p:ProductSpec){Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text("Technical specifications",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);InfoRow("Processor",p.processor?.displayName?:"Unknown");InfoRow("Internal CPU tier",p.processor?.performanceTier?.toString()?:"Unknown");InfoRow("Graphics",p.gpu?.displayName?:"Unknown");InfoRow("Internal GPU tier",p.gpu?.performanceTier?.toString()?:"Unknown");InfoRow("Memory",p.ramGB?.let{"$it GB ${p.ramType.orEmpty()}"}?:"Unknown");InfoRow("Storage",p.storageGB?.let{"$it GB ${p.storageType.orEmpty()}"}?:"Unknown");InfoRow("Display",listOfNotNull(p.displaySize?.let{"$it in"},p.displayResolution,p.displayRefreshRate?.let{"$it Hz"}).joinToString(" • ").ifBlank{"Unknown"});InfoRow("Operating system",p.operatingSystem?:"Unknown");InfoRow("Battery",p.batteryCapacityWh?.let{"$it Wh"}?:"Unknown");InfoRow("Weight",p.weightKg?.let{"$it kg"}?:"Unknown");InfoRow("Availability",p.availabilityStatus.name.replace('_',' ').lowercase().replaceFirstChar{it.uppercase()});Text("Availability is a local catalog value, not live branch inventory.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable fun InfoRow(label:String,value:String){Row(Modifier.fillMaxWidth()){Text(label,Modifier.width(128.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)}}
