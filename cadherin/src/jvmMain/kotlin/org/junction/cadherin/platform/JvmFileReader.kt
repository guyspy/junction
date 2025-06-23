package org.junction.cadherin.parser

import java.io.File

internal actual fun readPlatformFile(filePath: String): String {
    return File(filePath).readText()
}