package com.example.android.wearable.datalayer

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.wearable.*


class MainMobileViewModel :
    ViewModel(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener ,SensorEventListener{

    private var eventList = ArrayList<String>()
    private var _obEvents = MutableLiveData<ArrayList<String>>()
    val obEvents: LiveData<ArrayList<String>> = _obEvents


    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { dataEvent ->
            val items = ArrayList<String>()
            dataEvent.dataItem.also { item ->
                DataMapItem.fromDataItem(item).dataMap.apply {
                    if(getString("step")!=null)
                       items.add(getString("step"))
                }
            }
            eventList.addAll(items)
            _obEvents.postValue(eventList)
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
     //   eventList.add(messageEvent.toString())
       // _obEvents.postValue(eventList)
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
      //  eventList.add(capabilityInfo.toString())
       // _obEvents.postValue(eventList)
    }

    override fun onSensorChanged(p0: SensorEvent?) {
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

}

