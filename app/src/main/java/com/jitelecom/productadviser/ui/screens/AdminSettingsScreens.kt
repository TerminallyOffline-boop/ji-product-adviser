package com.jitelecom.productadviser.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jitelecom.productadviser.data.local.*
import com.jitelecom.productadviser.data.repository.toDomain
import com.jitelecom.productadviser.domain.model.*
import com.jitelecom.productadviser.ui.*
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(viewModel:SettingsViewModel=hiltViewModel()){
    val settings by viewModel.settings.collectAsState();val message by viewModel.message.collectAsState();var remote by remember(settings.remoteDatabaseUrl){mutableStateOf(settings.remoteDatabaseUrl)}
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{ScreenHeader("Settings","Local preferences")}
        item{Card{Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Dark mode",fontWeight=FontWeight.Bold);Text("Stored only on this device",style=MaterialTheme.typography.bodySmall)};Switch(settings.darkMode,viewModel::dark)}}}
        item{Card{Column(Modifier.padding(16.dp)){Text("Remote database manifest",fontWeight=FontWeight.Bold);Text("Optional. The app remains fully usable without it.",style=MaterialTheme.typography.bodySmall);Spacer(Modifier.height(8.dp));OutlinedTextField(value=remote,onValueChange={remote=it},label={Text("HTTPS manifest URL")},modifier=Modifier.fillMaxWidth(),singleLine=true);Button(onClick={viewModel.remoteUrl(remote)},modifier=Modifier.align(Alignment.End)){Text("Save")}}}}
        message?.let{item{Text(it,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.bodySmall)}}
        item{Card{Column(Modifier.padding(16.dp)){Text("Above-budget allowance",fontWeight=FontWeight.Bold);Text("${settings.allowAboveBudgetPercent}% when the employee explicitly enables it",style=MaterialTheme.typography.bodySmall);Slider(value=settings.allowAboveBudgetPercent.toFloat(),onValueChange={viewModel.aboveBudget(it.toInt())},valueRange=0f..25f,steps=24)}}}
        item{NoticeCard("No API keys or sensitive credentials are stored or exported. Admin PIN protection is local convenience protection, not enterprise-grade security.")}
    }
}

@Composable
fun DatabaseInfoScreen(online:Boolean,onAdmin:()->Unit,viewModel:HomeViewModel=hiltViewModel()){
    val products by viewModel.products.collectAsState();val software by viewModel.software.collectAsState();val version by viewModel.databaseVersion.collectAsState()
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){ScreenHeader("Database","Local source of truth"){OnlineBadge(online)};Card{Column(Modifier.padding(18.dp)){InfoRow("Version",version?:"Loading…");InfoRow("Products",products.size.toString());InfoRow("Software",software.size.toString());InfoRow("Operation","Offline-first")}};NoticeCard("Remote checks are optional. Updates are validated, backed up, and committed in one transaction; failures roll back without replacing the current catalog.");Button(onClick=onAdmin,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.AdminPanelSettings,null);Spacer(Modifier.width(8.dp));Text("OPEN ADMIN / DATABASE")};if(!online)NoticeCard("Remote updates and official links are unavailable. Search, compatibility, recommendations, comparison and editing still work.")}
}

