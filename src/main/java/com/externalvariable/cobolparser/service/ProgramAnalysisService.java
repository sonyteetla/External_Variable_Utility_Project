package com.externalvariable.cobolparser.service;

import com.externalvariable.cobolparser.model.CobolVariable;
import com.externalvariable.cobolparser.model.ComparisonResult;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ProgramAnalysisService {

    public void analyzeProgram(
            String programName,
            List<String> fileNames,
            List<List<CobolVariable>> fileVariablesList
    ) {

        String targetFile = programName.toUpperCase() + ".LIST";

        List<CobolVariable> baseVariables = null;

        // ✅ STEP 1: Find base program file
        for (int i = 0; i < fileNames.size(); i++) {
            if (fileNames.get(i).equalsIgnoreCase(targetFile)) {
                baseVariables = fileVariablesList.get(i);
                break;
            }
        }

        if (baseVariables == null) {
            System.out.println("❌ Program file not found: " + programName);
            return;
        }

        System.out.println("✅ Found program: " + targetFile);

        // ✅ STEP 2: Build variable set (base program)
        Set<String> baseVarNames = new HashSet<>();
        for (CobolVariable v : baseVariables) {
            baseVarNames.add(normalize(v.getName()));
        }

        Map<String, List<CobolVariable>> impactedFiles = new LinkedHashMap<>();
        Map<String, Set<String>> commonVarsMap = new LinkedHashMap<>();

        // ✅ STEP 3: Find REAL impacted files (based on COMMON VARIABLES COUNT)
        for (int i = 0; i < fileVariablesList.size(); i++) {

            String file = fileNames.get(i);
            List<CobolVariable> vars = fileVariablesList.get(i);

            Set<String> commonVars = new HashSet<>();

            for (CobolVariable v : vars) {
                String name = normalize(v.getName());

                if (baseVarNames.contains(name)) {
                    commonVars.add(name);
                }
            }

            // 🔥 IMPORTANT FILTER (core fix)
            if (commonVars.size() >= 5) {  // threshold (you can tune)
                impactedFiles.put(file, vars);
                commonVarsMap.put(file, commonVars);
            }
        }

        System.out.println("📊 Real impacted files: " + impactedFiles.size());

        // ✅ STEP 4: Write impact.txt
        writeImpact(programName, impactedFiles, commonVarsMap);

        // ✅ STEP 5: Compare base with others
        List<ComparisonResult> results = new ArrayList<>();

        Map<String, CobolVariable> mapBase = toMap(baseVariables);

        for (String file : impactedFiles.keySet()) {

            if (file.equalsIgnoreCase(targetFile)) continue;

            Map<String, CobolVariable> mapOther = toMap(impactedFiles.get(file));

            for (String var : mapBase.keySet()) {

                CobolVariable v1 = mapBase.get(var);
                CobolVariable v2 = mapOther.get(var);

                if (v2 == null) {
                    results.add(build(var, "MISSING_IN_" + file, "", ""));
                    continue;
                }

                if (v1.getLevel() != v2.getLevel()) {
                    results.add(build(var, "LEVEL_MISMATCH",
                            targetFile + ": " + v1.getLevel(),
                            file + ": " + v2.getLevel()));
                }

                if (!Objects.equals(v1.getPic(), v2.getPic())) {
                    results.add(build(var, "TYPE_MISMATCH",
                            targetFile + ": " + v1.getPic(),
                            file + ": " + v2.getPic()));
                }
            }
        }

        // ✅ STEP 6: Write report
        writeReport(programName, results);

        System.out.println("🔥 Command Type 3 PERFECTLY WORKING!");
    }

    // 🔧 Convert list → map
    private Map<String, CobolVariable> toMap(List<CobolVariable> list) {
        Map<String, CobolVariable> map = new HashMap<>();
        for (CobolVariable v : list) {
            map.put(normalize(v.getName()), v);
        }
        return map;
    }

    // 🔧 Normalize names
    private String normalize(String name) {
        return name.toUpperCase().replaceAll("[^A-Z0-9-]", "");
    }

    // 🔧 Build result
    private ComparisonResult build(String var, String issue, String v1, String v2) {
        ComparisonResult r = new ComparisonResult();
        r.setVariable(var);
        r.setIssue(issue);
        r.setFile1Value(v1);
        r.setFile2Value(v2);
        return r;
    }

    // 🔥 Improved impact.txt
    private void writeImpact(String program,
                             Map<String, List<CobolVariable>> map,
                             Map<String, Set<String>> commonVarsMap) {

        try (FileWriter fw = new FileWriter("impact.txt")) {

            fw.write("PROGRAM: " + program + "\n");
            fw.write("IMPACTED FILES:\n\n");

            for (String f : map.keySet()) {

                fw.write("- " + f + "\n");

                // 🔥 Show WHY impacted
                Set<String> vars = commonVarsMap.get(f);
                int count = 0;

                for (String v : vars) {
                    fw.write("    → " + v + "\n");
                    count++;
                    if (count == 5) break; // limit output
                }

                fw.write("\n");
            }

            fw.write("Total impacted files: " + map.size());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 🔥 Clean report
    private void writeReport(String program, List<ComparisonResult> results) {

        try (FileWriter fw = new FileWriter("report.txt")) {

            fw.write("PROGRAM ANALYSIS: " + program + "\n\n");

            if (results.isEmpty()) {
                fw.write("✅ No differences found\n");
                return;
            }

            // 🔥 Remove duplicates
            Set<String> seen = new HashSet<>();

            for (ComparisonResult r : results) {

                String key = r.getVariable() + r.getIssue();
                if (seen.contains(key)) continue;
                seen.add(key);

                fw.write("[" + r.getIssue() + "] " + r.getVariable() + "\n");
                fw.write("  " + r.getFile1Value() + "\n");
                fw.write("  " + r.getFile2Value() + "\n\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}