package com.vkard.pro.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object SafeGalleryImagesSerializer : KSerializer<List<String>> {
    private val delegate = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: List<String>) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): List<String> {
        return try {
            if (decoder is JsonDecoder) {
                val element = decoder.decodeJsonElement()
                if (element is JsonArray) {
                    element.mapNotNull { jsonElement ->
                        try {
                            jsonElement.jsonPrimitive.contentOrNull
                        } catch (e: Exception) {
                            null
                        }
                    }
                } else {
                    emptyList()
                }
            } else {
                delegate.deserialize(decoder)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
