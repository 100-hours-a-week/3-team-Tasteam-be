package com.tasteam.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.global.utils.CursorCodec;

@Configuration
public class CursorCodecConfig {

	@Bean
	public CursorCodec cursorCodec(ObjectMapper objectMapper) {
		return new CursorCodec(objectMapper);
	}
}
