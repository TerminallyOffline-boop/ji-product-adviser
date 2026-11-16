package com.jitelecom.productadviser.data.seed

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class CatalogInitializationState(
    val loading: Boolean = true,
    val error: String? = null
)

@Singleton
class CatalogInitializationStatus @Inject constructor() {
    private val mutableState = MutableStateFlow(CatalogInitializationState())
    val state = mutableState.asStateFlow()

    fun markLoading() {
        mutableState.value = CatalogInitializationState(loading = true)
    }

    fun markReady() {
        mutableState.value = CatalogInitializationState(loading = false)
    }

    fun markFailed(error: Exception) {
        val detail = error.message?.takeIf(String::isNotBlank)
        mutableState.value = CatalogInitializationState(
            loading = false,
            error = if (detail == null) error.javaClass.simpleName else "${error.javaClass.simpleName}: $detail"
        )
    }
}
