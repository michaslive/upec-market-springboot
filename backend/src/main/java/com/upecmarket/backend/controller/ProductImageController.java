package com.upecmarket.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.upecmarket.backend.AuthUser;
import com.upecmarket.backend.dto.ProductResponse;
import com.upecmarket.backend.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Productos", description = "Publicaciones y catálogo")
public class ProductImageController {
	private final ProductService productService;

	public ProductImageController(ProductService productService) {
		this.productService = productService;
	}

	@PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Subir imagen del producto", description = "Acepta JPG, PNG o WebP de hasta 5 MB. Cambiar la imagen devuelve el producto a revisión.")
	public ProductResponse uploadImage(@PathVariable long id,
			@RequestAttribute("authenticatedUser") AuthUser user,
			@RequestParam("file") MultipartFile file) {
		return productService.uploadImage(id, user, file);
	}
}
