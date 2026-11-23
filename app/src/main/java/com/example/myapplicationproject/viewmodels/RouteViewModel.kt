package com.example.myapplicationproject.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplicationproject.repository.RouteRepository
import com.example.myapplicationproject.repository.RouteResult
import com.example.myapplicationproject.services.PlaceInfo
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 路線查詢 ViewModel：保存畫面狀態並處理快切時過時請求的取消
 */
class RouteViewModel(application: Application) : AndroidViewModel(application) {

    private val routeRepository = RouteRepository(application)
    private var computeJob: Job? = null

    private val _routeResult = MutableLiveData<RouteResult?>()
    val routeResult: LiveData<RouteResult?> = _routeResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _selectedOrigin = MutableLiveData<PlaceInfo>()
    val selectedOrigin: LiveData<PlaceInfo> = _selectedOrigin

    private val _selectedDestination = MutableLiveData<PlaceInfo>()
    val selectedDestination: LiveData<PlaceInfo> = _selectedDestination

    init {
        // 預設目的地：嘉義大學蘭潭校區；預設起點：嘉義火車站
        _selectedOrigin.value = PlaceInfo("p_chiayi_station", "嘉義火車站", "嘉義市西區中山路528號", LatLng(23.4791, 120.4411))
        _selectedDestination.value = PlaceInfo("p_ncyu_lantan", "嘉義大學蘭潭校區", "嘉義市東區學府路300號", LatLng(23.4697, 120.4855))
    }

    fun setOrigin(origin: PlaceInfo) {
        _selectedOrigin.value = origin
    }

    fun setDestination(destination: PlaceInfo) {
        _selectedDestination.value = destination
    }

    fun calculateRoute(
        transportMode: String,
        meetingTime: Long,
        prepMins: Int,
        bufferMins: Int
    ) {
        val origin = _selectedOrigin.value ?: return
        val destination = _selectedDestination.value ?: return

        // 取消先前的舊請求 (避免使用者快速切換交通方式時舊結果覆蓋最新選擇)
        computeJob?.cancel()

        _isLoading.value = true
        computeJob = viewModelScope.launch {
            try {
                val result = routeRepository.computeRoute(
                    origin = origin,
                    destination = destination,
                    transportMode = transportMode,
                    meetingTime = meetingTime,
                    prepMins = prepMins,
                    bufferMins = bufferMins
                )
                _routeResult.value = result
            } catch (e: Exception) {
                // 處理發生的例外
            } finally {
                _isLoading.value = false
            }
        }
    }
}