@Composable
fun AdminScreen(online:Boolean,viewModel:AdminViewModel=hiltViewModel()){
    val state by viewModel.state.collectAsState()
    val settings by viewModel.settings.collectAsState()
    if(!settings.adminPinConfigured){AdminPinSetup(state.message,viewModel::configurePin);return}
    if(!state.unlocked){AdminUnlock(state.message,viewModel::unlock);return}
    val products by viewModel.productsFlow.collectAsState()
    val processors by viewModel.processors.collectAsState()
    val gpus by viewModel.gpus.collectAsState()
    val software by viewModel.software.collectAsState()
    val requirements by viewModel.requirements.collectAsState()
    val qualityIssues by viewModel.qualityIssues.collectAsState()
    var tab by remember{mutableIntStateOf(0)}
    var showProduct by remember{mutableStateOf<ProductSpec?>(null)}
    var processorEditor by remember{mutableStateOf<ProcessorEntity?>(null)}
    var gpuEditor by remember{mutableStateOf<GpuEntity?>(null)}
    var softwareEditor by remember{mutableStateOf<SoftwareEntity?>(null)}
    var requirementManagerFor by remember{mutableStateOf<SoftwareEntity?>(null)}
    var requirementEditorFor by remember{mutableStateOf<SoftwareEntity?>(null)}
    var requirementEditor by remember{mutableStateOf<RequirementEntity?>(null)}
    var archiveCandidate by remember{mutableStateOf<ProductSpec?>(null)}
    var deleteProcessor by remember{mutableStateOf<ProcessorEntity?>(null)}
    var deleteGpu by remember{mutableStateOf<GpuEntity?>(null)}
    var deleteSoftware by remember{mutableStateOf<SoftwareEntity?>(null)}
    var deleteRequirement by remember{mutableStateOf<RequirementEntity?>(null)}
    Column(Modifier.fillMaxSize().padding(16.dp)){
        ScreenHeader("Admin / Database","Complete and verify catalog data"){
            IconButton(onClick=viewModel::lock){Icon(Icons.Default.Lock,"Lock")}
        }
        state.message?.let{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(it,Modifier.weight(1f),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.bodySmall);if(state.undoArchiveId!=null)TextButton(onClick=viewModel::restoreArchived){Text("UNDO")}}}
        if(state.isBusy)LinearProgressIndicator(Modifier.fillMaxWidth())
        ScrollableTabRow(selectedTabIndex=tab,edgePadding=0.dp){
            listOf("Products","Hardware","Software","Data quality","Import & backup").forEachIndexed{i,label->
                Tab(selected=tab==i,onClick={tab=i},text={Text(if(label=="Data quality"&&qualityIssues.isNotEmpty())"$label (${qualityIssues.size})" else label)})
            }
        }
        Spacer(Modifier.height(10.dp))
        when(tab){
            0->AdminProducts(products,{showProduct=emptyProduct()},{showProduct=it},{id->archiveCandidate=products.firstOrNull{it.id==id}})
            1->AdminHardware(processors,gpus,{processorEditor=ProcessorEntity(manufacturer="",family="",model="")},{gpuEditor=GpuEntity(manufacturer="",model="",type=GpuType.INTEGRATED)},{processorEditor=it},{gpuEditor=it},{deleteProcessor=it},{deleteGpu=it})
            2->AdminSoftware(software,requirements,{softwareEditor=SoftwareEntity(name="",version="",category="Productivity",platform="Windows")},{softwareEditor=it},{deleteSoftware=it}){requirementManagerFor=it}
            3->DataQualityReport(qualityIssues)
            else->ImportBackupPane(online,state,viewModel)
        }
    }
    showProduct?.let{ProductEditor(it,processors,gpus,onDismiss={showProduct=null},onSave={viewModel.saveProduct(it);showProduct=null})}
    processorEditor?.let{initial->ProcessorEditor(initial,{processorEditor=null}){viewModel.saveProcessor(it);processorEditor=null}}
    gpuEditor?.let{initial->GpuEditor(initial,{gpuEditor=null}){viewModel.saveGpu(it);gpuEditor=null}}
    softwareEditor?.let{initial->SoftwareEditor(initial,{softwareEditor=null}){viewModel.saveSoftware(it);softwareEditor=null}}
    requirementManagerFor?.let{app->RequirementManager(app,requirements.filter{it.softwareId==app.id},{requirementManagerFor=null},{initial->requirementManagerFor=null;requirementEditorFor=app;requirementEditor=initial},{requirementManagerFor=null;deleteRequirement=it})}
    requirementEditorFor?.let{app->requirementEditor?.let{initial->RequirementEditor(app,initial,processors,gpus,{requirementEditorFor=null;requirementEditor=null}){viewModel.saveRequirement(it);requirementEditorFor=null;requirementEditor=null}}}
    archiveCandidate?.let{product->AlertDialog(onDismissRequest={archiveCandidate=null},title={Text("Archive product?")},text={Text("${product.displayName} will disappear from staff search and recommendations. You can undo immediately after archiving.")},confirmButton={Button(onClick={viewModel.archiveProduct(product.id);archiveCandidate=null}){Text("Archive")}},dismissButton={TextButton(onClick={archiveCandidate=null}){Text("Cancel")}})}
    deleteProcessor?.let{value->DeleteDialog("Delete processor?","Linked products will keep their other details but show an unspecified processor.",{deleteProcessor=null}){viewModel.deleteProcessor(value);deleteProcessor=null}}
    deleteGpu?.let{value->DeleteDialog("Delete GPU?","Linked products will keep their other details but show an unspecified GPU.",{deleteGpu=null}){viewModel.deleteGpu(value);deleteGpu=null}}
    deleteSoftware?.let{value->DeleteDialog("Delete software?","${value.name} and its stored requirements will be removed.",{deleteSoftware=null}){viewModel.deleteSoftware(value);deleteSoftware=null}}
    deleteRequirement?.let{value->DeleteDialog("Delete requirement?","This ${value.type.name.lowercase()} rule will be removed.",{deleteRequirement=null}){viewModel.deleteRequirement(value);deleteRequirement=null}}
}

@Composable private fun AdminPinSetup(message:String?,configure:(String)->Unit){var pin by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Card(Modifier.widthIn(max=440.dp).padding(20.dp)){Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Default.EnhancedEncryption,null,Modifier.size(48.dp));Text("Create admin PIN",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Set a device-specific PIN before editing catalog data.",style=MaterialTheme.typography.bodySmall);OutlinedTextField(pin,{pin=it.filter(Char::isDigit).take(12)},label={Text("New PIN (4–12 digits)")},visualTransformation=PasswordVisualTransformation(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword));OutlinedTextField(confirm,{confirm=it.filter(Char::isDigit).take(12)},label={Text("Confirm PIN")},visualTransformation=PasswordVisualTransformation(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword));if(confirm.isNotBlank()&&pin!=confirm)Text("PINs do not match.",color=MaterialTheme.colorScheme.error);message?.let{Text(it,color=MaterialTheme.colorScheme.error)};Button(onClick={configure(pin)},enabled=pin.length>=4&&pin==confirm,modifier=Modifier.fillMaxWidth()){Text("SAVE PIN & CONTINUE")}}}}}
@Composable private fun AdminUnlock(message:String?,unlock:(String)->Unit){var pin by remember{mutableStateOf("")};Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Card(Modifier.widthIn(max=420.dp).padding(20.dp)){Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.AdminPanelSettings,null,Modifier.size(48.dp));Text("Admin Mode",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Local protection — not enterprise security",style=MaterialTheme.typography.bodySmall);Spacer(Modifier.height(12.dp));OutlinedTextField(pin,{pin=it.filter(Char::isDigit).take(12)},label={Text("PIN")},visualTransformation=PasswordVisualTransformation(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword));message?.let{Text(it,color=MaterialTheme.colorScheme.error)};Spacer(Modifier.height(10.dp));Button({unlock(pin)},Modifier.fillMaxWidth(),enabled=pin.length>=4){Text("UNLOCK")}}}}}

@Composable private fun AdminProducts(products:List<ProductSpec>,add:()->Unit,edit:(ProductSpec)->Unit,archive:(Long)->Unit){
    var query by remember{mutableStateOf("")}
    val visible=products.filter{query.isBlank()||listOf(it.sku,it.brand,it.model,it.category.name).any{value->value.contains(query,true)}}
    Column{
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
            OutlinedTextField(query,{query=it},label={Text("Search products")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,modifier=Modifier.weight(1f))
            FilledIconButton(add){Icon(Icons.Default.Add,"Add product")}
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(7.dp)){
            items(visible,key={it.id}){p->OutlinedCard{Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(p.displayName,fontWeight=FontWeight.Bold);Text("${p.sku} • ${p.category.name.replace('_',' ')}",style=MaterialTheme.typography.bodySmall);VerificationLabel(p.verificationStatus)};IconButton({edit(p)}){Icon(Icons.Default.Edit,"Edit")};IconButton({archive(p.id)}){Icon(Icons.Default.Archive,"Archive")}}}}
        }
    }
}

@Composable private fun AdminHardware(processors:List<ProcessorEntity>,gpus:List<GpuEntity>,addCpu:()->Unit,addGpu:()->Unit,editCpu:(ProcessorEntity)->Unit,editGpu:(GpuEntity)->Unit,deleteCpu:(ProcessorEntity)->Unit,deleteGpu:(GpuEntity)->Unit){
    var query by remember{mutableStateOf("")}
    val cpuResults=processors.filter{"${it.manufacturer} ${it.family} ${it.model}".contains(query,true)}
    val gpuResults=gpus.filter{"${it.manufacturer} ${it.model}".contains(query,true)}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(7.dp)){
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(addCpu,Modifier.weight(1f)){Icon(Icons.Default.Add,null);Text(" CPU")};Button(addGpu,Modifier.weight(1f)){Icon(Icons.Default.Add,null);Text(" GPU")}}}
        item{OutlinedTextField(query,{query=it},label={Text("Search hardware")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,modifier=Modifier.fillMaxWidth())}
        item{Text("Processors (${cpuResults.size})",fontWeight=FontWeight.Bold)}
        items(cpuResults,key={it.id}){value->OutlinedCard{Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("${value.manufacturer} ${value.model}",fontWeight=FontWeight.SemiBold);Text("Tier ${value.performanceTier?:"?"} • ${value.architecture?:"architecture missing"}",style=MaterialTheme.typography.bodySmall)};IconButton({editCpu(value)}){Icon(Icons.Default.Edit,"Edit processor")};IconButton({deleteCpu(value)}){Icon(Icons.Default.Delete,"Delete processor")}}}}
        item{Text("Graphics (${gpuResults.size})",fontWeight=FontWeight.Bold)}
        items(gpuResults,key={it.id}){value->OutlinedCard{Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("${value.manufacturer} ${value.model}",fontWeight=FontWeight.SemiBold);Text("Tier ${value.performanceTier?:"?"} • ${value.vramGB?.let{"$it GB VRAM"}?:value.type.name.lowercase()}",style=MaterialTheme.typography.bodySmall)};IconButton({editGpu(value)}){Icon(Icons.Default.Edit,"Edit GPU")};IconButton({deleteGpu(value)}){Icon(Icons.Default.Delete,"Delete GPU")}}}}
    }
}

