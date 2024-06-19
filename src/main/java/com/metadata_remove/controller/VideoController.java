package com.metadata_remove.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @PostMapping("/remove-metadata")
    public ResponseEntity<String> removeMetadata(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>("No file uploaded", HttpStatus.BAD_REQUEST);
        }

        try {
            // Salvar o arquivo recebido
            Path tempDir = Files.createTempDirectory("");
            File tempFile = tempDir.resolve(file.getOriginalFilename()).toFile();
            file.transferTo(tempFile);

            // Criar um arquivo de saída
            File outputFile = tempDir.resolve("output_" + file.getOriginalFilename()).toFile();

            // Executar o comando FFmpeg para remover os metadados
            String command = String.format("ffmpeg -i %s -map_metadata -1 -c:v copy -c:a copy %s",
                    tempFile.getAbsolutePath(), outputFile.getAbsolutePath());

            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            if (outputFile.exists()) {
                return ResponseEntity.ok("Metadata removed successfully: " + outputFile.getAbsolutePath());
            } else {
                return new ResponseEntity<>("Failed to remove metadata", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException | InterruptedException e) {
            return new ResponseEntity<>("Error processing file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
