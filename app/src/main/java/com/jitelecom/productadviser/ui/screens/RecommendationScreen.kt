package com.jitelecom.productadviser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jitelecom.productadviser.domain.model.RecommendationResult
import com.jitelecom.productadviser.ui.RecommendationViewModel

private val profiles=listOf("Student","Teacher","Office Worker","Business Owner","Engineering Student","Architecture Student","Programmer","Graphic Designer","Video Editor","Gamer","Content Creator")

@Composable
fun RecommendationScreen(onProduct:(Long)->Unit,viewModel:RecommendationViewModel=hiltViewModel()){
    val budget by viewModel.budget.collectAsState();val profile by viewModel.profile.collectAsState();val category by viewModel.category.collectAsState();val software by viewModel.applicableSoftware.collectAsState();val selected by viewModel.selectedSoftware.collectAsState();val priorities by viewModel.priorities.collectAsState();val brand by viewModel.brand.collectAsState();val above by viewModel.aboveBudget.collectAsState();val results by viewModel.results.collectAsState();val message by viewModel.message.collectAsState();var query by remember{mutableStateOf("")}
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{ScreenHeader("Recommend a Gadget","Deterministic scoring against the local catalog")}
        item{OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),label={Text("Optional quick request")},placeholder={Text("30k laptop for architecture AutoCAD")},trailingIcon={IconButton(onClick={viewModel.parse(query)}){Icon(Icons.Default.AutoAwesome,"Parse locally")}},supportingText={Text("Local keyword helper only; it never decides compatibility.")})}
        item{Selector("Customer profile",profiles,profile,{it}){viewModel.profile.value=it}}
        item{Text("Product type",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){items(listOf(com.jitelecom.productadviser.domain.model.ProductCategory.LAPTOP,com.jitelecom.productadviser.domain.model.ProductCategory.SMARTPHONE,com.jitelecom.productadviser.domain.model.ProductCategory.TABLET)){type->FilterChip(selected=type==category,onClick={viewModel.setCategory(type)},label={Text(type.name.lowercase().replaceFirstChar(Char::uppercase))})}}}
        item{OutlinedTextField(value=budget,onValueChange={viewModel.budget.value=it.filter{c->c.isDigit()||c=='.'}},label={Text("Absolute maximum budget (₱)")},modifier=Modifier.fillMaxWidth(),singleLine=true)}
        item{OutlinedTextField(value=brand,onValueChange={viewModel.brand.value=it},label={Text("Preferred brand (optional)")},modifier=Modifier.fillMaxWidth(),singleLine=true)}
        item{Row(Modifier.fillMaxWidth()){Text("Show up to 10% above budget",Modifier.weight(1f));Switch(checked=above,onCheckedChange={viewModel.aboveBudget.value=it})}}
        item{Text("Required software",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){items(software,key={it.id}){app->FilterChip(selected=app.id in selected,onClick={viewModel.toggleSoftware(app.id)},label={Text(app.name)})}}}
        item{Text("Priorities",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){items(listOf("Performance","Battery","Portability","RAM / storage")){priority->FilterChip(selected=priority in priorities,onClick={viewModel.priorities.value=if(priority in priorities)priorities-priority else priorities+priority},label={Text(priority)})}}}
        item{Button(onClick=viewModel::recommend,modifier=Modifier.fillMaxWidth()){Text("FIND BEST MATCHES")}}
        message?.let { value -> item { Text(value,color=if(results.isEmpty())MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold) } }
        if(results.isNotEmpty()){item{Text("Best matches",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)};itemsIndexed(results,key={_,r->r.product.id}){index,result->RecommendationCard(index+1,result){onProduct(result.product.id)}}}
        item{NoticeCard("The score is an internal relative match, not a probability or performance guarantee. Products above the absolute budget are excluded unless explicitly enabled.")}
        item{Spacer(Modifier.height(20.dp))}
    }
}

@Composable private fun RecommendationCard(rank:Int,result:RecommendationResult,onClick:()->Unit){ElevatedCard(onClick=onClick){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Row{Text("$rank. ${result.product.displayName}",Modifier.weight(1f),fontWeight=FontWeight.Bold);Text("${result.internalScore}/100",fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)};Text(peso(result.product.effectivePrice),style=MaterialTheme.typography.titleMedium);Text(result.explanation,style=MaterialTheme.typography.bodySmall);if(result.compatibility.isNotEmpty())result.compatibility.values.forEach{StatusBadge(it.status)};Text("Strengths: ${result.strengths.joinToString()}",style=MaterialTheme.typography.bodySmall);if(result.limitations.isNotEmpty())Text("Limitations: ${result.limitations.joinToString()}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)}}}