@Composable private fun AdminSoftware(software:List<SoftwareEntity>,requirements:List<RequirementEntity>,add:()->Unit,edit:(SoftwareEntity)->Unit,delete:(SoftwareEntity)->Unit,manageRequirements:(SoftwareEntity)->Unit){
    var query by remember{mutableStateOf("")}
    val visible=software.filter{"${it.name} ${it.version} ${it.category} ${it.platform}".contains(query,true)}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(7.dp)){
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(query,{query=it},label={Text("Search apps and games")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,modifier=Modifier.weight(1f));FilledIconButton(add){Icon(Icons.Default.Add,"Add software")}}}
        items(visible,key={it.id}){s->
            val count=requirements.count{it.softwareId==s.id}
            OutlinedCard{Column(Modifier.padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("${s.name} ${s.version}",fontWeight=FontWeight.Bold);Text("${s.category} • ${s.platform}",style=MaterialTheme.typography.bodySmall);VerificationLabel(s.verificationStatus)};IconButton({edit(s)}){Icon(Icons.Default.Edit,"Edit software")};IconButton({delete(s)}){Icon(Icons.Default.Delete,"Delete software")}};TextButton(onClick={manageRequirements(s)}){Icon(Icons.Default.Rule,null);Spacer(Modifier.width(6.dp));Text("Manage requirements ($count)")}}}
        }
    }
}

@Composable private fun VerificationLabel(status:VerificationStatus){
    val color=when(status){VerificationStatus.VERIFIED->MaterialTheme.colorScheme.primary;VerificationStatus.OUTDATED->MaterialTheme.colorScheme.error;else->MaterialTheme.colorScheme.tertiary}
    Text(status.name.replace('_',' '),style=MaterialTheme.typography.labelSmall,color=color,fontWeight=FontWeight.Bold)
}

@Composable private fun DataQualityReport(issues:List<DataQualityIssue>){
    val errors=issues.count{it.severity==DataQualitySeverity.ERROR}
    val warnings=issues.size-errors
    LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{Card(colors=CardDefaults.cardColors(containerColor=if(errors==0)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)){Column(Modifier.fillMaxWidth().padding(16.dp)){Text(if(issues.isEmpty())"Catalog checks passed" else "$errors blocking issue${if(errors==1)"" else "s"} • $warnings warning${if(warnings==1)"" else "s"}",fontWeight=FontWeight.Bold);Text(if(issues.isEmpty())"No obvious missing comparison data was found." else "Fix blocking issues first. Warnings identify records that can still produce vague Can It Run results.",style=MaterialTheme.typography.bodySmall)}}}
        if(issues.isEmpty())item{NoticeCard("The automatic report checks required fields, verification sources, platform rules, tier ranges, and minimum/recommended consistency.")}
        items(issues,key={"${it.area}-${it.recordId}-${it.title}"}){issue->
            OutlinedCard{Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.Top){Icon(if(issue.severity==DataQualitySeverity.ERROR)Icons.Default.Error else Icons.Default.Warning,null,tint=if(issue.severity==DataQualitySeverity.ERROR)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary);Spacer(Modifier.width(10.dp));Column{Text(issue.title,fontWeight=FontWeight.Bold);Text(issue.area,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(issue.detail,style=MaterialTheme.typography.bodySmall)}}}
        }
    }
}

