@file:OptIn(ExperimentalEncodingApi::class)

package korlibs.image.core

import kotlin.io.encoding.*
import korlibs.io.compression.*
import korlibs.io.compression.deflate.*

object StbiCoreImageFormatProvider : CoreImageFormatProvider {
    override suspend fun info(data: ByteArray): CoreImageInfo {
        return StbiWASM.stbi_info_from_memory(data)
    }

    override suspend fun decode(data: ByteArray): CoreImage {
        return StbiWASM.stbi_load_from_memory(data)
    }

    override suspend fun encode(image: CoreImage, format: CoreImageFormat, level: Double): ByteArray {
        val iformat = when (format.name.lowercase()) {
            "png" -> StbiWASM.FORMAT_PNG
            "bmp" -> StbiWASM.FORMAT_BMP
            "tga" -> StbiWASM.FORMAT_TGA
            "jpg", "jpeg" -> StbiWASM.FORMAT_JPEG
            else -> StbiWASM.FORMAT_PNG
        }
        return StbiWASM.stbi_write_to_memory(image.to32(), iformat, 10)
    }

}

open class StbiWASM(bytes: ByteArray) : korlibs.wasm.WASMLib(bytes) {
    companion object : StbiWASM(STBI_WASM_BYTES) {
        const val FORMAT_PNG = 0
        const val FORMAT_BMP = 1
        const val FORMAT_TGA = 2
        const val FORMAT_JPEG = 3
    }

    private fun malloc(size: Int): Int = invokeFuncInt("malloc", size)
    private fun heap_reset(): Unit = invokeFuncUnit("heap_reset")
    private fun stbi_info_from_memory(
        buffer: Int, len: Int, xPtr: Int, yPtr: Int, compPtr: Int
    ): Int = invokeFuncInt("stbi_info_from_memory", buffer, len, xPtr, yPtr, compPtr)
    private fun stbi_load_from_memory(
        ptr: Int, size: Int, wPtr: Int, hPtr: Int, nPtr: Int, channels: Int
    ): Int = invokeFuncInt("stbi_load_from_memory", ptr, size, wPtr, hPtr, nPtr, channels)
    private fun stbi_write_to_memory(
        type: Int, inp: Int, w: Int, h: Int, out: Int, outSize: Int, quality: Int,
    ): Int = invokeFuncInt("stbi_write_to_memory", type, inp, w, h, out, outSize, quality)

    fun stbi_info_from_memory(bytes: ByteArray): CoreImageInfo {
        heap_reset()
        val ptr = allocBytes(bytes)
        val infoPtr = allocBytes(4 * 3)
        //println("infoPtr=$infoPtr")
        val result = stbi_info_from_memory(ptr, bytes.size, infoPtr + 0, infoPtr + 4, infoPtr + 8)
        if (result == 0) error("Can't get info from image")
        val infos = readInts(infoPtr, 3)
        return CoreImageInfo(width = infos[0], height = infos[1], bpp = infos[2] * 8)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun stbi_load_from_memory(bytes: ByteArray): CoreImage32 {
        heap_reset()
        val infoPtr = allocInts(3)
        val ptr = allocBytes(bytes)
        //println("ptr=$ptr")
        //println("infoPtr=$infoPtr")
        val dataPtr = stbi_load_from_memory(ptr, bytes.size, infoPtr + 0, infoPtr + 4, infoPtr + 8, 4)
        if (dataPtr == 0) error("Can't decode image")
        val infos = readInts(infoPtr, 3)
        val w = infos[0]
        val h = infos[1]
        val n = infos[2]
        val data = readInts(dataPtr, w * h)
        freeBytes(ptr, infoPtr, dataPtr)
        return CoreImage32(w, h, data, premultiplied = false).premultiplied()
        //return CoreImage32(w, h, data)
    }

    fun stbi_write_to_memory(image: CoreImage32, format: Int, quality: Int): ByteArray {
        heap_reset()
        val dataPtr = allocInts(image.depremultiplied().data)
        //val dataPtr = allocInts(image.data)
        val outSize = image.width * image.height * 4 + 1024
        val outPtr = allocBytes(outSize)
        val res = stbi_write_to_memory(format, dataPtr, image.width, image.height, outPtr, outSize, quality)
        //println("ENCODE: format=$format, res=$res, image=$image, outPtr=$outPtr, outSize=$outSize, quality=$quality")
        val out = readBytes(outPtr, res)
        freeBytes(dataPtr, outPtr)
        return out
    }
}

private val STBI_WASM_BYTES = Base64.decode("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")
    .uncompress(ZLib)
