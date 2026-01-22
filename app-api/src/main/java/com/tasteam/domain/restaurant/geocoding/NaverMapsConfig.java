package com.tasteam.domain.restaurant.geocoding;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(NaverMapsProperties.class)
@Configuration
public class NaverMapsConfig {}
