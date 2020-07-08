package com.banana.appwithgeolocation.utils

class Constants {
    companion object {
        const val SERVICE_START = "com.appwithgeolocation.service.START"
        const val SERVICE_STOP  = "com.appwithgeolocation.service.STOP"

        const val NOTIFICATION_ACTION_TAP_ON_SHORT_NOTIFY = "com.appwithgeolocation.notify.action.OPEN.SHORT_NOTIFY"
        const val NOTIFICATION_ACTION_TAP_ON_FOREGROUND   = "com.appwithgeolocation.notify.action.OPEN.FOREGROUND"
        const val NOTIFICATION_ACTION_STOP_SERVICE        = "com.appwithgeolocation.notify.action.STOP.SERVICE"
        const val NOTIFICATION_CHANNEL_ID_SHORT_NOTIFY    = "com.appwithgeolocation.notify.channel.ID.SHORT_NOTIFY"
        const val NOTIFICATION_CHANNEL_ID_FOREGROUND      = "com.appwithgeolocation.notify.channel.ID.FOREGROUND"

        const val SETTINGS_KEY_SAMPLE_RATE = "com.appwithgeolocation.settings.SAMPLE_RATE"
        const val SETTINGS_KEY_ACCURACY    = "com.appwithgeolocation.settings.ACCURACY"
        const val SETTINGS_KEY_SWITCH      = "com.appwithgeolocation.settings.SERVICE_SWITCH"
    }
}