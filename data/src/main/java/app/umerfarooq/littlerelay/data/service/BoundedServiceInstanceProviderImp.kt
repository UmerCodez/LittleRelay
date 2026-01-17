/*
 *     This file is a part of LittleRelay (https://www.github.com/UmerCodez/LittleRelay)
 *     Copyright (C) 2026 Umer Farooq (umerfarooq2383@gmail.com)
 *
 *     LittleRelay is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LittleRelay is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LittleRelay. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package app.umerfarooq.littlerelay.data.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import app.umerfarooq.littlerelay.data.service.bluetooth.BleCentralServiceImp
import app.umerfarooq.littlerelay.data.service.bluetooth.BlePeripheralServiceImp
import app.umerfarooq.littlerelay.data.service.bridge.BleToMqttBridgeServiceImp
import app.umerfarooq.littlerelay.data.service.mqtt.MqttServiceImp
import app.umerfarooq.littlerelay.domain.service.BoundedServiceInstanceProvider
import app.umerfarooq.littlerelay.domain.service.bluetooth.BleCentralService
import app.umerfarooq.littlerelay.domain.service.bluetooth.BlePeripheralService
import app.umerfarooq.littlerelay.domain.service.bridge.BleToMqttBridgeService
import app.umerfarooq.littlerelay.domain.service.mqtt.MqttService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


class BoundedServiceInstanceProviderImp @Inject  constructor(
    @param:ApplicationContext val context: Context
): BoundedServiceInstanceProvider {


    private val mqttServiceBindHelper = ServiceBindHelper(context, MqttServiceImp::class.java)
    private val blePeripheralServiceBindHelper = ServiceBindHelper(context, BlePeripheralServiceImp::class.java)
    private val bleToMqttBridgeServiceBindHelper = ServiceBindHelper(context, BleToMqttBridgeServiceImp::class.java)
    private val bleCentralServiceBindHelper = ServiceBindHelper(context, BleCentralServiceImp::class.java)





    private val _mqttService: MutableStateFlow<MqttService?> = MutableStateFlow(null)
    override val mqttService = _mqttService.asStateFlow()


    private val _blePeripheralService: MutableStateFlow<BlePeripheralService?> = MutableStateFlow(null)
    override val blePeripheralService = _blePeripheralService.asStateFlow()

    private val _bleToMqttBridgeService: MutableStateFlow<BleToMqttBridgeService?> = MutableStateFlow(null)
    override val bleToMqttBridgeService = _bleToMqttBridgeService.asStateFlow()

    private val _bleCentralService: MutableStateFlow<BleCentralService?> = MutableStateFlow(null)
    override val bleCentralService = _bleCentralService.asStateFlow()



    init {
        mqttServiceBindHelper.onServiceConnected { binder ->
            val mqttServiceImp = (binder as MqttServiceImp.LocalBinder).service
            _mqttService.value = mqttServiceImp

        }

        blePeripheralServiceBindHelper.onServiceConnected { binder ->
            val bluetoothServiceImp = (binder as BlePeripheralServiceImp.LocalBinder).service
            _blePeripheralService.value = bluetoothServiceImp
        }

        bleToMqttBridgeServiceBindHelper.onServiceConnected{ binder ->
            val bleToMqttBridgeServiceImp = (binder as BleToMqttBridgeServiceImp.LocalBinder).service
            _bleToMqttBridgeService.value = bleToMqttBridgeServiceImp
        }

        bleCentralServiceBindHelper.onServiceConnected{ binder ->
            val bleCentralServiceImp = (binder as BleCentralServiceImp.LocalBinder).service
            _bleCentralService.value = bleCentralServiceImp
        }

        mqttServiceBindHelper.onServiceConnectionLost {
            _mqttService.value = null
        }

        blePeripheralServiceBindHelper.onServiceConnectionLost {
            _blePeripheralService.value = null
        }

        bleToMqttBridgeServiceBindHelper.onServiceConnectionLost{
            _bleToMqttBridgeService.value = null
        }

        bleCentralServiceBindHelper.onServiceConnectionLost {
            _bleCentralService.value = null
        }

        blePeripheralServiceBindHelper.bindToService()
        mqttServiceBindHelper.bindToService()
        bleToMqttBridgeServiceBindHelper.bindToService()
        bleCentralServiceBindHelper.bindToService()
    
    }

}



private class ServiceBindHelper(
    private val context: Context,
    private val service: Class<out Service>
) : ServiceConnection {

    private var bounded = false
    private var onServiceConnectedCallBack: ((IBinder) -> Unit)? = null
    private var onServiceConnectionLostCallBack: (() -> Unit)? = null



    fun bindToService() {
        Log.d(TAG, "bindToService()")
        val intent = Intent(context, service)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
        bounded = true
    }

    fun unBindFromService() {
        Log.d(TAG, "unBindFromService()")
        if (bounded) {
            context.unbindService(this)
            bounded = false
        }
    }

    fun onServiceConnected(callBack: ((IBinder) -> Unit)?) {
        onServiceConnectedCallBack = callBack
    }
    fun onServiceConnectionLost(callBack: (() -> Unit)?) {
        onServiceConnectionLostCallBack = callBack
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        Log.d(TAG, "onServiceConnected()")
        bounded = true

        onServiceConnectedCallBack?.invoke(binder)

    }

    /**  The onServiceDisconnected() method in Android is called when the connection to the service is unexpectedly disconnected,
     *   usually due to a crash or the service being killed by the system.
     *   This allows you to handle the situation and possibly attempt to reestablish the connection.
     *   onServiceDisconnected() method is not called when you explicitly call context.unbindService().
     *   It's only called when the connection to the service is unexpectedly lost, such as when the service process crashes or is killed by the system.
     *   */
    override fun onServiceDisconnected(name: ComponentName) {
        Log.d(TAG, "onServiceDisconnected()")

        onServiceConnectionLostCallBack?.invoke()

        bounded = false
        bindToService()
    }


    companion object {
        private val TAG: String = ServiceBindHelper::class.java.simpleName
    }

}