package com.csm.kitchenguard.utils.result

/**
 * Wrapper standar untuk menangani state dari operasi asinkron.
 * Digunakan untuk mengkomunikasikan data, error, atau status loading
 * dari layer Data (Repository) ke layer UI (ViewModel).
 */
sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
