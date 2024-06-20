package com.metadata_remove.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);
    private static final String OUTPUT_DIR = "C:\\Users\\esdra\\Desktop\\videos-formatados";

    @PostMapping("/remove-metadata")
    public ResponseEntity<String> removeMetadata(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>("No file uploaded", HttpStatus.BAD_REQUEST);
        }

        try {
            // Salvar o arquivo recebido em um diretório temporário
            Path tempDir = Files.createTempDirectory("");
            File tempFile = tempDir.resolve(file.getOriginalFilename()).toFile();
            file.transferTo(tempFile);

            // Criar um arquivo de saída no diretório desejado
            File outputFile = new File(OUTPUT_DIR, "output_" + file.getOriginalFilename());
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }

            // Use o caminho completo do executável do FFmpeg
            String ffmpegPath = "C:\\PATH_ffmpeg\\ffmpeg.exe"; // Use o caminho completo se necessário, por exemplo, "C:\\ffmpeg\\bin\\ffmpeg.exe"

            String command = String.format("%s -i %s -map_metadata -1 -c:v copy -c:a copy %s",
                    ffmpegPath, tempFile.getAbsolutePath(), outputFile.getAbsolutePath());

            logger.info("Executing command: {}", command);

            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegPath, "-i", tempFile.getAbsolutePath(),
                    "-map_metadata", "-1", "-c:v", "copy", "-c:a", "copy", outputFile.getAbsolutePath());

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Ler a saída do processo para registrar eventuais erros
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0 && outputFile.exists()) {
                logger.info("Metadata removed successfully: {}", outputFile.getAbsolutePath());
                return ResponseEntity.ok("Metadata removed successfully: " + outputFile.getAbsolutePath());
            } else {
                logger.error("Failed to remove metadata, exit code: {}", exitCode);
                return new ResponseEntity<>("Failed to remove metadata", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error processing file: {}", e.getMessage());
            return new ResponseEntity<>("Error processing file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}