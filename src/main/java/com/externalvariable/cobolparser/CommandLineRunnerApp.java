package com.externalvariable.cobolparser;

import com.externalvariable.cobolparser.model.*;
import com.externalvariable.cobolparser.parser.CobolParser;
import com.externalvariable.cobolparser.service.*;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CommandLineRunnerApp implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {

        System.out.println("🔥 CLI STARTED");

        // ✅ Now only ONE input (ZIP file)
        if (args.length < 1) {
            System.out.println("❌ Please provide ZIP file path");
            return;
        }

        String zipPath = args[0];

        System.out.println("📦 Processing ZIP file...");

        // ✅ Extract ZIP
        ZipProcessor zipProcessor = new ZipProcessor();
        List<Path> files = zipProcessor.extractZip(zipPath);

        if (files.isEmpty()) {
            System.out.println("❌ No .list files found in ZIP");
            return;
        }

        CobolParser parser = new CobolParser();
        TreeBuilder treeBuilder = new TreeBuilder();
        ComparisonService comparisonService = new ComparisonService();

        List<Map<String, Object>> finalReport = new ArrayList<>();

        // ✅ Compare all file pairs
        for (int i = 0; i < files.size(); i++) {
            for (int j = i + 1; j < files.size(); j++) {

                Path file1 = files.get(i);
                Path file2 = files.get(j);

                System.out.println("🔍 Comparing: " + file1.getFileName() + " vs " + file2.getFileName());

                List<String> lines1 = Files.readAllLines(file1, StandardCharsets.ISO_8859_1);
                List<String> lines2 = Files.readAllLines(file2, StandardCharsets.ISO_8859_1);

                List<CobolVariable> vars1 = parser.parse(lines1);
                List<CobolVariable> vars2 = parser.parse(lines2);

                CobolVariable tree1 = treeBuilder.buildTree(vars1);
                CobolVariable tree2 = treeBuilder.buildTree(vars2);

                List<ComparisonResult> results = comparisonService.compareTrees(tree1, tree2);

                Map<String, Object> entry = new HashMap<>();
                entry.put("file1", file1.getFileName().toString());
                entry.put("file2", file2.getFileName().toString());
                entry.put("differences", results);

                finalReport.add(entry);
            }
        }

        // ✅ Save report in project root
        Path outputPath = Paths.get(System.getProperty("user.dir"), "report.json");

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(outputPath.toFile(), finalReport);

        System.out.println("✅ ZIP report generated at: " + outputPath.toAbsolutePath());
    }
}