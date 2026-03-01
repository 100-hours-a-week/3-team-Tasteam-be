package com.tasteam.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/admin/js/**")
			.addResourceLocations("classpath:/static/admin/")
			.setCachePeriod(0);
		registry.addResourceHandler("/admin/css/**")
			.addResourceLocations("classpath:/static/admin/")
			.setCachePeriod(0);
		registry.addResourceHandler("/admin/index.html")
			.addResourceLocations("classpath:/static/admin/")
			.setCachePeriod(0);
	}
}
