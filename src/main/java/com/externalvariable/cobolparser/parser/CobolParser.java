package com.externalvariable.cobolparser.parser;

import com.externalvariable.cobolparser.model.CobolVariable;

import java.util.*;
import java.util.regex.*;

public class CobolParser {

    public List<CobolVariable> parse(List<String> lines) {

    	Map<String, CobolVariable> uniqueMap = new LinkedHashMap<>();

        Pattern pattern = Pattern.compile("^\\s*(\\d{1,2})\\s+([A-Z][A-Z0-9-]*)");
        Pattern picPattern = Pattern.compile("PIC\\s+([A-Z0-9\\(\\)V]+)");
        Pattern posPattern = Pattern.compile("BLX=(\\d+),(\\d+)");

        for (String line : lines) {

            if (line == null || line.trim().isEmpty()) continue;

            line = line.toUpperCase();

            Matcher m = pattern.matcher(line);

            if (m.find()) {

                String levelStr = m.group(1);
                String name = m.group(2);

                // ❌ Skip condition level
                if ("88".equals(levelStr)) continue;

                // ❌ BASIC CLEANING
                if (name.length() < 3) continue;
                if (name.matches("\\d+")) continue;
                if (name.matches("[0-9A-F]+")) continue;
                if (name.startsWith("000")) continue;

                // ❌ REMOVE SYSTEM VARIABLES
                if (name.equals("RETURN-CODE")) continue;
                if (name.equals("PGMNAME")) continue;
                if (name.equals("CEEUOPT")) continue;
                if (name.equals("IEWBLIT")) continue;
                if (name.equals("OFFSET")) continue;
                if (name.equals("DEFINED")) continue;
                if (name.equals("PROGRAM-ID")) continue;
                if (name.equals("ENTRY")) continue;

                
                CobolVariable var = new CobolVariable();

                var.setLevel(Integer.parseInt(levelStr));
                var.setName(name);

                // PIC extraction
                Matcher picMatcher = picPattern.matcher(line);
                if (picMatcher.find()) {
                    var.setPic("PIC " + picMatcher.group(1));
                }

                // Position extraction
                Matcher posMatcher = posPattern.matcher(line);
                if (posMatcher.find()) {
                    var.setPosition(posMatcher.group(1) + "-" + posMatcher.group(2));
                }

                uniqueMap.put(name, var);

                // Debug
                System.out.println("Parsed: " + name);
            }
        }

        List<CobolVariable> variables = new ArrayList<>(uniqueMap.values());

        System.out.println("✅ Unique Parsed variables count: " + variables.size());

        return variables;
    }
}