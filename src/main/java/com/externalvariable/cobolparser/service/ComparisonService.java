package com.externalvariable.cobolparser.service;

import com.externalvariable.cobolparser.model.*;

import java.util.*;

public class ComparisonService {

    public List<ComparisonResult> compareTrees(CobolVariable tree1, CobolVariable tree2) {

        List<ComparisonResult> results = new ArrayList<>();

        if (tree1 == null || tree2 == null) {
            return results;
        }

        // 🔥 STEP 1: TREE comparison
        compareNodesIgnoringRoot(tree1, tree2, results);

        // 🔥 STEP 2: FLAT comparison (IMPORTANT FIX)
        Map<String, CobolVariable> map1 = new HashMap<>();
        Map<String, CobolVariable> map2 = new HashMap<>();

        flattenTree(tree1, map1);
        flattenTree(tree2, map2);

        for (String key : map1.keySet()) {
            if (!map2.containsKey(key)) {

                ComparisonResult r = new ComparisonResult();
                r.setVariable(key);
                r.setIssue("MISSING_IN_FILE2");

                results.add(r);
            }
        }

        // 🔥 REMOVE DUPLICATES (VERY IMPORTANT)
        return removeDuplicates(results);
    }

    private void compareNodesIgnoringRoot(CobolVariable tree1, CobolVariable tree2, List<ComparisonResult> results) {

        Map<String, CobolVariable> map2 = new HashMap<>();
        flattenTree(tree2, map2);

        for (CobolVariable child : tree1.getChildren()) {
            compareFlattened(child, map2, results);
        }
    }

    private void flattenTree(CobolVariable node, Map<String, CobolVariable> map) {

        if (node == null) return;

        map.put(node.getName(), node);

        for (CobolVariable child : node.getChildren()) {
            flattenTree(child, map);
        }
    }

    private void compareFlattened(CobolVariable node, Map<String, CobolVariable> map2, List<ComparisonResult> results) {

        if (node == null) return;

        if (!map2.containsKey(node.getName())) {

            ComparisonResult r = new ComparisonResult();
            r.setVariable(node.getName());
            r.setIssue("MISSING_IN_FILE2");

            results.add(r);

        } else {

            CobolVariable node2 = map2.get(node.getName());

            // Level mismatch
            if (node.getLevel() != node2.getLevel()) {
                ComparisonResult r = new ComparisonResult();
                r.setVariable(node.getName());
                r.setIssue("LEVEL_MISMATCH");
                r.setFile1Value("Level " + node.getLevel());
                r.setFile2Value("Level " + node2.getLevel());
                results.add(r);
            }

            // Type mismatch
            if (!Objects.equals(node.getPic(), node2.getPic())) {
                ComparisonResult r = new ComparisonResult();
                r.setVariable(node.getName());
                r.setIssue("TYPE_MISMATCH");
                r.setFile1Value(node.getPic());
                r.setFile2Value(node2.getPic());
                results.add(r);
            }
        }

        for (CobolVariable child : node.getChildren()) {
            compareFlattened(child, map2, results);
        }
    }

    // 🔥 REMOVE DUPLICATE RESULTS
    private List<ComparisonResult> removeDuplicates(List<ComparisonResult> results) {

        Map<String, ComparisonResult> uniqueMap = new LinkedHashMap<>();

        for (ComparisonResult r : results) {
            String key = r.getVariable() + "_" + r.getIssue();
            uniqueMap.put(key, r);
        }

        return new ArrayList<>(uniqueMap.values());
    }
}