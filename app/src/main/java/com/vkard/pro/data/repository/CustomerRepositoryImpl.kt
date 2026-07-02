package com.vkard.pro.data.repository

import com.vkard.pro.data.remote.SupabaseClientProvider
import com.vkard.pro.domain.model.Customer
import com.vkard.pro.domain.repository.CustomerRepository
import io.github.jan.supabase.postgrest.postgrest

class CustomerRepositoryImpl : CustomerRepository {
    
    private val supabase = SupabaseClientProvider.client
    
    override suspend fun getCustomers(userId: String, role: String): Result<List<Customer>> {
        return runCatching {
            supabase.postgrest["customers"]
                .select()
                .decodeList<Customer>()
        }
    }
    
    override suspend fun createCustomer(customer: Customer): Result<Customer> {
        return runCatching {
            val response = supabase.postgrest["customers"]
                .insert(customer)
            
            response.decodeList<Customer>().firstOrNull() 
                ?: throw Exception("Failed to insert customer.")
        }
    }
    
    override suspend fun searchCustomers(query: String, userId: String, role: String): Result<List<Customer>> {
        return runCatching {
            supabase.postgrest["customers"]
                .select {
                    filter {
                        ilike("name", "%$query%")
                    }
                }.decodeList<Customer>()
        }
    }
}