@Composable private fun RequirementManager(software:SoftwareEntity,requirements:List<RequirementEntity>,onDismiss:()->Unit,onEdit:(RequirementEntity)->Unit,onDelete:(RequirementEntity)->Unit){
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("${software.name} requirements")},
        text={Column(Modifier.heightIn(max=520.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("Store separate minimum and recommended rules for each platform when requirements differ.",style=MaterialTheme.typography.bodySmall)
            Button(onClick={onEdit(RequirementEntity(softwareId=software.id,type=RequirementType.MINIMUM,platform=defaultRequirementPlatform(software.platform)))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Text(" Add requirement")}
            if(requirements.isEmpty())NoticeCard("No requirements are stored. Can It Run will return Not Verified for this app.",warning=true)
            LazyColumn(verticalArrangement=Arrangement.spacedBy(7.dp)){
                items(requirements.sortedWith(compareBy<RequirementEntity>{it.platform}.thenBy{it.type.name}),key={it.id}){value->
                    OutlinedCard{Column(Modifier.padding(11.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("${value.type.name.lowercase().replaceFirstChar(Char::uppercase)} • ${value.platform.ifBlank{"All listed platforms"}}",fontWeight=FontWeight.Bold);VerificationLabel(value.verificationStatus)};IconButton({onEdit(value)}){Icon(Icons.Default.Edit,"Edit requirement")};IconButton({onDelete(value)}){Icon(Icons.Default.Delete,"Delete requirement")}};Text(summarizeRequirement(value),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
                }
            }
        }},
        confirmButton={TextButton(onClick=onDismiss){Text("Done")}}
    )
}

private fun defaultRequirementPlatform(value:String):String=value.split('•',',').map(String::trim).filter(String::isNotBlank).singleOrNull().orEmpty()
private fun summarizeRequirement(value:RequirementEntity):String=listOfNotNull(value.minimumRamGB?.let{"$it GB RAM"},value.minimumStorageGB?.let{"$it GB storage"},value.minimumCpuTier?.let{"CPU tier $it"},value.minimumGpuTier?.let{"GPU tier $it"},value.minimumVramGB?.let{"$it GB VRAM"},value.requiredArchitecture,value.supportedOperatingSystems.takeIf{it.isNotEmpty()}?.joinToString()).joinToString(" • ").ifBlank{"Hardware model or feature allowlist"}

@Composable private fun DeleteDialog(title:String,body:String,onDismiss:()->Unit,onConfirm:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Text(body)},confirmButton={Button(onClick=onConfirm,colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)){Text("Delete")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}

@Composable private fun ImportBackupPane(online:Boolean,state:AdminUiState,viewModel:AdminViewModel){
    val context=LocalContext.current;val scope=rememberCoroutineScope();var exportBytes by remember{mutableStateOf<ByteArray?>(null)}
    val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{source->scope.launch{val loaded=withContext(Dispatchers.IO){runCatching{readImportBytes(context,source)}};loaded.onSuccess{bytes->viewModel.previewImport(bytes,source.lastPathSegment?:"database.json")}.onFailure{viewModel.reportFileError(it,"Could not read the selected file.")}}}}
    val create=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->uri?.let{target->exportBytes?.let{bytes->scope.launch{val written=withContext(Dispatchers.IO){runCatching{context.contentResolver.openOutputStream(target)?.use{it.write(bytes)}?:error("The destination file could not be opened.")}};written.onSuccess{viewModel.reportMessage("Backup saved.")}.onFailure{viewModel.reportFileError(it,"Could not save the backup file.")}}}}}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){item{Button({open.launch(arrayOf("application/json","application/zip","text/csv"))},Modifier.fillMaxWidth(),enabled=!state.isBusy){Icon(Icons.Default.FileOpen,null);Text(" Import JSON / ZIP package")}};state.preview?.let{preview->item{Card{Column(Modifier.padding(14.dp)){Text("Import preview",fontWeight=FontWeight.Bold);Text("${preview.productCount} products • ${preview.softwareCount} software • version ${preview.version?:"unknown"}");preview.errors.take(10).forEach{Text("Error: ${it.record}: ${it.message}",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)};if(preview.errors.size>10)Text("${preview.errors.size-10} more errors not shown",style=MaterialTheme.typography.labelSmall);preview.warnings.take(5).forEach{Text("Warning: ${it.record}: ${it.message}",style=MaterialTheme.typography.bodySmall)};Button(viewModel::commitImport,enabled=preview.canImport&&!state.isBusy,modifier=Modifier.fillMaxWidth()){Text("VALIDATE & IMPORT")}}}}};item{OutlinedButton({scope.launch{viewModel.exportBytes()?.let{bytes->exportBytes=bytes;create.launch("JI_PRODUCT_DATABASE_BACKUP.json")}}},Modifier.fillMaxWidth(),enabled=!state.isBusy){Icon(Icons.Default.SaveAlt,null);Text(" Export backup")}};item{Button(viewModel::checkUpdate,enabled=online&&!state.isBusy,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.SystemUpdate,null);Text(" Check remote update")}};item{OutlinedButton(onClick=viewModel::clearLocalHistory,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.DeleteSweep,null);Text(" Clear local searches & shortcuts")}};item{NoticeCard("Every import is validated first. A local safety backup is created before the database is replaced, and the transaction rolls back automatically if any write fails.")};item{PinChange(viewModel::setPin)}}
}

private fun readImportBytes(context:Context,source:Uri):ByteArray{
    val declaredLength=runCatching{context.contentResolver.openAssetFileDescriptor(source,"r")?.use{it.length}}.getOrNull()
    require(declaredLength==null||declaredLength<0||declaredLength<=MAX_MANUAL_IMPORT_BYTES){"The import file is larger than 25 MB."}
    return context.contentResolver.openInputStream(source)?.use{input->
        val output=ByteArrayOutputStream();val chunk=ByteArray(8_192);var total=0
        while(true){val count=input.read(chunk);if(count<0)break;total+=count;require(total<=MAX_MANUAL_IMPORT_BYTES){"The import file is larger than 25 MB."};output.write(chunk,0,count)}
        output.toByteArray()
    }?:error("The selected file could not be opened.")
}

