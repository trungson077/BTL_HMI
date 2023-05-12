package com.example.nextface_android.api

interface NFCallback<T> {
    fun onSuccess(response: T)
    fun onFailure(message: String)
}