package com.externalvariable.cobolparser;

import com.externalvariable.cobolparser.model.*;
import com.externalvariable.cobolparser.parser.CobolParser;
import com.externalvariable.cobolparser.service.*;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.io.BufferedWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CommandLineRunnerApp implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {

        System.out.println("🔥 CLI STARTED");

        if (args.length < 2) {
            System.out.println("❌ Invalid input");
            return;
        }

        String zipPath = args[0];
        String mode = args[1];

        if ("VAR".equalsIgnoreCase(mode)) {

            String level = args[2];
            String varName = args[3];
            String external = args.length > 4 ? args[4] : null;

            runVariableAnalysis(zipPath, level, varName, external);

        } else if ("PROGRAM".equalsIgnoreCase(mode)) {

            if (args.length < 3) {
                System.out.println("❌ Program name missing");
                return;
            }

            String programName = args[2];

            runProgramAnalysis(zipPath, programName);

        } else {

            // 🔥 IMPORTANT FALLBACK
            runFullComparison(zipPath);
        }
    }

    // =========================================================
    // PROGRAM ANALYSIS (FIXED)
    // =========================================================
    private void runProgramAnalysis(String zipPath, String programName) throws Exception {

        System.out.println("📦 Extracting ZIP...");

        ZipProcessor zipProcessor = new ZipProcessor();
        List<Path> files = zipProcessor.extractZip(zipPath);

        System.out.println("📊 Total files: " + files.size());

        CobolParser parser = new CobolParser();

        List<String> fileNames = new ArrayList<>();
        List<List<CobolVariable>> fileVariablesList = new ArrayList<>();

        for (Path file : files) {

            // ✅ CRITICAL FIX (encoding)
            List<String> lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);

            List<CobolVariable> vars = parser.parse(lines);

            System.out.println("Parsed file: " + file.getFileName() + " → vars: " + vars.size());

            fileNames.add(file.getFileName().toString());
            fileVariablesList.add(vars);
        }

        ProgramAnalysisService service = new ProgramAnalysisService();
        service.analyzeProgram(programName, fileNames, fileVariablesList);

        System.out.println("🔥 Program Analysis Completed!");
    }

    // =========================================================
    // VARIABLE ANALYSIS (UNCHANGED)
    // =========================================================
    private void runVariableAnalysis(String zipPath, String level, String varName, String external) throws Exception {

        ZipProcessor zipProcessor = new ZipProcessor();
        List<Path> files = zipProcessor.extractZip(zipPath);

        CobolParser parser = new CobolParser();

        List<String> fileNames = new ArrayList<>();
        List<List<CobolVariable>> fileVariablesList = new ArrayList<>();

        for (Path file : files) {

            List<String> lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);

            List<CobolVariable> vars = parser.parse(lines);

            if (external != null && external.equalsIgnoreCase("EXTERNAL")) {

                boolean foundExternal = vars.stream()
                        .anyMatch(v ->
                                v.getName().equalsIgnoreCase(varName)
                                        && v.isExternal()
                        );

                if (!foundExternal) continue;
            }

            fileNames.add(file.getFileName().toString());
            fileVariablesList.add(vars);
        }

        VariableAnalysisService service = new VariableAnalysisService();
        service.analyzeVariableAcrossFiles(varName, fileNames, fileVariablesList);

        System.out.println("🔥 Variable Analysis Completed!");
    }

    // =========================================================
    // FULL COMPARISON (UPDATED CONSOLE FORMAT)
    // =========================================================
    private void runFullComparison(String zipPath) throws Exception {

        System.out.println("📦 Processing ZIP file...");

        ZipProcessor zipProcessor = new ZipProcessor();
        List<Path> files = zipProcessor.extractZip(zipPath);

        CobolParser parser = new CobolParser();
        TreeBuilder treeBuilder = new TreeBuilder();
        ComparisonService comparisonService = new ComparisonService();

        List<Map<String, Object>> finalReport = new ArrayList<>();

        Path txtPath = Paths.get(System.getProperty("user.dir"), "report.txt");
        BufferedWriter writer = Files.newBufferedWriter(txtPath);

        for (int i = 0; i < files.size(); i++) {
            for (int j = i + 1; j < files.size(); j++) {

                Path file1 = files.get(i);
                Path file2 = files.get(j);

                System.out.println("\n========================================");
                System.out.println("🔍 Comparing: " + file1.getFileName() + " vs " + file2.getFileName());
                System.out.println("========================================");

                writer.write("========================================\n");
                writer.write("Comparing: " + file1.getFileName() + " vs " + file2.getFileName() + "\n");

                // Parse file1
                List<String> lines1 = Files.readAllLines(file1, StandardCharsets.ISO_8859_1);
                List<CobolVariable> vars1 = parser.parse(lines1);

                // Parse file2
                List<String> lines2 = Files.readAllLines(file2, StandardCharsets.ISO_8859_1);
                List<CobolVariable> vars2 = parser.parse(lines2);

                // Build trees
                CobolVariable tree1 = treeBuilder.buildTree(vars1);
                CobolVariable tree2 = treeBuilder.buildTree(vars2);

                // Compare
                List<ComparisonResult> results = comparisonService.compareTrees(tree1, tree2);

                int missing = 0, type = 0, level = 0, position = 0;

                // ✅ PRINT ONLY ONCE
                for (ComparisonResult r : results) {

                    switch (r.getIssue()) {

                        case "MISSING_IN_FILE2":
                            System.out.println("❌ MISSING → " + r.getVariable());
                            writer.write("❌ MISSING → " + r.getVariable() + "\n");
                            missing++;
                            break;

                        case "TYPE_MISMATCH":
                            System.out.println("⚠ TYPE_MISMATCH → " + r.getVariable() +
                                    " (" + r.getFile1Value() + " vs " + r.getFile2Value() + ")");
                            writer.write("⚠ TYPE_MISMATCH → " + r.getVariable() + "\n");
                            type++;
                            break;

                        case "LEVEL_MISMATCH":
                            System.out.println("⚠ LEVEL_MISMATCH → " + r.getVariable() +
                                    " (" + r.getFile1Value() + " vs " + r.getFile2Value() + ")");
                            writer.write("⚠ LEVEL_MISMATCH → " + r.getVariable() + "\n");
                            level++;
                            break;

                        case "POSITION_MISMATCH":
                            System.out.println("📍 POSITION_MISMATCH → " + r.getVariable() +
                                    " (" + r.getFile1Value() + " vs " + r.getFile2Value() + ")");
                            writer.write("📍 POSITION_MISMATCH → " + r.getVariable() + "\n");
                            position++;
                            break;
                    }
                }

                // ✅ SUMMARY
                System.out.println("----------------------------------------");
                System.out.println("Summary:");
                System.out.println("Missing=" + missing +
                        " | Type=" + type +
                        " | Level=" + level +
                        " | Position=" + position);
                System.out.println("----------------------------------------");

                // ✅ SIMILARITY
                double similarity = comparisonService.calculateSimilarity(tree1, tree2);
                System.out.println("📊 Similarity: " + String.format("%.2f", similarity) + "%");

                Map<String, Object> entry = new HashMap<>();
                entry.put("file1", file1.getFileName().toString());
                entry.put("file2", file2.getFileName().toString());
                entry.put("differences", results);
                entry.put("similarity", similarity);

                finalReport.add(entry);
            }
        }

        writer.close();

        Path outputPath = Paths.get(System.getProperty("user.dir"), "report.json");

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), finalReport);

        System.out.println("\n✅ Reports generated");
    }
}