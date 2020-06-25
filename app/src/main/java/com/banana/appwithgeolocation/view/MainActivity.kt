package com.banana.appwithgeolocation.view

import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.util.Navigation
import com.banana.appwithgeolocation.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var mViewModel: MainViewModel

    private val mFragmentsMap = hashMapOf(
        R.id.map_dest to "com.banana.appwithgeolocation.view.FragmentMap",
        R.id.list_dest to "com.banana.appwithgeolocation.view.FragmentList",
        R.id.settings_dest to "com.banana.appwithgeolocation.view.FragmentSettings"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        lifecycle.addObserver(Navigation.getNavigation())

        mViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        setupBottomNavigationBar()
    }

    private fun setupBottomNavigationBar() {
        Navigation.getNavigation().setContext(applicationContext)
        Navigation.getNavigation().setContainer(R.id.fragment_container)
        Navigation.getNavigation().setFragmentManager(supportFragmentManager)
        Navigation.getNavigation().setFragments(mFragmentsMap, R.id.map_dest)
        Navigation.getNavigation().setBottomNavigation(bottom_navigation)
    }

    override fun onBackPressed() {
        if (Navigation.getNavigation().shutdown()) {
            super.onBackPressed()
        }
    }
}
