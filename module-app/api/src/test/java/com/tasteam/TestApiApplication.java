package com.tasteam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.tasteam.config.TestcontainersConfiguration;

@SpringBootApplication
public class TestApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(TestApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}
}
