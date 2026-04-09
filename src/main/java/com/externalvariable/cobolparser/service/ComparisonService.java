package com.externalvariable.cobolparser.service;

import com.externalvariable.cobolparser.model.*;

import java.util.*;

public class ComparisonService {

    public List<ComparisonResult> compareTrees(CobolVariable tree1, CobolVariable tree2) {

        List<ComparisonResult> results = new ArrayList<>();

        if (tree1 == null || tree2 == null) {
            return results;
        }

        // ✅ FLATTEN BOTH TREES
        Map<String, CobolVariable> map1 = new HashMap<>();
        Map<String, CobolVariable> map2 = new HashMap<>();

        flattenTree(tree1, map1);
        flattenTree(tree2, map2);

        // ✅ COMPARE
        for (String varName : map1.keySet()) {

            CobolVariable v1 = map1.get(varName);
            CobolVariable v2 = map2.get(varName);

            if (v2 == null) {
                // 🔴 MISSING
                ComparisonResult r = new ComparisonResult();
                r.setVariable(varName);
                r.setIssue("MISSING_IN_FILE2");
                results.add(r);
            } else {

                // 🟡 LEVEL MISMATCH
                if (v1.getLevel() != v2.getLevel()) {
                    ComparisonResult r = new ComparisonResult();
                    r.setVariable(varName);
                    r.setIssue("LEVEL_MISMATCH");
                    r.setFile1Value("Level " + v1.getLevel());
                    r.setFile2Value("Level " + v2.getLevel());
                    results.add(r);
                }

                // 🟡 TYPE MISMATCH
                if (!Objects.equals(v1.getPic(), v2.getPic())) {
                    ComparisonResult r = new ComparisonResult();
                    r.setVariable(varName);
                    r.setIssue("TYPE_MISMATCH");
                    r.setFile1Value(v1.getPic());
                    r.setFile2Value(v2.getPic());
                    results.add(r);
                }

                // 🟡 POSITION MISMATCH
                if (!Objects.equals(v1.getPosition(), v2.getPosition())) {
                    if (v1.getPosition() != null && v2.getPosition() != null) {
                        ComparisonResult r = new ComparisonResult();
                        r.setVariable(varName);
                        r.setIssue("POSITION_MISMATCH");
                        r.setFile1Value(v1.getPosition());
                        r.setFile2Value(v2.getPosition());
                        results.add(r);
                    }
                }
            }
        }

        // ✅ ALSO CHECK REVERSE (IMPORTANT FIX)
        for (String varName : map2.keySet()) {
            if (!map1.containsKey(varName)) {
                ComparisonResult r = new ComparisonResult();
                r.setVariable(varName);
                r.setIssue("MISSING_IN_FILE1");
                results.add(r);
            }
        }

        return removeDuplicates(results);
    }

    private void flattenTree(CobolVariable node, Map<String, CobolVariable> map) {

        if (node == null) return;

        String name = node.getName();

        // 🔥 CRITICAL FIX: DO NOT OVERRIDE EXISTING VARIABLE
        if (!map.containsKey(name)) {
            map.put(name, node);
        }

        for (CobolVariable child : node.getChildren()) {
            flattenTree(child, map);
        }
    }

    private List<ComparisonResult> removeDuplicates(List<ComparisonResult> results) {

        Map<String, ComparisonResult> uniqueMap = new LinkedHashMap<>();

        for (ComparisonResult r : results) {
            String key = r.getVariable() + "_" + r.getIssue();
            uniqueMap.put(key, r);
        }

        return new ArrayList<>(uniqueMap.values());
    }

    public double calculateSimilarity(CobolVariable tree1, CobolVariable tree2) {

        Map<String, CobolVariable> map1 = new HashMap<>();
        Map<String, CobolVariable> map2 = new HashMap<>();

        flattenTree(tree1, map1);
        flattenTree(tree2, map2);

        Set<String> allVars = new HashSet<>();
        allVars.addAll(map1.keySet());
        allVars.addAll(map2.keySet());

        int total = allVars.size();
        double score = 0;

        for (String var : allVars) {

            if (map1.containsKey(var) && map2.containsKey(var)) {

                CobolVariable v1 = map1.get(var);
                CobolVariable v2 = map2.get(var);

                double localScore = 1.0;

                if (!Objects.equals(v1.getLevel(), v2.getLevel())) {
                    localScore -= 0.3;
                }

                if (!Objects.equals(v1.getPic(), v2.getPic())) {
                    localScore -= 0.3;
                }

                if (!Objects.equals(v1.getPosition(), v2.getPosition())) {
                    localScore -= 0.4;
                }

                if (localScore < 0) localScore = 0;

                score += localScore;
            }
        }

        if (total == 0) return 0;

        return (score / total) * 100;
    }
}