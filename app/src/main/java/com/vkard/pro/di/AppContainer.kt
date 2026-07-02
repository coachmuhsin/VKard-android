package com.vkard.pro.di

import android.content.Context
import com.vkard.pro.data.local.SecureSessionManager
import com.vkard.pro.data.repository.AuthRepositoryImpl
import com.vkard.pro.data.repository.CardRepositoryImpl
import com.vkard.pro.data.repository.CustomerRepositoryImpl
import com.vkard.pro.domain.repository.AuthRepository
import com.vkard.pro.domain.repository.CardRepository
import com.vkard.pro.domain.repository.CustomerRepository

import com.vkard.pro.data.remote.UpdateApiService
import com.vkard.pro.data.repository.UpdateRepositoryImpl
import com.vkard.pro.domain.repository.UpdateRepository
import com.vkard.pro.presentation.update.UpdateManager
import com.vkard.pro.presentation.update.UpdateViewModel

class AppContainer(context: Context) {
    val sessionManager = SecureSessionManager(context)
    
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(sessionManager)
    }
    
    val cardRepository: CardRepository by lazy {
        CardRepositoryImpl(sessionManager)
    }
    
    val customerRepository: CustomerRepository by lazy {
        CustomerRepositoryImpl()
    }

    val updateApiService = UpdateApiService()
    
    val updateRepository: UpdateRepository by lazy {
        UpdateRepositoryImpl(context, updateApiService)
    }
    
    val updateManager by lazy {
        UpdateManager(context)
    }
    
    val updateViewModelFactory: () -> UpdateViewModel = {
        UpdateViewModel(updateRepository, updateManager)
    }
}
