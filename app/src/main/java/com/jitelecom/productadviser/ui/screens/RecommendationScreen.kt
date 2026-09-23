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
import com.jitelecom.productadviser.domain.model.RecommendationResult
import com.jitelecom.productadviser.ui.RecommendationViewModel

private val profiles=listOf("Student","Teacher","Office Worker","Business Owner","Engineering Student","Architecture Student","Programmer","Graphic Designer","Video Editor","Gamer","Content Creator")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendationScreen(onProduct:(Long)->Unit,viewModel:RecommendationViewModel=hiltViewModel()){
    val budget by viewModel.budget.collectAsState();val profile by viewModel.profile.collectAsState();val category by viewModel.category.collectAsState();val software by viewModel.applicableSoftware.collectAsState();val selected by viewModel.selectedSoftware.collectAsState();val priorities by viewModel.priorities.collectAsState();val brand by viewModel.brand.collectAsState();val above by viewModel.aboveBudget.collectAsState();val results by viewModel.results.collectAsState();val message by viewModel.message.collectAsState();val settings by viewModel.settings.collectAsState();val products by viewModel.products.collectAsState();var query by remember{mutableStateOf("")};var softwareQuery by remember{mutableStateOf("")}
    val brands=remember(products,category){products.filter{it.category==category}.map{it.brand}.distinct().sorted()}
    val filteredSoftware=remember(software,softwareQuery){software.filter{it.displayName.contains(softwareQuery,true)||it.category.contains(softwareQuery,true)}}
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{ScreenHeader("Recommend a Gadget","Deterministic scoring against the local catalog")}
        item{OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),label={Text("Optional quick request")},placeholder={Text("30k laptop for architecture AutoCAD")},trailingIcon={IconButton(onClick={viewModel.parse(query)}){Icon(Icons.Default.AutoAwesome,"Parse locally")}},supportingText={Text("Local keyword helper only; it never decides compatibility.")})}
        item{Selector("Customer profile",profiles,profile,{it}){viewModel.profile.value=it}}
        item{Text("Product type",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){items(listOf(com.jitelecom.productadviser.domain.model.ProductCategory.LAPTOP,com.jitelecom.productadviser.domain.model.ProductCategory.SMARTPHONE,com.jitelecom.productadviser.domain.model.ProductCategory.TABLET)){type->FilterChip(selected=type==category,onClick={viewModel.setCategory(type)},label={Text(type.name.lowercase().replaceFirstChar(Char::uppercase))})}}}
        item{OutlinedTextField(value=budget,onValueChange={viewModel.budget.value=it.filter{c->c.isDigit()||c=='.'}},label={Text("Absolute maximum budget (₱)")},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))}
        item{OptionalSelector("Preferred brand",brands,brand.takeIf{it.isNotBlank()},{it},noneText="Any brand"){viewModel.brand.value=it.orEmpty()}}
        item{Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Allow above-budget alternatives");Text("Up to ${settings.allowAboveBudgetPercent}% above the maximum",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=above,onCheckedChange={viewModel.aboveBudget.value=it})}}
        item{Text("Required software",fontWeight=FontWeight.Bold);OutlinedTextField(softwareQuery,{softwareQuery=it},label={Text("Search apps and games")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(7.dp));if(filteredSoftware.isEmpty())Text("No matching apps",style=MaterialTheme.typography.bodySmall)else FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){filteredSoftware.forEach{app->FilterChip(selected=app.id in selected,onClick={viewModel.toggleSoftware(app.id)},label={Text(app.name)})}}}
        item{Text("Priorities",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){items(listOf("Performance","Battery","Portability","RAM / storage")){priority->FilterChip(selected=priority in priorities,onClick={viewModel.priorities.value=if(priority in priorities)priorities-priority else priorities+priority},label={Text(priority)})}}}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=viewModel::reset,modifier=Modifier.weight(1f)){Icon(Icons.Default.Refresh,null);Spacer(Modifier.width(6.dp));Text("RESET")};Button(onClick=viewModel::recommend,modifier=Modifier.weight(2f)){Text("FIND BEST MATCHES")}}}
        message?.let { value -> item { Text(value,color=if(results.isEmpty())MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold) } }
        if(results.isNotEmpty()){item{Text("Best matches",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)};itemsIndexed(results,key={_,r->r.product.id}){index,result->RecommendationCard(index+1,result,software.associateBy{it.id}){onProduct(result.product.id)}};item{OutlinedButton(onClick=viewModel::reset,modifier=Modifier.fillMaxWidth()){Text("START ANOTHER CUSTOMER")}}}
        item{NoticeCard("The score is an internal relative match, not a probability or performance guarantee. Products above the absolute budget are excluded unless explicitly enabled.")}
        item{Spacer(Modifier.height(20.dp))}
    }
}

@Composable private fun RecommendationCard(rank:Int,result:RecommendationResult,software:Map<Long,com.jitelecom.productadviser.domain.model.SoftwareSpec>,onClick:()->Unit){ElevatedCard(onClick=onClick){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Row{Text("$rank. ${result.product.displayName}",Modifier.weight(1f),fontWeight=FontWeight.Bold);Text("${result.internalScore}/100",fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)};Text(peso(result.product.effectivePrice),style=MaterialTheme.typography.titleMedium);Text(result.explanation,style=MaterialTheme.typography.bodySmall);result.compatibility.forEach{(id,compatibility)->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text(software[id]?.name?:"Selected app",Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.SemiBold);StatusBadge(compatibility.status)}};Text("Strengths: ${result.strengths.joinToString()}",style=MaterialTheme.typography.bodySmall);if(result.limitations.isNotEmpty())Text("Limitations: ${result.limitations.joinToString()}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)}}}
