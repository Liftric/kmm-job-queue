package com.liftric.persisted.queue

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.liftric.persisted.queue.rules.ConnectionType

actual typealias Context = Context

actual class NetworkConnection {
    actual fun getConnectionType(context: Context?): ConnectionType {
        if (context == null) return ConnectionType.NotConnected()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                return when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.Wifi()
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.Cellular()
                    else -> {
                        val connected = getCurrentConnectivityState(connectivityManager)
                        if (connected) return ConnectionType.Undefined() else ConnectionType.NotConnected()
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return when (activeNetworkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> ConnectionType.Wifi()
                ConnectivityManager.TYPE_MOBILE -> ConnectionType.Cellular()
                else -> {
                    val connected = getCurrentConnectivityState(connectivityManager)
                    if (connected) return ConnectionType.Undefined() else ConnectionType.NotConnected()
                }
            }
        }
        return ConnectionType.NotConnected()
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
