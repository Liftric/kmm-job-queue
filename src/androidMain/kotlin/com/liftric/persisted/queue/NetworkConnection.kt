package com.liftric.persisted.queue

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.liftric.persisted.queue.rules.NetworkType

actual class NetworkConnection {
    fun getConnectionType(context: Context?): NetworkType {
        if (context == null) return NetworkType.NoConnection
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                return when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.Wifi
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.Cellular
                    else -> {
                        val connected = getCurrentConnectivityState(connectivityManager)
                        if (connected) return NetworkType.Undefined else NetworkType.NoConnection
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return when (activeNetworkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.Wifi
                ConnectivityManager.TYPE_MOBILE -> NetworkType.Cellular
                else -> {
                    val connected = getCurrentConnectivityState(connectivityManager)
                    if (connected) return NetworkType.Undefined else NetworkType.NoConnection
                }
            }
        }
        return NetworkType.Undefined
    }

    fun getCurrentConnectivityState(
        connectivityManager: ConnectivityManager
    ): Boolean {
        val connected =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1)
                connectivityManager.activeNetwork.let { network ->
                    connectivityManager.getNetworkCapabilities(network)
                        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        ?: false
                } else connectivityManager.activeNetworkInfo.let { networkInfo ->
                networkInfo?.isAvailable ?: false
            }
        return connected
    }
}
