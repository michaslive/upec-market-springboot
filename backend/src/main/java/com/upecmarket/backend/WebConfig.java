package com.upecmarket.backend;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final String[] allowedOrigins;
	private final AuthInterceptor authInterceptor;
	private final String uploadDirectory;

	public WebConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins,
			@Value("${app.upload.directory}") String uploadDirectory, AuthInterceptor authInterceptor) {
		this.allowedOrigins = allowedOrigins;
		this.authInterceptor = authInterceptor;
		this.uploadDirectory = uploadDirectory;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins)
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*");
	}

	@Override
	public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
		registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
	}

	@Override
	public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
		String location = Paths.get(uploadDirectory).toAbsolutePath().normalize().toUri().toString();
		registry.addResourceHandler("/uploads/**").addResourceLocations(location);
	}
}
