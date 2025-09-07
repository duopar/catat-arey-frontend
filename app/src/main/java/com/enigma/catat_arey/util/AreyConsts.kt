package com.enigma.catat_arey.util

object AreyConsts {
    // eventually exposed in http traffic and decompilation,
    // but might migrate it to native module for more "security"
    fun getBaseApiUrl() = "https://catat-arey-130212893572.asia-southeast2.run.app"
    fun getBaseApiKey() = "Aman0fsacuD0auiPa8pwP5zw3lNGpMS87FXS2qYJrnhVFJTTfx" // development token

    fun getTokenSeparator() = "arey_token"
}

enum class AreyUserRole {
    Owner,
    Employee,
    Unknown
}