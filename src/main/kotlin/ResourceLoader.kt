package org.jetos.neu.eams


import java.io.InputStreamReader

object ResourceLoader {
    fun loadJavaScriptFile(fileName: String): String {
        val classLoader = this::class.java.classLoader
        val inputStream = classLoader.getResourceAsStream(fileName)
            ?: throw IllegalArgumentException("File not found: $fileName")
        return InputStreamReader(inputStream).readText()
    }
}
