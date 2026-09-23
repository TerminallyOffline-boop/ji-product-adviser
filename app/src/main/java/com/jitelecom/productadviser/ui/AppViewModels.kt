package com.jitelecom.productadviser.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.jitelecom.productadviser.data.importexport.*
import com.jitelecom.productadviser.data.local.*
import com.jitelecom.productadviser.data.preferences.*
import com.jitelecom.productadviser.data.remote.RemoteUpdateWorker
import com.jitelecom.productadviser.data.repository.toDomain
import com.jitelecom.productadviser.domain.compatibility.CompatibilityEngine
import com.jitelecom.productadviser.domain.model.*
import com.jitelecom.productadviser.domain.recommendation.RecommendationEngine
import com.jitelecom.productadviser.domain.repository.*
import com.jitelecom.productadviser.util.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(preferences: AppPreferences, connectivity: ConnectivityObserver) : ViewModel() {
    val settings = preferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val online = connectivity.online.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    products: ProductRepository, catalog: CatalogRepository, metadata: MetadataDao, analytics: AnalyticsDao,
    preferences: AppPreferences
) : ViewModel() {
    val products = products.observeProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val software = catalog.observeSoftware().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val databaseVersion = metadata.observe("databaseVersion").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val popular = analytics.observePopular(5).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = preferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
}

enum class ProductSort(val label: String) { NAME("Name"), PRICE_LOW("Lowest price"), PRICE_HIGH("Highest price"), PERFORMANCE("Performance") }
private data class ProductFacets(val category: ProductCategory?, val brand: String?, val availableOnly: Boolean, val sort: ProductSort)

