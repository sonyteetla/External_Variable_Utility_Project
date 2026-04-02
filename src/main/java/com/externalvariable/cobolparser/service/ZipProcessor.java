package com.externalvariable.cobolparser.service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class ZipProcessor {

    public List<Path> extractZip(String zipFilePath) throws IOException {

        List<Path> extractedFiles = new ArrayList<>();

        Path tempDir = Files.createTempDirectory("cobol_zip_");

        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {

            if (!entry.getName().endsWith(".list")) continue;

            Path filePath = tempDir.resolve(entry.getName());

            Files.createDirectories(filePath.getParent());

            try (OutputStream os = Files.newOutputStream(filePath)) {
                zis.transferTo(os);
            }

            extractedFiles.add(filePath);
        }

        zis.close();

        System.out.println("✅ Extracted files: " + extractedFiles.size());

        return extractedFiles;
    }
}