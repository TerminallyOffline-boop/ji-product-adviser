package com.jitelecom.productadviser.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jitelecom.productadviser.domain.model.*
import com.jitelecom.productadviser.ui.CompareViewModel
import com.jitelecom.productadviser.ui.SoftwareViewModel

@Composable
fun CompareScreen(onProduct:(Long)->Unit,viewModel:CompareViewModel=hiltViewModel()){
    val products by viewModel.products.collectAsState();val selected by viewModel.selected.collectAsState();val chosen=products.filter{it.id in selected}
    val software by viewModel.software.collectAsState();val softwareId by viewModel.selectedSoftware.collectAsState();val compatibility by viewModel.compatibility.collectAsState();val message by viewModel.message.collectAsState();var query by remember{mutableStateOf("")};var category by remember{mutableStateOf<ProductCategory?>(null)};var differencesOnly by remember{mutableStateOf(true)}
    val categories=remember(products){products.map{it.category}.distinct().sortedBy{it.name}}
    val filtered=remember(products,query,category){products.filter{(category==null||it.category==category)&&(query.isBlank()||it.displayName.contains(query,true)||it.sku.contains(query,true)||it.processor?.displayName?.contains(query,true)==true)}}
    Column(Modifier.fillMaxSize().padding(20.dp)){
        ScreenHeader("Compare Products","Select 2–4; differences are shown without a universal winner")
        Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(if(chosen.size<2)"Select ${2-chosen.size} more product${if(chosen.size==1)"" else "s"}." else "${chosen.size} products selected",Modifier.weight(1f),style=MaterialTheme.typography.labelMedium);if(chosen.isNotEmpty())TextButton(onClick=viewModel::clear){Text("Clear")}}
        message?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
            item{OutlinedTextField(query,{query=it},label={Text("Search products")},leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={if(query.isNotBlank())IconButton(onClick={query=""}){Icon(Icons.Default.Clear,"Clear")}},singleLine=true,modifier=Modifier.fillMaxWidth())}
            item{LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){item{FilterChip(selected=category==null,onClick={category=null},label={Text("All")})};items(categories){type->FilterChip(selected=category==type,onClick={category=type},label={Text(type.name.lowercase().replace('_',' ').replaceFirstChar(Char::uppercase))})}}}
            items(filtered,key={it.id}){product->OutlinedCard(onClick={viewModel.toggle(product.id)}){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(checked=product.id in selected,onCheckedChange={viewModel.toggle(product.id)});Column(Modifier.weight(1f)){Text(product.displayName,fontWeight=FontWeight.SemiBold);Text("${peso(product.effectivePrice)} • ${product.processor?.model?:"CPU unknown"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
            if(chosen.size>=2)item{SearchableSelector("Compatibility for (optional)",software,software.firstOrNull{it.id==softwareId},{it.displayName}){viewModel.selectedSoftware.value=it.id}}
            if(chosen.size>=2)item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Show differences only",Modifier.weight(1f));Switch(differencesOnly,{differencesOnly=it})}}
            if(chosen.size>=2)item{ComparisonTable(chosen,compatibility,differencesOnly,onProduct)}
        }
    }
}

@Composable
private fun ComparisonTable(products:List<ProductSpec>,compatibility:Map<Long,CompatibilityResult>,differencesOnly:Boolean,onProduct:(Long)->Unit){
    val rows=listOf(
        "Price" to products.map{peso(it.effectivePrice)},
        "CPU" to products.map{it.processor?.displayName ?: "Unknown"},
        "CPU tier" to products.map{it.processor?.performanceTier?.toString() ?: "Unknown"},
        "GPU" to products.map{it.gpu?.displayName ?: "Unknown"},
        "GPU tier" to products.map{it.gpu?.performanceTier?.toString() ?: "Unknown"},
        "RAM" to products.map{it.ramGB?.let{"$it GB"} ?: "Unknown"},
        "Storage" to products.map{it.storageGB?.let{"$it GB"} ?: "Unknown"},
        "Display" to products.map{it.displaySize?.let{"$it in"} ?: "Unknown"},
        "Weight" to products.map{it.weightKg?.let{"$it kg"} ?: "Unknown"},
        "Battery" to products.map{it.batteryCapacityWh?.let{"$it Wh"} ?: "Unknown"}
    ) + if(compatibility.isNotEmpty()) listOf("Software" to products.map{compatibility[it.id]?.status?.name?.replace('_',' ') ?: "Not checked"}) else emptyList()
    val visibleRows=if(differencesOnly)rows.filter{(_,values)->values.distinct().size>1}else rows
    Column(Modifier.horizontalScroll(rememberScrollState())){
        CompareRow("",products.map{it.displayName},header=true)
        visibleRows.forEach{(label,values)->CompareRow(label,values)}
        if(visibleRows.isEmpty())Text("The selected products have no different stored values in these fields.",Modifier.padding(16.dp),style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
        Row { Spacer(Modifier.width(115.dp)); products.forEach { OutlinedButton(onClick={onProduct(it.id)},modifier=Modifier.width(190.dp).padding(horizontal=4.dp)){Text("View details")} } }
    }
}
@Composable private fun CompareRow(label:String,values:List<String>,header:Boolean=false){Row(Modifier.padding(vertical=3.dp),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.width(115.dp),fontWeight=FontWeight.Bold);values.forEach{value->Surface(Modifier.width(190.dp).padding(horizontal=4.dp),color=if(header)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,shape=MaterialTheme.shapes.small){Text(value,Modifier.padding(10.dp),style=if(header)MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,fontWeight=if(header)FontWeight.Bold else FontWeight.Normal)}}}}

@Composable
fun SoftwareScreen(viewModel:SoftwareViewModel=hiltViewModel()){
    val software by viewModel.software.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        item{ScreenHeader("Software & Apps","Version-aware local requirements catalog")}
        items(software,key={it.id}){app->
            OutlinedCard{Column(Modifier.padding(15.dp)){
                Row{Column(Modifier.weight(1f)){Text(app.displayName,fontWeight=FontWeight.Bold);Text("${app.category} • ${app.platform}",style=MaterialTheme.typography.bodySmall)};VerificationBadge(app.verificationStatus)}
                app.developer?.let{Text(it,style=MaterialTheme.typography.bodyMedium)}
                app.description?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }}
        }
        item{Spacer(Modifier.height(20.dp))}
    }
}

@Composable
fun MoreScreen(onNavigate:(String)->Unit){
    val menuItems=listOf(
        Triple("Compare products","Side-by-side strengths",Icons.AutoMirrored.Filled.CompareArrows) to "compare",
        Triple("Software & Apps","Browse versioned requirements",Icons.Default.Apps) to "software",
        Triple("Database","Version and update status",Icons.Default.Storage) to "database",
        Triple("Settings","Theme and remote source",Icons.Default.Settings) to "settings",
        Triple("Admin","Manage and import records",Icons.Default.AdminPanelSettings) to "admin"
    )
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        item{ScreenHeader("More")}
        items(menuItems){(info,route)->ElevatedCard(onClick={onNavigate(route)}){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(info.third,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(info.first,fontWeight=FontWeight.Bold);Text(info.second,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Default.ChevronRight,null)}}}
    }
}
