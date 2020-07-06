package com.banana.appwithgeolocation.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.banana.appwithgeolocation.model.entity.Point
import com.banana.appwithgeolocation.model.storage.PointRoomDatabase
import com.banana.appwithgeolocation.model.storage.Repository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: Repository = Repository(PointRoomDatabase.getDatabase(application).pointDao())
    private var _points: LiveData<List<Point>> = repository.getPoints()
    private var _selectedPointName: MutableLiveData<String> = MutableLiveData()
    var location: Location = Location("")

    val points: LiveData<List<Point>>
        get() = _points

    val selectedPointName: LiveData<String>
        get() = _selectedPointName

    private fun insertPoint(point: Point) = viewModelScope.launch(IO) {
        repository.insertPoint(point)
    }

    private fun updatePoint(point: Point) = viewModelScope.launch(IO) {
        repository.updatePoint(point)
    }

    fun deletePoint(point: Point) = viewModelScope.launch(IO) {
        repository.deletePoint(point)
    }

    fun deletePoints() = viewModelScope.launch(IO) {
        repository.deletePoints()
    }

    fun addPoint(point: Point) = checkPointName(point.name).apply {
        if (this == NameValidationResult.SUCCESS) insertPoint(point)
    }

    fun renamePoint(point: Point, name: String) = checkPointName(name).apply {
        if (this == NameValidationResult.SUCCESS) {
            point.name = name
            updatePoint(point)
        }
    }

    private fun checkPointName(name: String): NameValidationResult {
        return when (name.length) {
            in 0..5  -> NameValidationResult.TOO_SHORT
            in 6..25 -> {
                _points.value?.forEach { point ->
                    if (point.name == name) { return NameValidationResult.ALREADY_EXISTS }
                }
                NameValidationResult.SUCCESS
            }
            else -> NameValidationResult.TOO_LONG
        }
    }

    fun selectMarker(name: String) {
        _selectedPointName.value = name
    }

    fun checkDistance(accuracy: Int) : Boolean {
        _points.value?.forEach { point ->
            if (location.distanceTo(point.getLocation()) <= accuracy) { return false }
        }
        return true
    }


    enum class NameValidationResult {
        TOO_SHORT, TOO_LONG, ALREADY_EXISTS, SUCCESS
    }
}