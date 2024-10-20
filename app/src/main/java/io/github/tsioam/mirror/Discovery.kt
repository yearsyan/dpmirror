package io.github.tsioam.mirror

import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import io.github.tsioam.shared.Constants
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Discovery(context: Context) : NsdManager.DiscoveryListener {

    companion object {
        @Volatile
        private var instance: Discovery? = null

        fun initialize(ctx: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = Discovery(ctx)
                    }
                }
            }
        }

        fun getInstance(): Discovery {
            return instance ?: throw IllegalStateException("Discovery is not initialized, call initialize() first.")
        }
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceSet = ConcurrentHashMap<String,NsdServiceInfo>()
    private var serviceChangeListeners = HashSet<(services: Map<String,NsdServiceInfo>) -> Unit>()
    private val executor = ThreadPoolExecutor(2 , 4, 60, TimeUnit.SECONDS, LinkedBlockingQueue())
    init {
        nsdManager.discoverServices(Constants.NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this)
    }


    override fun onStartDiscoveryFailed(p0: String?, p1: Int) {

    }

    override fun onStopDiscoveryFailed(p0: String?, p1: Int) {

    }

    override fun onDiscoveryStarted(p0: String?) {

    }

    override fun onDiscoveryStopped(p0: String?) {

    }

    override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
        if (serviceInfo == null) {
            return
        }
        Log.d("NSD", "Service Found: ${serviceInfo.serviceName} ${serviceInfo.host} ${serviceInfo.port}")
        if (serviceInfo.serviceType == Constants.NSD_SERVICE_TYPE) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val cb = object: NsdManager.ServiceInfoCallback {
                    override fun onServiceInfoCallbackRegistrationFailed(p0: Int) {
                        Log.e("ERR", "fail to register service callback")
                    }

                    override fun onServiceUpdated(result: NsdServiceInfo) {
                        serviceSet[serviceInfo.serviceName] = result
                        synchronized(serviceChangeListeners) {
                            serviceChangeListeners.forEach{
                                it(serviceSet)
                            }
                        }
                    }

                    override fun onServiceLost() {
                        serviceSet.remove(serviceInfo.serviceName)
                        synchronized(serviceChangeListeners) {
                            serviceChangeListeners.forEach{
                                it(serviceSet)
                            }
                        }
                        nsdManager.unregisterServiceInfoCallback(this)
                    }

                    override fun onServiceInfoCallbackUnregistered() {
                    }

                }
                nsdManager.registerServiceInfoCallback(serviceInfo, executor, cb)
            } else {
                nsdManager.resolveService(serviceInfo, object: NsdManager.ResolveListener{
                    override fun onResolveFailed(p0: NsdServiceInfo?, p1: Int) {
                        Log.e("ERR", "fail to resolve service")
                    }

                    override fun onServiceResolved(result: NsdServiceInfo?) {
                        if (result == null) {
                            return
                        }
                        serviceSet[serviceInfo.serviceName] = result
                        synchronized(serviceChangeListeners) {
                            serviceChangeListeners.forEach{
                                it(serviceSet)
                            }
                        }
                    }
                })
            }

        }
    }

    override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
        if (serviceInfo != null) {
            serviceSet.remove(serviceInfo.serviceName)
            synchronized(serviceChangeListeners) {
                serviceChangeListeners.forEach{
                    it(serviceSet)
                }
            }
        }
    }

    fun registerServiceChangeListener(listener: (services: Map<String,NsdServiceInfo>) -> Unit) {
        synchronized(serviceChangeListeners) {
            serviceChangeListeners.add(listener)
        }
    }

    fun unregisterServiceChangeListener(listener: (services: Map<String,NsdServiceInfo>) -> Unit) {
        synchronized(serviceChangeListeners) {
            serviceChangeListeners.remove(listener)
        }
    }

    fun getServices(): List<NsdServiceInfo> {
        return ArrayList(serviceSet.values)
    }
}