@HiltViewModel
@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProductsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProductRepository,
    private val analytics: AnalyticsDao
) : ViewModel() {
    val query = MutableStateFlow(savedStateHandle.get<String>("query").orEmpty())
    val minimumPrice = MutableStateFlow("")
    val maximumPrice = MutableStateFlow("")
    val category = MutableStateFlow<ProductCategory?>(null)
    val brand = MutableStateFlow<String?>(null)
    val availableOnly = MutableStateFlow(false)
    val sort = MutableStateFlow(ProductSort.NAME)
    private val catalogProducts = repository.observeProducts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val brands = catalogProducts.map { rows -> rows.map { it.brand }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = catalogProducts.map { rows -> rows.map { it.category }.distinct().sortedBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val priceRange = combine(minimumPrice, maximumPrice) { min, max -> min.toDoubleOrNull() to max.toDoubleOrNull() }
    private val facets = combine(category, brand, availableOnly, sort, ::ProductFacets)
    val products = combine(query.debounce(180), priceRange, facets) { text, prices, options -> Triple(text, prices, options) }
        .flatMapLatest { (text, prices, options) -> repository.observeProducts(text).map { rows ->
            rows.asSequence()
                .filter { prices.first == null || it.effectivePrice >= prices.first!! }
                .filter { prices.second == null || it.effectivePrice <= prices.second!! }
                .filter { options.category == null || it.category == options.category }
                .filter { options.brand == null || it.brand == options.brand }
                .filter { !options.availableOnly || it.availabilityStatus in setOf(AvailabilityStatus.AVAILABLE, AvailabilityStatus.LIMITED, AvailabilityStatus.DISPLAY_UNIT) }
                .let { sequence -> when (options.sort) {
                    ProductSort.NAME -> sequence.sortedWith(compareBy<ProductSpec> { it.brand }.thenBy { it.model })
                    ProductSort.PRICE_LOW -> sequence.sortedBy(ProductSpec::effectivePrice)
                    ProductSort.PRICE_HIGH -> sequence.sortedByDescending(ProductSpec::effectivePrice)
                    ProductSort.PERFORMANCE -> sequence.sortedWith(compareByDescending<ProductSpec> { it.processor?.performanceTier ?: 0 }.thenByDescending { it.gpu?.performanceTier ?: 0 }.thenBy { it.effectivePrice })
                }}.toList()
        }}
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun setQuery(value: String) { query.value = value }
    fun submitSearch() { query.value.trim().takeIf { it.length >= 3 }?.let { value -> viewModelScope.launch { analytics.increment("product_search", value.lowercase()) } } }
    fun resetFilters() { minimumPrice.value=""; maximumPrice.value=""; category.value=null; brand.value=null; availableOnly.value=false; sort.value=ProductSort.NAME }
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(savedState: SavedStateHandle, repository: ProductRepository, private val preferences: AppPreferences) : ViewModel() {
    private val id = checkNotNull(savedState.get<String>("id")).toLong()
    private val _product = MutableStateFlow<ProductSpec?>(null); val product = _product.asStateFlow()
    private val _loading = MutableStateFlow(true); val loading = _loading.asStateFlow()
    val settings = preferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    init { viewModelScope.launch { _product.value=repository.getProduct(id); preferences.recordViewed(id); _loading.value=false } }
    fun toggleFavorite() = viewModelScope.launch { preferences.toggleFavorite(id) }
}

@HiltViewModel
class CompatibilityViewModel @Inject constructor(
    products: ProductRepository,
    catalog: CatalogRepository,
    private val engine: CompatibilityEngine,
    private val analytics: AnalyticsDao
) : ViewModel() {
    val products = products.observeProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val software = catalog.observeSoftware().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selectedProduct = MutableStateFlow<Long?>(null)
    val selectedSoftware = MutableStateFlow<Long?>(null)
    val showUnavailableApps = MutableStateFlow(false)
    private val _result = MutableStateFlow<CompatibilityResult?>(null); val result = _result.asStateFlow()
    val compatibleSoftware = combine(software, this.products, selectedProduct, showUnavailableApps) { apps, allProducts, productId, showUnavailable ->
        val product = allProducts.firstOrNull { it.id == productId }
        if (product == null || showUnavailable) apps else apps.filter { it.supportsOperatingSystem(product.operatingSystemForCompatibility()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val availableSoftwareCount = combine(software, this.products, selectedProduct) { apps, allProducts, productId ->
        val product = allProducts.firstOrNull { it.id == productId }
        if (product == null) apps.size else apps.count { it.supportsOperatingSystem(product.operatingSystemForCompatibility()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    private val repository = catalog

    fun selectProduct(id: Long) {
        selectedProduct.value = id
        val product = products.value.firstOrNull { it.id == id }
        val selectedApp = software.value.firstOrNull { it.id == selectedSoftware.value }
        if (product != null && selectedApp != null && !selectedApp.supportsOperatingSystem(product.operatingSystemForCompatibility())) {
            selectedSoftware.value = null
        }
        _result.value = null
    }

    fun selectSoftware(id: Long) {
        selectedSoftware.value = id
        _result.value = null
    }
    fun setShowUnavailable(value: Boolean) {
        showUnavailableApps.value = value
        if (!value) {
            val product = products.value.firstOrNull { it.id == selectedProduct.value }
            val app = software.value.firstOrNull { it.id == selectedSoftware.value }
            if (product != null && app != null && !app.supportsOperatingSystem(product.operatingSystemForCompatibility())) selectedSoftware.value = null
        }
        _result.value = null
    }

    fun evaluate() = viewModelScope.launch {
        val product = products.value.firstOrNull { it.id == selectedProduct.value } ?: return@launch
        val app = software.value.firstOrNull { it.id == selectedSoftware.value } ?: return@launch
        _result.value = engine.evaluate(product, app, repository.getRequirements(app.id))
        analytics.increment("compatibility_check", app.displayName)
    }
}

@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    catalog: CatalogRepository,
    private val engine: RecommendationEngine,
    private val analytics: AnalyticsDao,
    preferences: AppPreferences
) : ViewModel() {
    val products = productRepository.observeProducts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val software = catalog.observeSoftware().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val budget = MutableStateFlow("35000")
    val profile = MutableStateFlow("Architecture Student")
    val category = MutableStateFlow(ProductCategory.LAPTOP)
    val selectedSoftware = MutableStateFlow<Set<Long>>(emptySet())
    val applicableSoftware = combine(software, category) { apps, selectedCategory ->
        apps.filter { it.appliesTo(selectedCategory) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val priorities = MutableStateFlow<Set<String>>(setOf("Performance"))
    val brand = MutableStateFlow("")
    val aboveBudget = MutableStateFlow(false)
    val settings = preferences.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    private val _results = MutableStateFlow<List<RecommendationResult>>(emptyList()); val results = _results.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val repository = catalog
    init {
        merge(
            budget.drop(1).map { Unit }, profile.drop(1).map { Unit }, category.drop(1).map { Unit },
            selectedSoftware.drop(1).map { Unit }, priorities.drop(1).map { Unit }, brand.drop(1).map { Unit }, aboveBudget.drop(1).map { Unit }
        ).onEach { _results.value=emptyList(); _message.value=null }.launchIn(viewModelScope)
    }
    fun toggleSoftware(id: Long) { selectedSoftware.update { if (id in it) it - id else it + id } }
    fun setCategory(value: ProductCategory) {
        category.value = value
        val allowed = software.value.filter { it.appliesTo(value) }.map { it.id }.toSet()
        selectedSoftware.update { it intersect allowed }
    }
    fun recommend() = viewModelScope.launch {
        val amount = budget.value.replace(",", "").toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _results.value = emptyList()
            _message.value = "Enter a valid maximum budget first."
            return@launch
        }
        val availableProducts = productRepository.getProducts()
        if (availableProducts.isEmpty()) {
            _results.value = emptyList()
            _message.value = "No products are available in the local catalog yet."
            return@launch
        }
        val selectedApps = selectedSoftware.value intersect software.value.filter { it.appliesTo(category.value) }.map { it.id }.toSet()
        val requirements = selectedApps.associateWith { repository.getRequirements(it) }
        val availableSoftware = repository.getSoftware().associateBy { it.id }
        val request = CustomerRequest(
            profile.value, category.value, amount, selectedApps, priorities.value,
            brand.value.trim().takeIf(String::isNotBlank), aboveBudget.value, settings.value.allowAboveBudgetPercent
        )
        _results.value = engine.recommend(request, availableProducts, availableSoftware, requirements)
        _message.value = if (_results.value.isEmpty()) {
            "No ${category.value.name.lowercase()} matches were found within this budget and brand filter."
        } else {
            "${_results.value.size} best match${if (_results.value.size == 1) "" else "es"} found."
        }
        analytics.increment("budget_range", "₱${(amount / 5000).toInt() * 5}k range")
        analytics.increment("customer_profile", profile.value)
        _results.value.forEach { analytics.increment("recommended_product", it.product.displayName) }
    }
    fun parse(text: String) {
        val parsed = engine.parseLocalQuery(text, software.value)
        parsed.budget?.let { budget.value = it.toInt().toString() }
        parsed.profile?.let { profile.value = it }
        parsed.category?.let(::setCategory)
        if (parsed.softwareIds.isNotEmpty()) {
            val allowed = software.value.filter { it.appliesTo(category.value) }.map { it.id }.toSet()
            selectedSoftware.value = parsed.softwareIds intersect allowed
        }
    }
    fun reset() {
        budget.value="35000"; profile.value="Architecture Student"; category.value=ProductCategory.LAPTOP
        selectedSoftware.value=emptySet(); priorities.value=setOf("Performance"); brand.value=""; aboveBudget.value=false
    }

    private fun SoftwareSpec.appliesTo(category: ProductCategory): Boolean {
        val families = platformFamilies(platform)
        return when (category) {
            ProductCategory.LAPTOP, ProductCategory.DESKTOP -> PlatformFamily.WINDOWS in families || PlatformFamily.MACOS in families || PlatformFamily.LINUX in families
            ProductCategory.SMARTPHONE, ProductCategory.TABLET -> PlatformFamily.ANDROID in families || PlatformFamily.IOS in families
            else -> false
        }
    }
}

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CompareViewModel @Inject constructor(
    products: ProductRepository,
    catalog: CatalogRepository,
    private val engine: CompatibilityEngine
) : ViewModel() {
    val products = products.observeProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val software = catalog.observeSoftware().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selected = MutableStateFlow<List<Long>>(emptyList())
    val selectedSoftware = MutableStateFlow<Long?>(null)
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    val compatibility = combine(this.products, selected, selectedSoftware) { all, ids, softwareId -> Triple(all.filter { it.id in ids }, softwareId, software.value.firstOrNull { it.id == softwareId }) }
        .flatMapLatest { (chosen, softwareId, app) -> flow {
            if (softwareId == null || app == null) emit(emptyMap())
            else { val requirements=catalog.getRequirements(softwareId); emit(chosen.associate { it.id to engine.evaluate(it, app, requirements) }) }
        }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    fun toggle(id: Long) {
        val candidate = products.value.firstOrNull { it.id == id } ?: return
        selected.update { current ->
            when {
                id in current -> { _message.value=null; current-id }
                current.size >= 4 -> { _message.value="Compare supports up to four products."; current }
                current.mapNotNull { chosenId -> products.value.firstOrNull { it.id==chosenId } }.any { it.category != candidate.category } -> {
                    _message.value="Choose products from the same category for a useful comparison."; current
                }
                else -> { _message.value=null; current+id }
            }
        }
    }
    fun clear() { selected.value=emptyList(); selectedSoftware.value=null; _message.value=null }
}

@HiltViewModel
class SoftwareViewModel @Inject constructor(catalog: CatalogRepository) : ViewModel() {
    val software = catalog.observeSoftware().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class AdminUiState(
    val unlocked: Boolean = false,
    val message: String? = null,
    val preview: ImportPreview? = null,
    val isBusy: Boolean = false,
    val undoArchiveId: Long? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val importExport: ImportExportManager,
    private val products: ProductRepository,
    private val productDao: ProductDao,
    private val hardwareDao: HardwareDao,
    private val softwareDao: SoftwareDao,
    private val requirementDao: RequirementDao,
    private val analytics: AnalyticsDao,
    private val workManager: WorkManager
) : ViewModel() {
    private val _state = MutableStateFlow(AdminUiState()); val state = _state.asStateFlow()
    val productsFlow = products.observeProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val processors = hardwareDao.observeProcessors().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val gpus = hardwareDao.observeGpus().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val software = softwareDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = preferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    private var pending: Pair<ByteArray, String>? = null
    private var pendingIsCsv = false
    private var failedPinAttempts=0
    private var pinLockedUntil=0L
    fun unlock(pin: String) = viewModelScope.launch {
        val now=System.currentTimeMillis()
        if(now<pinLockedUntil){message("Too many attempts. Try again in ${((pinLockedUntil-now)/1000).coerceAtLeast(1)} seconds.");return@launch}
        val valid=preferences.verifyAdminPin(pin)
        if(valid){failedPinAttempts=0;_state.update{it.copy(unlocked=true,message=null)}}else{
            failedPinAttempts++
            if(failedPinAttempts>=5){pinLockedUntil=now+30_000;failedPinAttempts=0;message("Too many attempts. Admin is locked for 30 seconds.")}else message("Incorrect PIN. ${5-failedPinAttempts} attempts remaining.")
        }
    }
    fun configurePin(pin: String) = viewModelScope.launch { runCatching { preferences.setAdminPin(pin) }.onSuccess { _state.update { it.copy(unlocked=true,message="Admin PIN configured.") } }.onFailure { message(it.message ?: "PIN must contain at least four digits.") } }
    fun lock() { _state.value = AdminUiState() }
    fun setPin(pin: String) = viewModelScope.launch { runCatching { preferences.setAdminPin(pin) }.onSuccess { message("PIN updated.") }.onFailure { message(it.message ?: "Invalid PIN") } }
    fun previewImport(bytes: ByteArray, name: String) = viewModelScope.launch(Dispatchers.Default) {
        _state.update { it.copy(isBusy=true,message="Validating import…") }
        pending = bytes to name
        pendingIsCsv = name.endsWith(".csv", true)
        val preview = if (pendingIsCsv) importExport.previewProductCsv(bytes.toString(Charsets.UTF_8), processors.value.map { it.id }.toSet(), gpus.value.map { it.id }.toSet()).second else importExport.preview(bytes, name).second
        _state.update { it.copy(preview=preview,message=if(preview.canImport)"Import is ready for review." else "Import has validation errors.",isBusy=false) }
    }
    fun commitImport() = viewModelScope.launch { val (bytes, name)=pending ?: return@launch; _state.update { it.copy(isBusy=true,message="Importing database…") }; val result=if(pendingIsCsv) importExport.importProductCsv(bytes) else importExport.importPackage(bytes,name); _state.update { it.copy(message=result.message,preview=result.preview,isBusy=false) } }
    suspend fun exportBytes() = importExport.exportJson()
    fun checkUpdate() {
        val request = OneTimeWorkRequestBuilder<RemoteUpdateWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        workManager.enqueueUniqueWork("remote_database_update", ExistingWorkPolicy.REPLACE, request)
        message("Checking for a database update…")
        viewModelScope.launch {
            val info = workManager.getWorkInfoByIdFlow(request.id).filterNotNull().filter { it.state.isFinished }.first()
            message(info.outputData.getString("message") ?: when(info.state){
                WorkInfo.State.SUCCEEDED -> "Database update check completed."
                WorkInfo.State.CANCELLED -> "Database update check was cancelled."
                else -> "Database update failed. Check the manifest URL and connection."
            })
        }
    }
    fun saveProduct(product: ProductSpec) = viewModelScope.launch { runCatching { products.saveProduct(product) }.onSuccess { message("Product saved.") }.onFailure { message(it.message ?: "Could not save product.") } }
    fun archiveProduct(id: Long) = viewModelScope.launch { products.archiveProduct(id); _state.update { it.copy(message="Product archived. You can undo this action.",undoArchiveId=id) } }
    fun restoreArchived() = viewModelScope.launch { val id=_state.value.undoArchiveId ?: return@launch; productDao.setArchived(id,false); _state.update { it.copy(message="Product restored.",undoArchiveId=null) } }
    fun clearLocalHistory() = viewModelScope.launch { analytics.deleteAll();preferences.clearProductHistory();message("Local searches, favorites, recent products and usage counters were cleared.") }
    fun saveProcessor(value: ProcessorEntity) = viewModelScope.launch { if(value.id==0L) hardwareDao.insertProcessor(value) else hardwareDao.updateProcessor(value); message("Processor saved.") }
    fun saveGpu(value: GpuEntity) = viewModelScope.launch { if(value.id==0L) hardwareDao.insertGpu(value) else hardwareDao.updateGpu(value); message("GPU saved.") }
    fun saveSoftware(value: SoftwareEntity) = viewModelScope.launch { if(value.id==0L) softwareDao.insert(value) else softwareDao.update(value); message("Software saved.") }
    fun saveRequirement(value: RequirementEntity) = viewModelScope.launch { requirementDao.insert(value); message("Requirement saved.") }
    private fun message(value: String) { _state.update { it.copy(message=value) } }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(private val preferences: AppPreferences) : ViewModel() {
    val settings = preferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    private val _message=MutableStateFlow<String?>(null);val message=_message.asStateFlow()
    fun dark(value: Boolean) = viewModelScope.launch { preferences.setDarkMode(value) }
    fun remoteUrl(value: String) = viewModelScope.launch { runCatching{preferences.setRemoteUrl(value)}.onSuccess{_message.value="Remote manifest saved."}.onFailure{_message.value=it.message?:"Invalid manifest URL."} }
    fun aboveBudget(value: Int) = viewModelScope.launch { preferences.setAboveBudgetPercent(value) }
}
