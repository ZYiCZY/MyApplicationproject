package com.example.myapplicationproject.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProfileViewModel : ViewModel() {

    private val _nickname = MutableLiveData<String>("使用者暱稱")
    val nickname: LiveData<String> = _nickname

    private val _defaultPrepMins = MutableLiveData<Int>(30)
    val defaultPrepMins: LiveData<Int> = _defaultPrepMins

    fun updateNickname(newNickname: String) {
        _nickname.value = newNickname
    }

    fun updateDefaultPrepMins(newMins: Int) {
        _defaultPrepMins.value = newMins
    }
}
