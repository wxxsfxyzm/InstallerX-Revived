package com.rosan.installer.data.app.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object FlexibleXapkVersionCodeSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) =
        encoder.encodeString(value)

    override fun deserialize(decoder: Decoder): String {
        return try {
            // Try to decode as a string
            decoder.decodeString()
        } catch (_: Exception) {
            try {
                // Try to decode as an int
                decoder.decodeLong().toString()
            } catch (e: Exception) {
                throw SerializationException("Expected string or int for FlexibleString, but got invalid token", e)
            }
        }
    }
}