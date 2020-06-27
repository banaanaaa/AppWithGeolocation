package com.banana.appwithgeolocation.view

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.banana.appwithgeolocation.R

class FragmentSettings : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }
}