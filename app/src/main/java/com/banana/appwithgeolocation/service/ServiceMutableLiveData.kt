package com.banana.appwithgeolocation.service

import android.content.Context
import android.location.Location
import androidx.lifecycle.MutableLiveData

class ServiceMutableLiveData(context: Context) : MutableLiveData<Location>() {

    private var context: Context? = context

    private var location = Location("")

    private val listener = { location: Location ->
        value = location
    }

    companion object {
        private var instance: ServiceMutableLiveData? = null

        fun getInstance(context: Context): ServiceMutableLiveData? {
            if (instance == null) instance = ServiceMutableLiveData(context.applicationContext)
            return instance
        }
    }
}