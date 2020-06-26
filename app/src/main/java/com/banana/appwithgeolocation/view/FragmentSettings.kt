package com.banana.appwithgeolocation.view

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.utils.FilterNumber

class FragmentSettings : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

//        val mAccuracySeekBar: SeekBarPreference? = preferenceManager.findPreference("accuracy")
        val mEditTextSampleRate: EditTextPreference? = preferenceManager.findPreference("sample_rate")


        mEditTextSampleRate!!.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
            it.filters = arrayOf<InputFilter>(
                FilterNumber(
                    1,
                    30
                )
            )
        }
    }
}