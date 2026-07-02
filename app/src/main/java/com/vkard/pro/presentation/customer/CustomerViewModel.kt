package com.vkard.pro.presentation.customer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vkard.pro.domain.model.Customer
import com.vkard.pro.domain.repository.CustomerRepository
import kotlinx.coroutines.launch

sealed interface CustomerUiState {
    object Idle : CustomerUiState
    object Loading : CustomerUiState
    object Success : CustomerUiState
    data class Error(val message: String) : CustomerUiState
}

class CustomerViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {
    
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")
    var companyName by mutableStateOf("")
    
    var uiState by mutableStateOf<CustomerUiState>(CustomerUiState.Idle)
        private set
        
    var customerList by mutableStateOf<List<Customer>>(emptyList())
        private set
        
    fun loadCustomers(userId: String, role: String) {
        viewModelScope.launch {
            customerRepository.getCustomers(userId, role)
                .onSuccess {
                    customerList = it
                }
        }
    }
    
    fun createCustomer(userId: String) {
        if (name.isBlank()) {
            uiState = CustomerUiState.Error("Customer name cannot be empty.")
            return
        }
        
        val customer = Customer(
            name = name.trim(),
            email = email.trim().ifBlank { null },
            phone = phone.trim().ifBlank { null },
            company_name = companyName.trim().ifBlank { null },
            created_by = userId
        )
        
        uiState = CustomerUiState.Loading
        viewModelScope.launch {
            customerRepository.createCustomer(customer)
                .onSuccess {
                    uiState = CustomerUiState.Success
                    // Reload list
                    loadCustomers(userId, "agent")
                    // Clear fields
                    name = ""
                    email = ""
                    phone = ""
                    companyName = ""
                }
                .onFailure {
                    uiState = CustomerUiState.Error(it.message ?: "Failed to create customer.")
                }
        }
    }
    
    fun searchCustomers(query: String, userId: String, role: String) {
        if (query.isBlank()) {
            loadCustomers(userId, role)
            return
        }
        
        viewModelScope.launch {
            customerRepository.searchCustomers(query, userId, role)
                .onSuccess {
                    customerList = it
                }
        }
    }
    
    fun clearState() {
        uiState = CustomerUiState.Idle
    }
}
