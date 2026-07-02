package com.vkard.pro.domain.repository

import com.vkard.pro.domain.model.DigitalCard
import com.vkard.pro.domain.model.RevenueLedger

interface CardRepository {
    suspend fun getCards(userId: String, role: String): Result<List<DigitalCard>>
    suspend fun getCardBySlug(slug: String): Result<DigitalCard?>
    suspend fun createCard(card: DigitalCard, creatorRole: String): Result<String>
    suspend fun updateCard(card: DigitalCard): Result<Unit>
    suspend fun deleteCard(cardId: String): Result<Unit>
    suspend fun getLedger(userId: String, role: String): Result<List<RevenueLedger>>
    suspend fun uploadMedia(base64Image: String): Result<String>
}
