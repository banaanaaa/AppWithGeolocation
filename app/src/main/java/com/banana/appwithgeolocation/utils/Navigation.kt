package com.banana.appwithgeolocation.utils

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*
import kotlin.collections.HashMap

class Navigation : LifecycleObserver {
    companion object {
        private var sInstance = Navigation()
        fun getNavigation() = sInstance
    }

    private var mContext: Context? = null
    private var mFragments = HashMap<Int, Fragment>()
    private var mNames = HashMap<Int, String>()
    private var mOldNames = HashMap<Int, String>()
    private var mHomeFragmentId: Int? = null
    private var mFragmentManager: FragmentManager? = null
    private var mContainer: Int? = null
    private lateinit var mBottomNavigation: BottomNavigationView

    private var mActiveFragment: Fragment? = null
    private var mActiveId: Int? = null
    private var mBackStack = LinkedHashSet<Int>()

    fun setContext(context: Context) {
        mContext = context
    }

    fun setFragments(map: HashMap<Int, String>, id: Int) {
        mNames = map
        mHomeFragmentId = id
    }

    fun setBottomNavigation(bottom: BottomNavigationView) {
        mBottomNavigation = bottom

        mBottomNavigation.setOnNavigationItemSelectedListener { item ->
            showFragmentById(item.itemId)
            addToBackStack(item.itemId)
            return@setOnNavigationItemSelectedListener true
        }
    }

    fun setFragmentManager(fragmentManager: FragmentManager?) {
        mFragmentManager = fragmentManager
    }

    fun setContainer(container: Int) {
        mContainer = container
    }

    private fun createFragmentFromClassName(name: String) : Fragment {
        return Class.forName(name).getConstructor().newInstance() as Fragment
    }

    private fun createFragmentById(id: Int) {
        if (mFragments[id] == null) {
            mFragments[id] = createFragmentFromClassName(mNames[id]!!)
        }
    }

    private fun showFragmentById(id: Int) {
        if (mFragmentManager == null) {
            return
        }
        if (mActiveFragment != null && mActiveFragment == mFragments[id]) {
            return
        }
        val transaction = mFragmentManager!!.beginTransaction()
        if (mActiveFragment != null) {
            transaction.hide(mActiveFragment!!)
        }
        if (mFragmentManager!!.findFragmentByTag("bottom#$id") == null) {
            createFragmentById(id)
            transaction.add(mContainer!!, mFragments[id]!!, "bottom#$id")
        } else {
            transaction.show(mFragments[id]!!)
        }
        mActiveFragment = mFragments[id]
        mActiveId = id
        transaction.commit()
    }

    fun shutdown(): Boolean {
        if (mBackStack.isNotEmpty()) {
            var id = mBackStack.last()
            mBackStack.remove(id)
            id = if (mBackStack.size == 0) {
                if (id == mHomeFragmentId) {
                    mBackStack.add(id)
                    return true
                }
                mBackStack.add(mHomeFragmentId as Int)
                mHomeFragmentId as Int
            } else {
                mBackStack.last()
            }
            setSelectedItem(id)
            return false
        }
        return true
    }

    fun addToBackStack(id: Int) {
        if (mBackStack.contains(id)) {
            mBackStack.remove(id)
        }
        mBackStack.add(id)
    }

    fun setSelectedItem(id: Int) {
        mBottomNavigation.selectedItemId = id
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        if (mOldNames.isNotEmpty()) {
            val tmpBack = mBackStack
            mBackStack.forEach { back ->
                mNames.forEach { new ->
                    if (mOldNames[back] == new.value) {
                        tmpBack.add(new.key)
                        createFragmentById(new.key)
                        mFragmentManager!!
                            .beginTransaction()
                            .add(mContainer!!, mFragments[new.key]!!, "bottom#${new.key}")
                            .hide(mFragments[new.key]!!)
                            .commit()
                        mActiveId = new.key
                    }
                }
            }
            mBackStack = tmpBack
            mActiveFragment = mFragments[mBackStack.last()]
            setSelectedItem(mActiveId as Int)
        }
        if (mActiveFragment == null) {
            setSelectedItem(mHomeFragmentId!!)
            addToBackStack(mHomeFragmentId!!)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        mOldNames = mNames
        mActiveFragment = null
        mFragmentManager = null
    }
}
