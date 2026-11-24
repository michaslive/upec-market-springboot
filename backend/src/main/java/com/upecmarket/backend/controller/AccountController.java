package com.upecmarket.backend.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.upecmarket.backend.AuthUser;
import com.upecmarket.backend.service.ProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth/me")
@Tag(name = "Cuenta", description = "Gestión del perfil institucional")
public class AccountController {
	private final ProfileService profileService;

	public AccountController(ProfileService profileService) {
		this.profileService = profileService;
	}

	@PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Subir foto de perfil", description = "Acepta JPG, PNG o WebP de hasta 5 MB.")
	public Map<String, Object> uploadPhoto(@RequestAttribute("authenticatedUser") AuthUser user,
			@RequestParam("file") MultipartFile file) {
		return profileService.uploadPhoto(user, file);
	}
}
