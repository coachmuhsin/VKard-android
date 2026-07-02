package com.vkard.pro.domain.repository

import com.vkard.pro.domain.model.Customer

interface CustomerRepository {
    suspend fun getCustomers(userId: String, role: String): Result<List<Customer>>
    suspend fun createCustomer(customer: Customer): Result<Customer>
    suspend fun searchCustomers(query: String, userId: String, role: String): Result<List<Customer>>
}
