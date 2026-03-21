package com.tasteam;

import org.springframework.boot.SpringApplication;

import com.tasteam.config.TestcontainersConfiguration;

public class TestApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}
}
