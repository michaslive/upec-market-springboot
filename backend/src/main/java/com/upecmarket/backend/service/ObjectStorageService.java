package com.upecmarket.backend.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class ObjectStorageService {
	private final String bucket;
	private final String publicUrl;
	private final Path uploadDirectory;
	private final S3Client s3Client;

	public ObjectStorageService(
			@Value("${app.storage.bucket}") String bucket,
			@Value("${app.storage.endpoint:}") String endpoint,
			@Value("${app.storage.region:us-east-1}") String region,
			@Value("${app.storage.access-key:}") String accessKey,
			@Value("${app.storage.secret-key:}") String secretKey,
			@Value("${app.storage.public-url:}") String publicUrl,
			@Value("${app.storage.path-style:true}") boolean pathStyle,
			@Value("${app.upload.directory}") String uploadDirectory) {
		this.bucket = bucket;
		this.publicUrl = publicUrl == null ? "" : publicUrl.replaceAll("/+$", "");
		this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();

		if (!endpoint.isBlank() && !accessKey.isBlank() && !secretKey.isBlank()) {
			S3Configuration configuration = S3Configuration.builder()
					.pathStyleAccessEnabled(pathStyle)
					.build();
			this.s3Client = S3Client.builder()
					.region(Region.of(region))
					.endpointOverride(URI.create(endpoint))
					.credentialsProvider(StaticCredentialsProvider.create(
							AwsBasicCredentials.create(accessKey, secretKey)))
					.serviceConfiguration(configuration)
					.build();
		} else {
			this.s3Client = null;
		}
	}

	public String store(MultipartFile file, String key) {
		try {
			if (s3Client == null) {
				Files.createDirectories(uploadDirectory);
				Path destination = uploadDirectory.resolve(key.replace('/', '-')).normalize();
				file.transferTo(destination);
				return "/uploads/" + destination.getFileName();
			}

			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.contentType(file.getContentType())
					.cacheControl("public, max-age=31536000")
					.build();
			s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

			if (!publicUrl.isBlank()) {
				return publicUrl + "/" + key;
			}
			return "/uploads/" + key;
		} catch (IOException exception) {
			throw new IllegalStateException("No se pudo guardar el archivo.", exception);
		}
	}
}
