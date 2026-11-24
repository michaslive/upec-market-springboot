package com.upecmarket.backend;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, String>> handleApiException(ApiException exception) {
		return ResponseEntity.status(exception.status()).body(Map.of("error", exception.getMessage()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> handleConflict(DataIntegrityViolationException exception) {
		return ResponseEntity.status(409).body(Map.of("error", "La operación entra en conflicto con los datos existentes."));
	}

	@ExceptionHandler(EmptyResultDataAccessException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(EmptyResultDataAccessException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "El recurso solicitado no existe."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.distinct()
				.reduce((left, right) -> left + "; " + right)
				.orElse("Los datos enviados no son válidos.");
		return ResponseEntity.badRequest().body(Map.of("error", message));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
				.body(Map.of("error", "El archivo supera el límite de carga permitido."));
	}
}
