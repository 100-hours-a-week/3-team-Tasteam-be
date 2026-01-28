package com.tasteam;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.tasteam.config.TestStorageConfiguration;
import com.tasteam.config.TestcontainersConfiguration;

@Import({TestcontainersConfiguration.class, TestStorageConfiguration.class})
@SpringBootTest
class ApiApplicationTests {

	@Test
	void contextLoads() {}
}