private const val MAX_MANUAL_IMPORT_BYTES=25*1024*1024
@Composable private fun PinChange(setPin:(String)->Unit){var pin by remember{mutableStateOf("")};OutlinedCard{Column(Modifier.padding(14.dp)){Text("Change admin PIN",fontWeight=FontWeight.Bold);OutlinedTextField(pin,{pin=it.filter(Char::isDigit).take(12)},label={Text("New PIN (4–12 digits)")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth());Button({setPin(pin)},enabled=pin.length>=4,modifier=Modifier.align(Alignment.End)){Text("Update PIN")}}}}

private fun emptyProduct()=ProductSpec(0,"","","",category=ProductCategory.LAPTOP,price=0.0)
@Composable private fun ProductEditor(initial:ProductSpec,cpus:List<ProcessorEntity>,gpus:List<GpuEntity>,onDismiss:()->Unit,onSave:(ProductSpec)->Unit){
    var sku by remember{mutableStateOf(initial.sku)};var brand by remember{mutableStateOf(initial.brand)};var model by remember{mutableStateOf(initial.model)}
    var family by remember{mutableStateOf(initial.modelFamily.orEmpty())};var variant by remember{mutableStateOf(initial.variant.orEmpty())};var year by remember{mutableStateOf(initial.releaseYear?.toString().orEmpty())}
    var price by remember{mutableStateOf(initial.price.takeIf{it>0}?.toString().orEmpty())};var promo by remember{mutableStateOf(initial.promotionalPrice?.toString().orEmpty())}
    var ram by remember{mutableStateOf(initial.ramGB?.toString().orEmpty())};var ramType by remember{mutableStateOf(initial.ramType.orEmpty())}
    var storage by remember{mutableStateOf(initial.storageGB?.toString().orEmpty())};var storageType by remember{mutableStateOf(initial.storageType.orEmpty())}
    var os by remember{mutableStateOf(initial.operatingSystem.orEmpty())};var architecture by remember{mutableStateOf(initial.architecture.orEmpty())};var features by remember{mutableStateOf(initial.supportedFeatures.joinToString(", "))}
    var notes by remember{mutableStateOf(initial.notes.orEmpty())};var sourceName by remember{mutableStateOf(initial.sourceName.orEmpty())};var sourceUrl by remember{mutableStateOf(initial.sourceUrl.orEmpty())};var verifiedBy by remember{mutableStateOf(initial.verifiedBy.orEmpty())};var verifiedDate by remember{mutableStateOf(initial.verifiedDate.orEmpty())}
    var category by remember{mutableStateOf(initial.category)};var availability by remember{mutableStateOf(initial.availabilityStatus)};var verification by remember{mutableStateOf(initial.verificationStatus)}
    var cpu by remember{mutableStateOf(cpus.firstOrNull{it.id==initial.processor?.id})};var gpu by remember{mutableStateOf(gpus.firstOrNull{it.id==initial.gpu?.id})}
    val verifiedValid=verification!=VerificationStatus.VERIFIED||(sourceUrl.isHttps()&&verifiedBy.isNotBlank())
    val valid=sku.isNotBlank()&&brand.isNotBlank()&&model.isNotBlank()&&(price.toDoubleOrNull()?:0.0)>0&&(ram.isBlank()||(ram.toIntOrNull()?:0)>0)&&(storage.isBlank()||(storage.toIntOrNull()?:0)>0)&&verifiedValid
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(initial.id==0L)"Add product" else "Edit product")},text={
        Column(Modifier.heightIn(max=570.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Text("Identity",fontWeight=FontWeight.Bold)
            OutlinedTextField(sku,{sku=it},label={Text("SKU")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(brand,{brand=it},label={Text("Brand")},singleLine=true,modifier=Modifier.weight(1f));OutlinedTextField(model,{model=it},label={Text("Model")},singleLine=true,modifier=Modifier.weight(1f))}
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(family,{family=it},label={Text("Model family")},singleLine=true,modifier=Modifier.weight(1f));OutlinedTextField(variant,{variant=it},label={Text("Variant")},singleLine=true,modifier=Modifier.weight(1f))}
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Selector("Category",ProductCategory.entries.filter{it!=ProductCategory.PRINTER},category,{it.name.replace('_',' ')},Modifier.weight(1f)){category=it};Selector("Availability",AvailabilityStatus.entries,availability,{it.name.replace('_',' ')},Modifier.weight(1f)){availability=it}}
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(year,{year=it.filter(Char::isDigit)},label={Text("Release year")},singleLine=true,modifier=Modifier.weight(1f));OutlinedTextField(price,{price=it.decimalCharacters()},label={Text("Price")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.weight(1f));OutlinedTextField(promo,{promo=it.decimalCharacters()},label={Text("Promo price")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.weight(1f))}
            HorizontalDivider();Text("Technical specification",fontWeight=FontWeight.Bold)
            OptionalSelector("Processor",cpus,cpu,{"${it.manufacturer} ${it.model}"},noneText="Not specified"){cpu=it}
            OptionalSelector("GPU",gpus,gpu,{"${it.manufacturer} ${it.model}"},noneText="Not specified"){gpu=it}
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(ram,{ram=it.filter(Char::isDigit)},label={Text("RAM GB")},singleLine=true,modifier=Modifier.weight(1f));OutlinedTextField(ramType,{ramType=it},label={Text("RAM type")},singleLine=true,modifier=Modifier.weight(1f))}
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(storage,{storage=it.filter(Char::isDigit)},label={Text("Storage GB")},singleLine=true,modifier=Modifier.weight(1f));OutlinedTextField(storageType,{storageType=it},label={Text("Storage type")},singleLine=true,modifier=Modifier.weight(1f))}
            OutlinedTextField(os,{os=it},label={Text("Operating system and version")},supportingText={Text("Examples: Windows 11, macOS 14, Android 13")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(architecture,{architecture=it},label={Text("Architecture")},supportingText={Text("Examples: x64, arm64")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(features,{features=it},label={Text("Features, comma separated")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(notes,{notes=it},label={Text("Specification notes")},minLines=2,modifier=Modifier.fillMaxWidth())
            HorizontalDivider();Text("Verification",fontWeight=FontWeight.Bold)
            Selector("Status",VerificationStatus.entries,verification,{it.name.replace('_',' ')}){verification=it}
            OutlinedTextField(sourceName,{sourceName=it},label={Text("Source name")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(sourceUrl,{sourceUrl=it},label={Text("Official HTTPS source URL")},singleLine=true,isError=verification==VerificationStatus.VERIFIED&&!sourceUrl.isHttps(),modifier=Modifier.fillMaxWidth())
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(verifiedBy,{verifiedBy=it},label={Text("Verified by")},singleLine=true,isError=verification==VerificationStatus.VERIFIED&&verifiedBy.isBlank(),modifier=Modifier.weight(1f));OutlinedTextField(verifiedDate,{verifiedDate=it},label={Text("Verified date")},placeholder={Text("YYYY-MM-DD")},singleLine=true,modifier=Modifier.weight(1f))}
        }
    },confirmButton={Button({onSave(initial.copy(sku=sku.trim(),brand=brand.trim(),model=model.trim(),modelFamily=family.trim().takeIf(String::isNotBlank),variant=variant.trim().takeIf(String::isNotBlank),releaseYear=year.toIntOrNull(),category=category,availabilityStatus=availability,price=price.toDouble(),promotionalPrice=promo.toDoubleOrNull(),ramGB=ram.toIntOrNull(),ramType=ramType.trim().takeIf(String::isNotBlank),storageGB=storage.toIntOrNull(),storageType=storageType.trim().takeIf(String::isNotBlank),operatingSystem=os.trim().takeIf(String::isNotBlank),architecture=architecture.trim().takeIf(String::isNotBlank),supportedFeatures=features.toStringSet(),notes=notes.trim().takeIf(String::isNotBlank),processor=cpu?.toDomain(),gpu=gpu?.toDomain(),sourceName=sourceName.trim().takeIf(String::isNotBlank),sourceUrl=sourceUrl.trim().takeIf(String::isNotBlank),verifiedBy=verifiedBy.trim().takeIf(String::isNotBlank),verifiedDate=verifiedDate.trim().takeIf(String::isNotBlank),verificationStatus=verification,lastUpdated=java.time.LocalDate.now().toString()))},enabled=valid){Text("Save")}},dismissButton={TextButton(onDismiss){Text("Cancel")}})
}

@Composable private fun ProcessorEditor(initial:ProcessorEntity,onDismiss:()->Unit,onSave:(ProcessorEntity)->Unit){
    var maker by remember{mutableStateOf(initial.manufacturer)};var model by remember{mutableStateOf(initial.model)};var family by remember{mutableStateOf(initial.family)};var generation by remember{mutableStateOf(initial.generation.orEmpty())};var architecture by remember{mutableStateOf(initial.architecture.orEmpty())}
    var cores by remember{mutableStateOf(initial.coreCount?.toString().orEmpty())};var threads by remember{mutableStateOf(initial.threadCount?.toString().orEmpty())};var base by remember{mutableStateOf(initial.baseClockGhz?.toString().orEmpty())};var boost by remember{mutableStateOf(initial.boostClockGhz?.toString().orEmpty())};var integrated by remember{mutableStateOf(initial.integratedGpu.orEmpty())}
    var tier by remember{mutableStateOf(initial.performanceTier?.toString().orEmpty())};var workload by remember{mutableStateOf(initial.workloadTier?.toString().orEmpty())};var source by remember{mutableStateOf(initial.sourceUrl.orEmpty())};var notes by remember{mutableStateOf(initial.notes.orEmpty())}
    val valid=(tier.isBlank()||tier.toIntOrNull() in 1..7)&&(workload.isBlank()||workload.toIntOrNull() in 1..7)&&(cores.isBlank()||(cores.toIntOrNull()?:0)>0)&&(threads.isBlank()||(threads.toIntOrNull()?:0)>0)
    SimpleEditor(if(initial.id==0L)"Add processor" else "Edit processor",onDismiss,{onSave(initial.copy(manufacturer=maker.trim(),model=model.trim(),family=family.trim(),generation=generation.trim().takeIf(String::isNotBlank),architecture=architecture.trim().takeIf(String::isNotBlank),coreCount=cores.toIntOrNull(),threadCount=threads.toIntOrNull(),baseClockGhz=base.toDoubleOrNull(),boostClockGhz=boost.toDoubleOrNull(),integratedGpu=integrated.trim().takeIf(String::isNotBlank),performanceTier=tier.toIntOrNull(),workloadTier=workload.toIntOrNull(),sourceUrl=source.trim().takeIf(String::isNotBlank),notes=notes.trim().takeIf(String::isNotBlank)))},maker,model,{maker=it},{model=it},extraValid=valid){
        OutlinedTextField(family,{family=it},label={Text("Family")},modifier=Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(generation,{generation=it},label={Text("Generation")},modifier=Modifier.weight(1f));OutlinedTextField(architecture,{architecture=it},label={Text("Architecture")},modifier=Modifier.weight(1f))}
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(cores,{cores=it.filter(Char::isDigit)},label={Text("Cores")},modifier=Modifier.weight(1f));OutlinedTextField(threads,{threads=it.filter(Char::isDigit)},label={Text("Threads")},modifier=Modifier.weight(1f))}
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(base,{base=it.decimalCharacters()},label={Text("Base GHz")},modifier=Modifier.weight(1f));OutlinedTextField(boost,{boost=it.decimalCharacters()},label={Text("Boost GHz")},modifier=Modifier.weight(1f))}
        OutlinedTextField(integrated,{integrated=it},label={Text("Integrated graphics")},modifier=Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(tier,{tier=it.filter(Char::isDigit)},label={Text("Performance tier 1–7")},isError=tier.isNotBlank()&&tier.toIntOrNull() !in 1..7,modifier=Modifier.weight(1f));OutlinedTextField(workload,{workload=it.filter(Char::isDigit)},label={Text("Workload tier 1–7")},isError=workload.isNotBlank()&&workload.toIntOrNull() !in 1..7,modifier=Modifier.weight(1f))}
        OutlinedTextField(source,{source=it},label={Text("Official source URL")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(notes,{notes=it},label={Text("Notes")},minLines=2,modifier=Modifier.fillMaxWidth())
    }
}

@Composable private fun GpuEditor(initial:GpuEntity,onDismiss:()->Unit,onSave:(GpuEntity)->Unit){
    var maker by remember{mutableStateOf(initial.manufacturer)};var model by remember{mutableStateOf(initial.model)};var type by remember{mutableStateOf(initial.type)};var vram by remember{mutableStateOf(initial.vramGB?.toString().orEmpty())};var architecture by remember{mutableStateOf(initial.architecture.orEmpty())}
    var tier by remember{mutableStateOf(initial.performanceTier?.toString().orEmpty())};var graphicsTier by remember{mutableStateOf(initial.graphicsTier?.toString().orEmpty())};var source by remember{mutableStateOf(initial.sourceUrl.orEmpty())};var notes by remember{mutableStateOf(initial.notes.orEmpty())}
    val valid=(tier.isBlank()||tier.toIntOrNull() in 1..7)&&(graphicsTier.isBlank()||graphicsTier.toIntOrNull() in 1..7)&&(vram.isBlank()||(vram.toDoubleOrNull()?:0.0)>0)
    SimpleEditor(if(initial.id==0L)"Add GPU" else "Edit GPU",onDismiss,{onSave(initial.copy(manufacturer=maker.trim(),model=model.trim(),type=type,vramGB=vram.toDoubleOrNull(),architecture=architecture.trim().takeIf(String::isNotBlank),performanceTier=tier.toIntOrNull(),graphicsTier=graphicsTier.toIntOrNull(),sourceUrl=source.trim().takeIf(String::isNotBlank),notes=notes.trim().takeIf(String::isNotBlank)))},maker,model,{maker=it},{model=it},extraValid=valid){
        Selector("GPU type",GpuType.entries,type,{it.name}){type=it}
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(vram,{vram=it.decimalCharacters()},label={Text("VRAM GB")},modifier=Modifier.weight(1f));OutlinedTextField(architecture,{architecture=it},label={Text("Architecture")},modifier=Modifier.weight(1f))}
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(tier,{tier=it.filter(Char::isDigit)},label={Text("Performance tier 1–7")},isError=tier.isNotBlank()&&tier.toIntOrNull() !in 1..7,modifier=Modifier.weight(1f));OutlinedTextField(graphicsTier,{graphicsTier=it.filter(Char::isDigit)},label={Text("Graphics tier 1–7")},isError=graphicsTier.isNotBlank()&&graphicsTier.toIntOrNull() !in 1..7,modifier=Modifier.weight(1f))}
        OutlinedTextField(source,{source=it},label={Text("Official source URL")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(notes,{notes=it},label={Text("Notes")},minLines=2,modifier=Modifier.fillMaxWidth())
    }
}
@Composable private fun SoftwareEditor(initial:SoftwareEntity,onDismiss:()->Unit,onSave:(SoftwareEntity)->Unit){
    var name by remember{mutableStateOf(initial.name)};var version by remember{mutableStateOf(initial.version)};var developer by remember{mutableStateOf(initial.developer.orEmpty())};var category by remember{mutableStateOf(initial.category)};var platform by remember{mutableStateOf(initial.platform)}
    var website by remember{mutableStateOf(initial.officialWebsite.orEmpty())};var source by remember{mutableStateOf(initial.requirementsSourceUrl.orEmpty())};var verifiedDate by remember{mutableStateOf(initial.lastVerified.orEmpty())};var description by remember{mutableStateOf(initial.description.orEmpty())};var verification by remember{mutableStateOf(initial.verificationStatus)}
    val platforms=listOf("Windows","macOS","Windows • macOS","Android","iOS","iPadOS","iOS • iPadOS","Android • iOS • iPadOS","Linux","Windows • macOS • Linux")
    val verifiedValid=verification!=VerificationStatus.VERIFIED||source.isHttps()
    SimpleEditor(if(initial.id==0L)"Add software" else "Edit software",onDismiss,{onSave(initial.copy(name=name.trim(),version=version.trim(),developer=developer.trim().takeIf(String::isNotBlank),category=category.trim(),platform=platform,officialWebsite=website.trim().takeIf(String::isNotBlank),requirementsSourceUrl=source.trim().takeIf(String::isNotBlank),lastVerified=verifiedDate.trim().takeIf(String::isNotBlank),description=description.trim().takeIf(String::isNotBlank),verificationStatus=verification))},name,version,{name=it},{version=it},"Name","Version",extraValid=verifiedValid){
        OutlinedTextField(developer,{developer=it},label={Text("Developer / publisher")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(category,{category=it},label={Text("Category")},modifier=Modifier.fillMaxWidth())
        Selector("Supported platforms",platforms,platform,{it}){platform=it}
        OutlinedTextField(website,{website=it},label={Text("Official website")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(source,{source=it},label={Text("Official requirements HTTPS URL")},singleLine=true,isError=verification==VerificationStatus.VERIFIED&&!source.isHttps(),modifier=Modifier.fillMaxWidth())
        OutlinedTextField(verifiedDate,{verifiedDate=it},label={Text("Last verified")},placeholder={Text("YYYY-MM-DD")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(description,{description=it},label={Text("Description")},minLines=2,modifier=Modifier.fillMaxWidth())
        Selector("Verification status",VerificationStatus.entries,verification,{it.name.replace('_',' ')}){verification=it}
    }
}
@Composable private fun RequirementEditor(software:SoftwareEntity,initial:RequirementEntity,processors:List<ProcessorEntity>,gpus:List<GpuEntity>,onDismiss:()->Unit,onSave:(RequirementEntity)->Unit){
    var type by remember{mutableStateOf(initial.type)};var platform by remember{mutableStateOf(initial.platform)}
    var ram by remember{mutableStateOf(initial.minimumRamGB?.toString().orEmpty())};var storage by remember{mutableStateOf(initial.minimumStorageGB?.toString().orEmpty())};var cpu by remember{mutableStateOf(initial.minimumCpuTier?.toString().orEmpty())};var gpu by remember{mutableStateOf(initial.minimumGpuTier?.toString().orEmpty())};var vram by remember{mutableStateOf(initial.minimumVramGB?.toString().orEmpty())}
    var architecture by remember{mutableStateOf(initial.requiredArchitecture.orEmpty())};var operatingSystems by remember{mutableStateOf(initial.supportedOperatingSystems.joinToString(", "))};var features by remember{mutableStateOf(initial.requiredFeatures.joinToString(", "))};var notes by remember{mutableStateOf(initial.notes.orEmpty())};var verification by remember{mutableStateOf(initial.verificationStatus)}
    var acceptedCpus by remember{mutableStateOf(initial.acceptedProcessorIds)};var acceptedGpus by remember{mutableStateOf(initial.acceptedGpuIds)}
    val platformOptions=(listOf("")+software.platform.split('•',',').map(String::trim).filter(String::isNotBlank)).distinct()
    val tiersValid=(cpu.isBlank()||cpu.toIntOrNull() in 1..7)&&(gpu.isBlank()||gpu.toIntOrNull() in 1..7)
    val meaningful=listOf(ram,storage,cpu,gpu,vram,architecture,operatingSystems,features).any{it.isNotBlank()}||acceptedCpus.isNotEmpty()||acceptedGpus.isNotEmpty()
    val verifiedValid=verification!=VerificationStatus.VERIFIED||software.requirementsSourceUrl.isHttps()
    AlertDialog(onDismissRequest=onDismiss,title={Text("${software.name} ${software.version}")},text={
        Column(Modifier.heightIn(max=570.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Selector("Requirement type",RequirementType.entries,type,{it.name},Modifier.weight(1f)){type=it};Selector("Platform scope",platformOptions,platform,{it.ifBlank{"All"}},Modifier.weight(1f)){platform=it}}
            Text("Use a platform scope when Windows, macOS, Android, iOS, or iPadOS requirements differ.",style=MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(ram,{ram=it.filter(Char::isDigit)},label={Text("RAM GB")},modifier=Modifier.weight(1f));OutlinedTextField(storage,{storage=it.filter(Char::isDigit)},label={Text("Storage GB")},modifier=Modifier.weight(1f))}
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(cpu,{cpu=it.filter(Char::isDigit)},label={Text("CPU tier 1–7")},isError=cpu.isNotBlank()&&cpu.toIntOrNull() !in 1..7,modifier=Modifier.weight(1f));OutlinedTextField(gpu,{gpu=it.filter(Char::isDigit)},label={Text("GPU tier 1–7")},isError=gpu.isNotBlank()&&gpu.toIntOrNull() !in 1..7,modifier=Modifier.weight(1f));OutlinedTextField(vram,{vram=it.decimalCharacters()},label={Text("VRAM GB")},modifier=Modifier.weight(1f))}
            OutlinedTextField(architecture,{architecture=it},label={Text("Required architecture")},supportingText={Text("Examples: x64, arm64")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(operatingSystems,{operatingSystems=it},label={Text("Supported OS versions, comma separated")},supportingText={Text("Examples: Windows 11, macOS 14")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(features,{features=it},label={Text("Required features, comma separated")},modifier=Modifier.fillMaxWidth())
            MultiHardwareSelector("Approved processor models",processors.map{it.id to "${it.manufacturer} ${it.model}"},acceptedCpus){acceptedCpus=it}
            MultiHardwareSelector("Approved GPU models",gpus.map{it.id to "${it.manufacturer} ${it.model}"},acceptedGpus){acceptedGpus=it}
            OutlinedTextField(notes,{notes=it},label={Text("Specification notes")},minLines=2,modifier=Modifier.fillMaxWidth())
            Selector("Verification status",VerificationStatus.entries,verification,{it.name.replace('_',' ')}){verification=it}
            if(verification==VerificationStatus.VERIFIED&&!software.requirementsSourceUrl.isHttps())Text("Add an official HTTPS requirements source to the software record before marking this verified.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={Button({onSave(initial.copy(softwareId=software.id,type=type,platform=platform,minimumRamGB=ram.toIntOrNull(),minimumStorageGB=storage.toIntOrNull(),minimumCpuTier=cpu.toIntOrNull(),minimumGpuTier=gpu.toIntOrNull(),minimumVramGB=vram.toDoubleOrNull(),requiredArchitecture=architecture.trim().takeIf(String::isNotBlank),supportedOperatingSystems=operatingSystems.toStringSet(),requiredFeatures=features.toStringSet(),acceptedProcessorIds=acceptedCpus,acceptedGpuIds=acceptedGpus,notes=notes.trim().takeIf(String::isNotBlank),verificationStatus=verification))},enabled=tiersValid&&meaningful&&verifiedValid){Text("Save")}},dismissButton={TextButton(onDismiss){Text("Cancel")}})
}

@Composable private fun MultiHardwareSelector(label:String,options:List<Pair<Long,String>>,selected:Set<Long>,onSelected:(Set<Long>)->Unit){
    var expanded by remember{mutableStateOf(false)}
    Box(Modifier.fillMaxWidth()){
        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text(if(selected.isEmpty())"$label: Any model" else "$label: ${selected.size} selected")}
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=340.dp)){
            DropdownMenuItem(text={Text("Any model")},leadingIcon={Checkbox(selected.isEmpty(),null)},onClick={onSelected(emptySet())})
            options.forEach{(id,name)->DropdownMenuItem(text={Text(name)},leadingIcon={Checkbox(id in selected,null)},onClick={onSelected(if(id in selected)selected-id else selected+id)})}
        }
    }
}
@Composable private fun SimpleEditor(title:String,onDismiss:()->Unit,onSave:()->Unit,first:String,second:String,setFirst:(String)->Unit,setSecond:(String)->Unit,firstLabel:String="Manufacturer",secondLabel:String="Model",extraValid:Boolean=true,extra:@Composable ColumnScope.()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(first,setFirst,label={Text(firstLabel)});OutlinedTextField(second,setSecond,label={Text(secondLabel)});extra()}},confirmButton={Button(onSave,enabled=first.isNotBlank()&&second.isNotBlank()&&extraValid){Text("Save")}},dismissButton={TextButton(onDismiss){Text("Cancel")}})}

private fun String.decimalCharacters():String=filter{it.isDigit()||it=='.'}.let{value->if(value.count{it=='.'}<=1)value else value.substringBefore('.')+"."+value.substringAfter('.').replace(".","")}
private fun String.toStringSet():Set<String> = split(',').map(String::trim).filter(String::isNotBlank).toSet()
private fun String?.isHttps():Boolean=this?.trim()?.startsWith("https://",ignoreCase=true)==true
