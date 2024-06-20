package com.metadata_remove.controller;

import org.apache.tika.Tika;
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
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);
    private static final String OUTPUT_DIR = "C:\\Users\\esdra\\Desktop\\videos-formatados";

    @PostMapping("/remove-metadata")
    public ResponseEntity<List<String>> removeMetadata(@RequestParam("files") MultipartFile[] files) {
        List<String> results = new ArrayList<>();
        Tika tika = new Tika();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                results.add("No file uploaded for one of the entries");
                continue;
            }

            try {
                // Salvar o arquivo recebido em um diretório temporário
                Path tempDir = Files.createTempDirectory("");
                File tempFile = tempDir.resolve(file.getOriginalFilename()).toFile();
                file.transferTo(tempFile);

                // Verificar o tipo de arquivo
                String mimeType = tika.detect(tempFile);
                if (!mimeType.startsWith("video/")) {
                    results.add("Invalid file type for " + file.getOriginalFilename() + ". Please upload a video file.");
                    continue;
                }

                // Criar um arquivo de saída no diretório desejado
                File outputFile = new File(OUTPUT_DIR, file.getOriginalFilename());
                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }

            // Use o caminho completo do executável do FFmpeg
            String ffmpegPath = "C:\\PATH_ffmpeg\\ffmpeg.exe";

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
                    results.add("Metadata removed successfully: " + outputFile.getAbsolutePath());
                } else {
                    logger.error("Failed to remove metadata for {}: exit code: {}", file.getOriginalFilename(), exitCode);
                    results.add("Failed to remove metadata for " + file.getOriginalFilename());
                }
            } catch (IOException | InterruptedException e) {
                logger.error("Error processing file {}: {}", file.getOriginalFilename(), e.getMessage());
                results.add("Error processing file " + file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}