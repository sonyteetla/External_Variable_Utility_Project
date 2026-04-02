package com.externalvariable.cobolparser.controller;

import com.externalvariable.cobolparser.model.ComparisonResult;
import com.externalvariable.cobolparser.model.CobolVariable;
import com.externalvariable.cobolparser.parser.CobolParser;
import com.externalvariable.cobolparser.service.ComparisonService;
import com.externalvariable.cobolparser.service.TreeBuilder;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CobolController {

    @PostMapping("/compare")
    public List<ComparisonResult> compareFiles(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2) {

        try {
            // ✅ Convert uploaded files → lines
            List<String> lines1 = convertToLines(file1);
            List<String> lines2 = convertToLines(file2);

            // ✅ Parse COBOL variables
            CobolParser parser = new CobolParser();
            List<CobolVariable> vars1 = parser.parse(lines1);
            List<CobolVariable> vars2 = parser.parse(lines2);

            // ✅ Build hierarchy (NEW STEP 🔥)
            TreeBuilder treeBuilder = new TreeBuilder();

            CobolVariable tree1 = treeBuilder.buildTree(vars1);
            CobolVariable tree2 = treeBuilder.buildTree(vars2);

            // ✅ Debug output (IMPORTANT)
            if (tree1 != null) {
                System.out.println("Tree1 Root: " + tree1.getName());
            } else {
                System.out.println("Tree1 is NULL");
            }

            if (tree2 != null) {
                System.out.println("Tree2 Root: " + tree2.getName());
            } else {
                System.out.println("Tree2 is NULL");
            }

            // ✅ Compare variables (still flat for now)
            ComparisonService comparisonService = new ComparisonService();
            return comparisonService.compareTrees(tree1, tree2);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error processing files: " + e.getMessage());
        }
    }

    // 🔧 Helper method
    private List<String> convertToLines(MultipartFile file) throws Exception {
        String content = new String(file.getBytes());
        return Arrays.asList(content.split("\\r?\\n"));
    }
}