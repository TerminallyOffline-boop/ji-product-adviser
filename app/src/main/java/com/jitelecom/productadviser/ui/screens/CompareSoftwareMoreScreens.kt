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
import kotlin.math.roundToInt
import com.jitelecom.productadviser.domain.model.*
import com.jitelecom.productadviser.ui.CompareViewModel
import com.jitelecom.productadviser.ui.SoftwareViewModel

@Composable
fun CompareScreen(onProduct:(Long)->Unit,viewModel:CompareViewModel=hiltViewModel()) {
    val products by viewModel.products.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val chosen=selected.mapNotNull{id->products.firstOrNull{it.id==id}}
    val software by viewModel.software.collectAsState()
    val softwareId by viewModel.selectedSoftware.collectAsState()
    val compatibility by viewModel.compatibility.collectAsState()
    val message by viewModel.message.collectAsState()
    var query by remember{mutableStateOf("")}
    var category by remember{mutableStateOf<ProductCategory?>(null)}
    var differencesOnly by remember{mutableStateOf(true)}
    var showingResults by remember(selected.size>=2){mutableStateOf(false)}
    val categories=remember(products){products.map{it.category}.distinct().sortedBy{it.name}}
    val filtered=remember(products,query,category,selected){products.filter{it.id !in selected&&(category==null||it.category==category)&&(query.isBlank()||it.displayName.contains(query,true)||it.sku.contains(query,true)||it.processor?.displayName?.contains(query,true)==true)}}

    if(showingResults&&chosen.size>=2){
        Column(Modifier.fillMaxSize().padding(horizontal=20.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            ScreenHeader("Your comparison","${chosen.size} ${chosen.first().category.name.lowercase().replace('_',' ')} products")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedButton(onClick={showingResults=false},modifier=Modifier.weight(1f)){Icon(Icons.Default.Edit,null);Spacer(Modifier.width(5.dp));Text("Change devices")}
                OutlinedButton(onClick={viewModel.clear();showingResults=false},modifier=Modifier.weight(1f)){Icon(Icons.Default.DeleteOutline,null);Spacer(Modifier.width(5.dp));Text("Start over")}
            }
            SearchableSelector("Software compatibility (optional)",software,software.firstOrNull{it.id==softwareId},{it.displayName},Modifier.fillMaxWidth()){viewModel.selectedSoftware.value=it?.id}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Show differences only",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge);Switch(differencesOnly,{differencesOnly=it})}
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(bottom=16.dp)){
                item{ComparisonProductStrip(chosen,onProduct)}
                item{NoticeCard("Percentages compare listed capacity, price, weight, and display values. CPU/GPU use stored performance tiers; tier differences are not real-world speed or benchmark claims.")}
                items(compareMetrics(chosen),key={it.label}){metric->if(!differencesOnly||metric.values.map{it.value}.distinct().size>1)ComparisonMetricCard(metric)}
                if(compatibility.isNotEmpty())item{CompatibilityComparisonCard(chosen,compatibility)}
            }
        }
    }else{
        Column(Modifier.fillMaxSize().padding(horizontal=20.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            ScreenHeader("Compare Products","Choose devices, then open the comparison directly")
            if(chosen.isNotEmpty()){
                Text("Selected (${chosen.size}/4)",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){items(chosen,key={it.id}){p->InputChip(selected=true,onClick={viewModel.toggle(p.id)},label={Text(p.displayName,maxLines=1)},trailingIcon={Icon(Icons.Default.Close,null,Modifier.size(16.dp))})}}
            }
            OutlinedTextField(query,{query=it},label={Text("Search products")},leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={if(query.isNotBlank())IconButton(onClick={query=""}){Icon(Icons.Default.Clear,"Clear")}},singleLine=true,modifier=Modifier.fillMaxWidth())
            LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){item{FilterChip(selected=category==null,onClick={category=null},label={Text("All")})};items(categories){type->FilterChip(selected=category==type,onClick={category=type},label={Text(type.name.lowercase().replace('_',' ').replaceFirstChar(Char::uppercase))})}}
            message?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp),contentPadding=PaddingValues(bottom=8.dp)){
                items(filtered,key={it.id}){p->
                    val checked=p.id in selected
                    OutlinedCard(onClick={viewModel.toggle(p.id)},colors=CardDefaults.outlinedCardColors(containerColor=if(checked)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)){
                        Row(Modifier.fillMaxWidth().padding(horizontal=11.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
                            Checkbox(checked=checked,onCheckedChange={viewModel.toggle(p.id)})
                            Column(Modifier.weight(1f)){Text(p.displayName,fontWeight=FontWeight.SemiBold,maxLines=1);Text("${peso(p.effectivePrice)} • ${p.processor?.model?:"CPU not listed"} • ${p.ramGB?.let{"$it GB RAM"}?:"RAM not listed"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)}
                            if(checked)Icon(Icons.Default.CheckCircle,null,tint=MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if(filtered.isEmpty())item{EmptyState(Icons.Default.SearchOff,"No products found","Try another search or category.")}
            }
            Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerHigh)){
                Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Column(Modifier.weight(1f)){Text(if(chosen.size<2)"Select ${2-chosen.size} more" else "${chosen.size} ready to compare",fontWeight=FontWeight.SemiBold);Text("Tap a product to add or remove it",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    if(chosen.isNotEmpty())TextButton(onClick=viewModel::clear){Text("Clear")}
                    Button(onClick={showingResults=true},enabled=chosen.size>=2){Icon(Icons.AutoMirrored.Filled.CompareArrows,null);Spacer(Modifier.width(5.dp));Text("Compare")}
                }
            }
        }
    }
}

@Composable
private fun CompareScreenLegacy(onProduct:(Long)->Unit,viewModel:CompareViewModel=hiltViewModel()){
    val products by viewModel.products.collectAsState();val selected by viewModel.selected.collectAsState();val chosen=selected.mapNotNull{id->products.firstOrNull{it.id==id}}
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
            if(chosen.size>=2)item{Column(verticalArrangement=Arrangement.spacedBy(4.dp)){SearchableSelector("Compatibility for (optional)",software,software.firstOrNull{it.id==softwareId},{it.displayName}){viewModel.selectedSoftware.value=it.id};if(softwareId!=null)TextButton(onClick={viewModel.selectedSoftware.value=null},modifier=Modifier.align(Alignment.End)){Icon(Icons.Default.Clear,null);Spacer(Modifier.width(4.dp));Text("Remove software check")}}}
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
    ) + if(compatibility.isNotEmpty()) listOf("Software" to products.map{product->compatibility[product.id]?.status?.name?.replace('_',' ') ?: "Not checked"}) else emptyList()
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

private data class CompareMetricValue(val productName:String,val value:Double?,val formatted:String)
private data class CompareMetric(val label:String,val values:List<CompareMetricValue>,val higherIsBetter:Boolean,val tierBased:Boolean=false)

private fun compareMetrics(products:List<ProductSpec>):List<CompareMetric> = listOf(
    CompareMetric("CPU tier score",products.map{CompareMetricValue(it.displayName,it.processor?.performanceTier?.toDouble(),it.processor?.performanceTier?.let{"Tier $it / 7"}?:"Not listed")},true,true),
    CompareMetric("Graphics tier score",products.map{CompareMetricValue(it.displayName,it.gpu?.performanceTier?.toDouble(),it.gpu?.performanceTier?.let{"Tier $it / 7"}?:"Not listed")},true,true),
    CompareMetric("RAM",products.map{CompareMetricValue(it.displayName,it.ramGB?.toDouble(),it.ramGB?.let{"$it GB"}?:"Not listed")},true),
    CompareMetric("Storage",products.map{CompareMetricValue(it.displayName,it.storageGB?.toDouble(),it.storageGB?.let{"$it GB"}?:"Not listed")},true),
    CompareMetric("Display refresh",products.map{CompareMetricValue(it.displayName,it.displayRefreshRate?.toDouble(),it.displayRefreshRate?.let{"$it Hz"}?:"Not listed")},true),
    CompareMetric("Battery",products.map{CompareMetricValue(it.displayName,it.batteryCapacityWh,it.batteryCapacityWh?.let{"${it} Wh"}?:"Not listed")},true),
    CompareMetric("Price",products.map{CompareMetricValue(it.displayName,it.effectivePrice,peso(it.effectivePrice))},false),
    CompareMetric("Weight",products.map{CompareMetricValue(it.displayName,it.weightKg,it.weightKg?.let{"${it} kg"}?:"Not listed")},false)
)

internal fun comparisonAdvantagePercent(best:Double,worst:Double,higherIsBetter:Boolean):Int?{
    if(best<=0||worst<=0||best==worst)return null
    val ratio=if(higherIsBetter)(best-worst)/worst else (worst-best)/worst
    return (ratio*100).roundToInt().coerceAtLeast(0)
}

@Composable private fun ComparisonProductStrip(products:List<ProductSpec>,onProduct:(Long)->Unit){
    LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        items(products,key={it.id}){p->ElevatedCard(onClick={onProduct(p.id)},modifier=Modifier.width(220.dp)){Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(p.displayName,fontWeight=FontWeight.Bold,maxLines=2);Text(peso(p.effectivePrice),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary);Text("${p.processor?.model?:"CPU not listed"} • ${p.ramGB?.let{"$it GB RAM"}?:"RAM unknown"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("View full details",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)}}}
    }
}

@Composable private fun ComparisonMetricCard(metric:CompareMetric){
    val known=metric.values.mapNotNull{it.value}
    val best=if(known.isEmpty())null else if(metric.higherIsBetter)known.maxOrNull() else known.minOrNull()
    val worst=if(known.isEmpty())null else if(metric.higherIsBetter)known.minOrNull() else known.maxOrNull()
    ElevatedCard{Column(Modifier.fillMaxWidth().padding(13.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text(metric.label,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            if(best!=null&&worst!=null&&best!=worst){
                if(metric.tierBased){
                    val spread=(best-worst).roundToInt()
                    Text("$spread tier${if(spread==1)"" else "s"} apart",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
                }else{
                    val percent=comparisonAdvantagePercent(best,worst,metric.higherIsBetter)
                    Text("$percent% ${if(metric.higherIsBetter)"difference" else "lower"}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
                }
            }
        }
        metric.values.forEach{entry->
            val isBest=entry.value!=null&&entry.value==best
            val isWorst=entry.value!=null&&entry.value==worst
            val advantage=if(isBest&&!metric.tierBased)comparisonAdvantagePercent(requireNotNull(best),requireNotNull(worst),metric.higherIsBetter)else null
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text(entry.productName,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,maxLines=1)
                Text(entry.formatted,style=MaterialTheme.typography.bodyMedium,fontWeight=if(isBest)FontWeight.Bold else FontWeight.Normal)
                Spacer(Modifier.width(8.dp))
                Text(when{entry.value==null->"";best==worst->"Tied";isBest&&metric.tierBased->"Top tier";isBest->"+${advantage}%";isWorst->"Lowest";else->""},style=MaterialTheme.typography.labelSmall,color=if(isBest)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }}
}

@Composable private fun CompatibilityComparisonCard(products:List<ProductSpec>,compatibility:Map<Long,CompatibilityResult>){
    ElevatedCard{Column(Modifier.fillMaxWidth().padding(13.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
        Text("Selected software compatibility",fontWeight=FontWeight.Bold)
        products.forEach{p->val status=compatibility[p.id]?.status?.name?.replace('_',' ')?:"NOT CHECKED";Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(p.displayName,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall);Text(status,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}}
    }}
}

@Composable
fun SoftwareScreen(viewModel:SoftwareViewModel=hiltViewModel()){
    val software by viewModel.software.collectAsState();var query by remember{mutableStateOf("")};var category by remember{mutableStateOf<String?>(null)}
    val categories=remember(software){software.map{it.category}.distinct().sorted()}
    val filtered=remember(software,query,category){software.filter{app->(category==null||app.category==category)&&(query.isBlank()||app.displayName.contains(query,true)||app.developer?.contains(query,true)==true||app.platform.contains(query,true))}}
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        item{ScreenHeader("Software & Apps","Version-aware local requirements catalog")}
        item{OutlinedTextField(query,{query=it},label={Text("Search software, developer or platform")},leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={if(query.isNotBlank())IconButton(onClick={query=""}){Icon(Icons.Default.Clear,"Clear search")}},singleLine=true,modifier=Modifier.fillMaxWidth())}
        item{LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){item{FilterChip(selected=category==null,onClick={category=null},label={Text("All")})};items(categories){value->FilterChip(selected=category==value,onClick={category=value},label={Text(value)})}}}
        item{Text("${filtered.size} app${if(filtered.size==1)"" else "s"}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(filtered.isEmpty())item{EmptyState(Icons.Default.SearchOff,"No software found","Try another search or category.")}
        items(filtered,key={it.id}){app->
            OutlinedCard{Column(Modifier.padding(15.dp)){
                Row{Column(Modifier.weight(1f)){Text(app.displayName,fontWeight=FontWeight.Bold);Text("${app.category} • ${app.platform}",style=MaterialTheme.typography.bodySmall)}}
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

