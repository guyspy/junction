package org.junction.cadherin.parser

internal actual fun readPlatformFile(filePath: String): String {
    // JS environment, file content needs to be passed from external source
    throw UnsupportedOperationException("File reading not supported in JS environment. Use parseFromString instead.")
}