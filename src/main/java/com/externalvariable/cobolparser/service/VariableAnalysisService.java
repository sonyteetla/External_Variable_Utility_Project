package com.externalvariable.cobolparser.service;

import com.externalvariable.cobolparser.model.CobolVariable;
import com.externalvariable.cobolparser.model.ComparisonResult;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class VariableAnalysisService {

    public void analyzeVariableAcrossFiles(
            String variableName,
            List<String> fileNames,
            List<List<CobolVariable>> fileVariablesList   // ✅ FIXED TYPE
    ) {

        Map<String, CobolVariable> fileVariableMap = new LinkedHashMap<>();

        String normalizedSearch = normalize(variableName);

        System.out.println("🔍 Searching for variable: " + normalizedSearch);

        // ✅ STEP 1: DIRECT SEARCH (NO TREE)
        for (int i = 0; i < fileVariablesList.size(); i++) {

            List<CobolVariable> variables = fileVariablesList.get(i);

            boolean found = false;

            for (CobolVariable var : variables) {

                String parsedName = normalize(var.getName());

                if (parsedName.equals(normalizedSearch)) {
                    fileVariableMap.put(fileNames.get(i), var);
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.out.println("❌ NOT FOUND in: " + fileNames.get(i));
            } else {
                System.out.println("✅ FOUND in: " + fileNames.get(i));
            }
        }

        System.out.println("📊 Total matched files: " + fileVariableMap.size());

        // ✅ STEP 2: impact.txt
        writeImpactFile(variableName, fileVariableMap);

        // ✅ STEP 3: comparison
        List<ComparisonResult> results = new ArrayList<>();
        List<String> files = new ArrayList<>(fileVariableMap.keySet());

        for (int i = 0; i < files.size(); i++) {
            for (int j = i + 1; j < files.size(); j++) {

                String file1 = files.get(i);
                String file2 = files.get(j);

                CobolVariable v1 = fileVariableMap.get(file1);
                CobolVariable v2 = fileVariableMap.get(file2);

                if (v1 == null || v2 == null) continue;

                if (v1.getLevel() != v2.getLevel()) {
                    results.add(buildResult(variableName, "LEVEL_MISMATCH",
                            file1 + ": " + v1.getLevel(),
                            file2 + ": " + v2.getLevel()));
                }

                if (!Objects.equals(v1.getPic(), v2.getPic())) {
                    results.add(buildResult(variableName, "TYPE_MISMATCH",
                            file1 + ": " + v1.getPic(),
                            file2 + ": " + v2.getPic()));
                }

                if (!Objects.equals(v1.getPosition(), v2.getPosition())) {
                    if (v1.getPosition() != null && v2.getPosition() != null) {
                        results.add(buildResult(variableName, "POSITION_MISMATCH",
                                file1 + ": " + v1.getPosition(),
                                file2 + ": " + v2.getPosition()));
                    }
                }
            }
        }

        // ✅ STEP 4: report.txt
        writeReport(variableName, results);

        System.out.println("✅ Command Type 2 FULLY WORKING!");
    }

    // 🔥 NORMALIZATION (VERY IMPORTANT)
    private String normalize(String name) {
        if (name == null) return "";

        return name
                .toUpperCase()
                .replaceAll("\\.", "")
                .replaceAll("[^A-Z0-9-]", "")
                .trim();
    }

    private ComparisonResult buildResult(String var, String issue, String v1, String v2) {
        ComparisonResult r = new ComparisonResult();
        r.setVariable(var);
        r.setIssue(issue);
        r.setFile1Value(v1);
        r.setFile2Value(v2);
        return r;
    }

    private void writeImpactFile(String variableName, Map<String, CobolVariable> map) {
        try (FileWriter fw = new FileWriter("impact.txt")) {

            fw.write("VARIABLE: " + variableName + "\n");
            fw.write("FILES CONTAINING VARIABLE:\n\n");

            for (String file : map.keySet()) {
                fw.write("- " + file + "\n");
            }

            fw.write("\nTotal impacted files: " + map.size());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeReport(String variableName, List<ComparisonResult> results) {
        try (FileWriter fw = new FileWriter("report.txt")) {

            fw.write("VARIABLE ANALYSIS: " + variableName + "\n\n");

            if (results.isEmpty()) {
                fw.write("✅ No differences found\n");
                return;
            }

            for (ComparisonResult r : results) {
                fw.write("[" + r.getIssue() + "] " + r.getVariable() + "\n");
                fw.write("  " + r.getFile1Value() + "\n");
                fw.write("  " + r.getFile2Value() + "\n\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}