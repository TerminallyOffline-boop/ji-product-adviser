package com.jitelecom.productadviser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jitelecom.productadviser.domain.model.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold); subtitle?.let { Text(it, style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onSurfaceVariant) } }
        action?.invoke()
    }
}

@Composable
fun OnlineBadge(online: Boolean) {
    val color = if (online) Color(0xFF167A47) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color=color.copy(alpha=.12f), contentColor=color, shape=RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal=10.dp, vertical=5.dp), verticalAlignment=Alignment.CenterVertically) {
            Icon(if(online) Icons.Default.Wifi else Icons.Default.WifiOff, null, Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text(if(online) "Online" else "Offline mode", style=MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun VerificationBadge(status: VerificationStatus) {
    val (label,color)=when(status){
        VerificationStatus.VERIFIED -> "Verified" to Color(0xFF167A47)
        VerificationStatus.NEEDS_REVIEW -> "Needs review" to Color(0xFFB36800)
        VerificationStatus.UNVERIFIED -> "Unverified" to Color(0xFF616675)
        VerificationStatus.OUTDATED -> "Outdated" to Color(0xFFB3261E)
    }
    Surface(color=color.copy(alpha=.12f), contentColor=color, shape=RoundedCornerShape(50)) { Text(label, Modifier.padding(horizontal=9.dp,vertical=4.dp), style=MaterialTheme.typography.labelSmall) }
}

@Composable
fun StatusBadge(status: CompatibilityStatus) {
    val (label,color)=when(status){
        CompatibilityStatus.MEETS_RECOMMENDED -> "MEETS RECOMMENDED" to Color(0xFF167A47)
        CompatibilityStatus.MEETS_MINIMUM -> "MEETS MINIMUM" to Color(0xFFB36800)
        CompatibilityStatus.BELOW_MINIMUM -> "BELOW MINIMUM" to Color(0xFFB3261E)
        CompatibilityStatus.NOT_AVAILABLE -> "NOT AVAILABLE" to Color(0xFF7A3E9D)
        CompatibilityStatus.NOT_VERIFIED -> "NOT VERIFIED" to Color(0xFF616675)
    }
    Surface(color=color.copy(alpha=.14f), contentColor=color, shape=RoundedCornerShape(50)) { Text(label, Modifier.padding(horizontal=12.dp,vertical=7.dp), style=MaterialTheme.typography.labelMedium, fontWeight=FontWeight.Bold) }
}

@Composable
fun ProductCard(product: ProductSpec, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier.fillMaxWidth().clickable(onClick=onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment=Alignment.CenterVertically) {
            Surface(Modifier.size(54.dp), shape=RoundedCornerShape(14.dp), color=MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment=Alignment.Center) { Icon(Icons.Default.Laptop, null, tint=MaterialTheme.colorScheme.onPrimaryContainer) } }
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) {
                Text(product.displayName, fontWeight=FontWeight.SemiBold, maxLines=1, overflow=TextOverflow.Ellipsis)
                Text(listOfNotNull(product.processor?.model, product.ramGB?.let{"${it} GB"}, product.storageGB?.let{"${it} GB ${product.storageType.orEmpty()}"}).joinToString(" • "), style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant, maxLines=2)
                Spacer(Modifier.height(5.dp)); VerificationBadge(product.verificationStatus)
            }
            Column(horizontalAlignment=Alignment.End) { Text(peso(product.effectivePrice), fontWeight=FontWeight.Bold); Icon(Icons.Default.ChevronRight, null) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Selector(label: String, items: List<T>, selected: T?, itemText: (T) -> String, modifier: Modifier = Modifier, onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded=expanded, onExpandedChange={expanded=it}, modifier=modifier) {
        OutlinedTextField(value=selected?.let(itemText).orEmpty(), onValueChange={}, readOnly=true, label={Text(label)}, trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)}, modifier=Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
        ExposedDropdownMenu(expanded=expanded, onDismissRequest={expanded=false}) { items.forEach { item -> DropdownMenuItem(text={Text(itemText(item))}, onClick={onSelected(item);expanded=false}) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OptionalSelector(label: String, items: List<T>, selected: T?, itemText: (T) -> String, modifier: Modifier = Modifier, noneText: String = "All", onSelected: (T?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=it},modifier=modifier) {
        OutlinedTextField(value=selected?.let(itemText) ?: noneText,onValueChange={},readOnly=true,label={Text(label)},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)},modifier=Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
        ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            DropdownMenuItem(text={Text(noneText)},onClick={onSelected(null);expanded=false})
            items.forEach { item -> DropdownMenuItem(text={Text(itemText(item))},onClick={onSelected(item);expanded=false}) }
        }
    }
}

@Composable
fun <T> SearchableSelector(label: String, items: List<T>, selected: T?, itemText: (T) -> String, modifier: Modifier = Modifier, onSelected: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    OutlinedCard(onClick={open=true},modifier=modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)){Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(selected?.let(itemText) ?: "Tap to choose",maxLines=1,overflow=TextOverflow.Ellipsis)}
            Icon(Icons.Default.ArrowDropDown,null)
        }
    }
    if(open) AlertDialog(
        onDismissRequest={open=false;query=""},
        title={Text(label)},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(query,{query=it},singleLine=true,label={Text("Search")},leadingIcon={Icon(Icons.Default.Search,null)},modifier=Modifier.fillMaxWidth())
            val filtered=remember(items,query){items.filter{itemText(it).contains(query,true)}}
            if(filtered.isEmpty()) EmptyState(Icons.Default.SearchOff,"No matches","Try a shorter search.") else LazyColumn(Modifier.heightIn(max=430.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){items(filtered){item->TextButton(onClick={onSelected(item);open=false;query=""},modifier=Modifier.fillMaxWidth()){Text(itemText(item),Modifier.fillMaxWidth())}}}
        }},
        confirmButton={},dismissButton={TextButton(onClick={open=false;query=""}){Text("Cancel")}}
    )
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment=Alignment.CenterHorizontally) { Icon(icon,null,Modifier.size(48.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));Text(title,fontWeight=FontWeight.Bold);Text(body,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
}

fun peso(value: Double): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH")).format(value)

@Composable
fun NoticeCard(text: String, warning: Boolean = false) {
    val color=if(warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Card(colors=CardDefaults.cardColors(containerColor=color.copy(alpha=.08f)), border=BorderStroke(1.dp,color.copy(alpha=.25f))) { Row(Modifier.padding(14.dp)){Icon(if(warning)Icons.Default.Warning else Icons.Default.Info,null,tint=color);Spacer(Modifier.width(10.dp));Text(text,style=MaterialTheme.typography.bodySmall)} }
}
