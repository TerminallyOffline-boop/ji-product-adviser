package com.jitelecom.productadviser.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val software by viewModel.software.collectAsState();val softwareId by viewModel.selectedSoftware.collectAsState();val compatibility by viewModel.compatibility.collectAsState()
    Column(Modifier.fillMaxSize().padding(20.dp)){
        ScreenHeader("Compare Products","Select 2–4; differences are shown without a universal winner")
        Spacer(Modifier.height(10.dp))
        if(chosen.size<2) Text("Select ${2-chosen.size} more product${if(chosen.size==1)"" else "s"}.",style=MaterialTheme.typography.labelMedium) else Text("${chosen.size} products selected",style=MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
            item{products.chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{p->FilterChip(selected=p.id in selected,onClick={viewModel.toggle(p.id)},label={Text(p.model)},modifier=Modifier.weight(1f))};if(row.size==1)Spacer(Modifier.weight(1f))}}}
            if(chosen.size>=2)item{Selector("Compatibility for (optional)",software,software.firstOrNull{it.id==softwareId},{it.displayName}){viewModel.selectedSoftware.value=it.id}}
            if(chosen.size>=2)item{ComparisonTable(chosen,compatibility,onProduct)}
        }
    }
}

@Composable
private fun ComparisonTable(products:List<ProductSpec>,compatibility:Map<Long,CompatibilityResult>,onProduct:(Long)->Unit){
    Column(Modifier.horizontalScroll(rememberScrollState())){
        CompareRow("",products.map{it.displayName},header=true)
        CompareRow("Price",products.map{peso(it.effectivePrice)})
        CompareRow("CPU",products.map{it.processor?.displayName ?: "Unknown"})
        CompareRow("CPU tier",products.map{it.processor?.performanceTier?.toString() ?: "Unknown"})
        CompareRow("GPU",products.map{it.gpu?.displayName ?: "Unknown"})
        CompareRow("GPU tier",products.map{it.gpu?.performanceTier?.toString() ?: "Unknown"})
        CompareRow("RAM",products.map{it.ramGB?.let{"$it GB"} ?: "Unknown"})
        CompareRow("Storage",products.map{it.storageGB?.let{"$it GB"} ?: "Unknown"})
        CompareRow("Display",products.map{it.displaySize?.let{"$it in"} ?: "Unknown"})
        CompareRow("Weight",products.map{it.weightKg?.let{"$it kg"} ?: "Unknown"})
        CompareRow("Battery",products.map{it.batteryCapacityWh?.let{"$it Wh"} ?: "Unknown"})
        if(compatibility.isNotEmpty()) CompareRow("Software",products.map{compatibility[it.id]?.status?.name?.replace('_',' ') ?: "Not checked"})
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
