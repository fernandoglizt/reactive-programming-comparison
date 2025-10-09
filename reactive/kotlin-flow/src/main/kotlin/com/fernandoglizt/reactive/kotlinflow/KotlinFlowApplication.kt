package com.fernandoglizt.reactive.kotlinflow

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KotlinFlowApplication

fun main(args: Array<String>) {
	val logger = LoggerFactory.getLogger(KotlinFlowApplication::class.java)
	logger.info("Starting Kotlin Flow Application...")
	runApplication<KotlinFlowApplication>(*args)
	logger.info("Kotlin Flow Application started successfully!")
}
