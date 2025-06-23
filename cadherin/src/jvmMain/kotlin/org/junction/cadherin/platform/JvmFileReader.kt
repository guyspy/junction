package org.junction.cadherin.platform

import java.io.File

actual fun readPlatformFile(filePath: String): String {
    return File(filePath).readText()
}