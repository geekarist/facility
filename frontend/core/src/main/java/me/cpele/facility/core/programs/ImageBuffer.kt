package me.cpele.facility.core.programs

import kotlinx.serialization.Serializable

@Serializable
data class ImageBuffer(val array: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageBuffer

        return array.contentEquals(other.array)
    }

    override fun hashCode(): Int {
        return array.contentHashCode()
    }

    override fun toString(): String {
        return "ImageBuffer(array=${array.contentToString()})"
    }
}