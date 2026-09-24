package com.jitelecom.productadviser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jitelecom.productadviser.ui.HomeViewModel

@Composable
fun HomeScreen(online: Boolean, onNavigate: (String) -> Unit, onSearch: (String) -> Unit, onProduct: (Long) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val products by viewModel.products.collectAsState(); val software by viewModel.software.collectAsState(); val version by viewModel.databaseVersion.collectAsState(); val popular by viewModel.popular.collectAsState(); val settings by viewModel.settings.collectAsState()
    val favorites=remember(products,settings.favoriteProductIds){settings.favoriteProductIds.mapNotNull{id->products.firstOrNull{it.id==id}}}
    val recent=remember(products,settings.recentProductIds){settings.recentProductIds.mapNotNull{id->products.firstOrNull{it.id==id}}}
    var search by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)){Text("JI PRODUCT ADVISER",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary);Text("What does your customer need?",color=MaterialTheme.colorScheme.onSurfaceVariant)}
            OnlineBadge(online)
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search product, CPU, model…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { IconButton(onClick = { onSearch(search) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Open search") } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(search) })
        )
        Spacer(Modifier.height(20.dp)); Text("Quick actions",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(columns=GridCells.Adaptive(155.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.weight(1f)) {
            item { ActionCard("CAN IT RUN?","Check stored requirements",Icons.Default.CheckCircle){onNavigate("compatibility")} }
            item { ActionCard("RECOMMEND","Match needs and budget",Icons.Default.Star){onNavigate("recommend")} }
            item { ActionCard("FIND PRODUCT","Search local catalog",Icons.Default.Search){onNavigate("products")} }
            item { ActionCard("COMPARE","Compare 2–4 products",Icons.AutoMirrored.Filled.CompareArrows){onNavigate("compare")} }
            item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)}) {
                Card { Column(Modifier.padding(16.dp)) { Text("Local database",fontWeight=FontWeight.Bold);Text("${products.size} products • ${software.size} software records",style=MaterialTheme.typography.bodyMedium);Text("Version ${version ?: "loading…"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);if(popular.isNotEmpty()){Spacer(Modifier.height(8.dp));Text("Frequently recommended: ${popular.take(3).joinToString { it.key }}",style=MaterialTheme.typography.bodySmall)} } }
            }
            if(favorites.isNotEmpty()) item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)}) { ProductShortcutRow("Favorites",favorites,onProduct) }
            if(recent.isNotEmpty()) item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)}) { ProductShortcutRow("Recently viewed",recent,onProduct) }
            item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)}) { NoticeCard("The July 2026 lineup supplies product names, prices, and an inventory snapshot. Technical specifications remain marked for review until verified from official sources.",warning=true) }
        }
    }
}

@Composable private fun ProductShortcutRow(title:String,products:List<com.jitelecom.productadviser.domain.model.ProductSpec>,onProduct:(Long)->Unit){Card{Column(Modifier.fillMaxWidth().padding(14.dp)){Text(title,fontWeight=FontWeight.Bold);Spacer(Modifier.height(7.dp));LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){items(products,key={it.id}){product->AssistChip(onClick={onProduct(product.id)},label={Text(product.displayName,maxLines=1)},leadingIcon={Icon(if(title=="Favorites")Icons.Default.Favorite else Icons.Default.History,null,Modifier.size(16.dp))})}}}}}

@Composable private fun ActionCard(title:String,subtitle:String,icon:ImageVector,onClick:()->Unit){ ElevatedCard(Modifier.fillMaxWidth().height(125.dp).clickable(onClick=onClick)){Column(Modifier.padding(16.dp)){Icon(icon,null,tint=MaterialTheme.colorScheme.secondary);Spacer(Modifier.height(12.dp));Text(title,fontWeight=FontWeight.Bold);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}} }
