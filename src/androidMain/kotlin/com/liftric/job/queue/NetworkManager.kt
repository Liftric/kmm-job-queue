package com.liftric.job.queue

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@SuppressLint("MissingPermission")
actual class NetworkManager(context: Context) {
    private var connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val connectionPublisher = MutableStateFlow(getCurrentNetworkConnection())
    val network: Flow<NetworkState?> = connectionPublisher

    private fun getCurrentNetworkConnection(): NetworkState? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            postAndroidMNetworkConnection(connectivityManager)
        } else {
            preAndroidMNetworkConnection(connectivityManager)
        }

    @TargetApi(Build.VERSION_CODES.M)
    private fun postAndroidMNetworkConnection(connectivityManager: ConnectivityManager): NetworkState? {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return getNetworkConnection(capabilities)
    }

    @Suppress("DEPRECATION")
    private fun preAndroidMNetworkConnection(connectivityManager: ConnectivityManager): NetworkState? =
        when (connectivityManager.activeNetworkInfo?.type) {
            null -> null
            ConnectivityManager.TYPE_WIFI -> NetworkState.WIFI
            else -> NetworkState.MOBILE
        }

    private fun getNetworkConnection(capabilities: NetworkCapabilities?): NetworkState? =
        when {
            capabilities == null -> null
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    && !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> null
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) -> null
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkState.MOBILE
            else -> null
        }
}
