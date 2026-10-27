package cn.yixiu.starter.core
import java.io.InputStream
import java.io.ByteArrayOutputStream

// InputStream.readNBytes 在 Android 33 才提供；此实现兼容 minSdk 26。
fun InputStream.readBounded(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (output.size() < limit) {
        val count = read(buffer, 0, minOf(buffer.size, limit - output.size()))
        if (count < 0) break
        if (count == 0) {
            val next = read()
            if (next < 0) break
            output.write(next)
        } else output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
