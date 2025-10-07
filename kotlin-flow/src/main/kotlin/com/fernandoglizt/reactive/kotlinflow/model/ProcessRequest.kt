package com.fernandoglizt.reactive.kotlinflow.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ProcessRequest(
    @JsonProperty("count")
    val count: Int,
    
    @JsonProperty("batch")
    val batch: Int,
    
    @JsonProperty("io_delay_ms")
    val ioDelayMs: Int = 50,
    
    @JsonProperty("downstream_url")
    val downstreamUrl: String = "http://slow-io:8080/slow"
)
