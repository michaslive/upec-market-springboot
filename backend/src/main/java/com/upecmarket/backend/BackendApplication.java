package com.upecmarket.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	public static void main(String[] args) {
		loadLocalEnvironment();
		SpringApplication.run(BackendApplication.class, args);
	}

	private static void loadLocalEnvironment() {
		Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
		Path[] candidates = { workingDirectory.resolve("backend"), workingDirectory };

		for (Path directory : candidates) {
			if (Files.isRegularFile(directory.resolve(".env"))) {
				Dotenv dotenv = Dotenv.configure()
						.directory(directory.toString())
						.ignoreIfMissing()
						.load();
				dotenv.entries().forEach(entry -> {
					if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
						System.setProperty(entry.getKey(), entry.getValue());
					}
				});
				return;
			}
		}
	}
}
