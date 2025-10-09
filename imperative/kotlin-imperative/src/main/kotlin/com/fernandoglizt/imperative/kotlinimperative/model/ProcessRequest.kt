package com.fernandoglizt.imperative.kotlinimperative.model

data class ProcessRequest(
    val count: Int,
    val batch: Int,
    val ioDelayMs: Int = 0,
    val downstreamUrl: String? = null
) {
    init {
        require(count > 0) { "count must be > 0" }
        require(batch > 0) { "batch must be > 0" }
    }

    fun getDownstreamUrlOrDefault(): String =
        downstreamUrl?.takeIf { it.isNotBlank() } ?: "http://slow-io:8080/slow"

    fun getIoDelayMsNormalized(): Int = maxOf(0, ioDelayMs)
}

