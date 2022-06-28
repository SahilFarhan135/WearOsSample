package com.example.android.wearable.datalayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.wearable.*

class MainWearViewModel(
    application: Application
) :
    AndroidViewModel(application),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private var eventList = ArrayList<String>()

    private var _obEvents = MutableLiveData<ArrayList<String>>()

    val obEvents: LiveData<ArrayList<String>> = _obEvents


    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { dataEvent ->
            val items = ArrayList<String>()
            dataEvent.dataItem.also { item ->
                DataMapItem.fromDataItem(item).dataMap.apply {
                    if(getString("msg")!=null)
                       items.add(getString("msg"))
                }
            }
            eventList.addAll(items)
            _obEvents.postValue(eventList)
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        //eventList.add(messageEvent.data.toString())
        //_obEvents.postValue(eventList)
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        //eventList.add(capabilityInfo.toString())
       // _obEvents.postValue(eventList)
    }

}

