package cn.yixiu.starter.settings

import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

/** 只清理工具临时文件；不遍历账号缓存、用户文件或偏好，不跟随符号链接。 */
object ToolCache {
    private fun directory(cacheDir: File): Path {
        val base = cacheDir.toPath().toAbsolutePath().normalize()
        val target = base.resolve("tool-temp")
        require(!Files.isSymbolicLink(target)) { "临时缓存目录不可为链接" }
        return target
    }
    fun size(cacheDir: File): Long {
        val root = directory(cacheDir)
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return 0
        var total = 0L
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (attrs.isRegularFile) total += attrs.size()
                return FileVisitResult.CONTINUE
            }
        })
        return total
    }
    fun clear(cacheDir: File) {
        val root = directory(cacheDir)
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }
            override fun postVisitDirectory(dir: Path, error: java.io.IOException?): FileVisitResult {
                if (error != null) throw error
                if (dir != root) Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })
    }
}
