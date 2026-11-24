package com.upecmarket.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.upecmarket.backend.ApiException;

@Service
public class ImageStorageService {
	private static final Map<String, String> EXTENSIONS = Map.of(
			"image/jpeg", ".jpg",
			"image/png", ".png",
			"image/webp", ".webp");
	private final long maximumBytes;
	private final ObjectStorageService objectStorage;

	public ImageStorageService(
			@Value("${app.upload.directory}") String uploadDirectory,
			@Value("${app.upload.max-bytes}") long maximumBytes,
			ObjectStorageService objectStorage) {
		this.maximumBytes = maximumBytes;
		this.objectStorage = objectStorage;
	}

	public String store(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw ApiException.badRequest("Selecciona una imagen para subir.");
		}
		if (file.getSize() > maximumBytes) {
			throw ApiException.badRequest("La imagen supera el límite de 5 MB.");
		}
		String extension = EXTENSIONS.get(file.getContentType());
		if (extension == null) {
			throw ApiException.badRequest("Formato no permitido. Usa JPG, PNG o WebP.");
		}
		String filename = UUID.randomUUID() + extension;
		try {
			if (!matchesImageSignature(file, file.getContentType())) {
				throw ApiException.badRequest("El contenido del archivo no coincide con una imagen JPG, PNG o WebP.");
			}
			return objectStorage.store(file, "products/" + filename);
		} catch (IOException exception) {
			throw new IllegalStateException("No se pudo guardar la imagen en el servidor.", exception);
		}
	}

	private boolean matchesImageSignature(MultipartFile file, String contentType) throws IOException {
		try (InputStream input = file.getInputStream()) {
			byte[] header = input.readNBytes(12);
			return switch (contentType) {
				case "image/jpeg" -> header.length >= 3 && (header[0] & 0xff) == 0xff
						&& (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
				case "image/png" -> header.length >= 8 && (header[0] & 0xff) == 0x89
						&& header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
						&& header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a;
				case "image/webp" -> header.length >= 12 && header[0] == 'R' && header[1] == 'I'
						&& header[2] == 'F' && header[3] == 'F' && header[8] == 'W'
						&& header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
				default -> false;
			};
		}
	}
}
