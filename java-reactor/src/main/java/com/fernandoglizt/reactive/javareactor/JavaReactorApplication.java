package com.fernandoglizt.reactive.javareactor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JavaReactorApplication {

	private static final Logger logger = LoggerFactory.getLogger(JavaReactorApplication.class);

	public static void main(String[] args) {
		logger.info("Starting Java Reactor Application...");
		SpringApplication.run(JavaReactorApplication.class, args);
		logger.info("Java Reactor Application started successfully!");
	}

}
