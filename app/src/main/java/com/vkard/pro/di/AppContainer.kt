package com.vkard.pro.di

import android.content.Context
import com.vkard.pro.data.local.SecureSessionManager
import com.vkard.pro.data.repository.AuthRepositoryImpl
import com.vkard.pro.data.repository.CardRepositoryImpl
import com.vkard.pro.data.repository.CustomerRepositoryImpl
import com.vkard.pro.domain.repository.AuthRepository
import com.vkard.pro.domain.repository.CardRepository
import com.vkard.pro.domain.repository.CustomerRepository

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
}
