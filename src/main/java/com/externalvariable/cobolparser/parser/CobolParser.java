package com.externalvariable.cobolparser.parser;

import com.externalvariable.cobolparser.model.CobolVariable;

import java.util.*;
import java.util.regex.*;

public class CobolParser {

    public List<CobolVariable> parse(List<String> lines) {

        Map<String, CobolVariable> uniqueMap = new LinkedHashMap<>();

        // ✅ STRICT + SAFE pattern
        Pattern varPattern = Pattern.compile("^\\s*(\\d{1,2})\\s+([A-Z][A-Z0-9-]*)");
        Pattern picPattern = Pattern.compile("\\bPIC\\s+([A-Z0-9\\(\\)V]+)");

        boolean insideCopybook = false;
        boolean currentBlockExternal = false;

        for (String rawLine : lines) {

            if (rawLine == null || rawLine.trim().isEmpty()) continue;

            String line = rawLine.toUpperCase();

            // ❌ Ignore comments
            if (line.trim().startsWith("*")) continue;

            // ❌ Ignore separators
            if (line.contains("*****")) continue;

            // ❌ Ignore headers
            if (line.contains("MODIFICATIONS HISTORY")) continue;
            if (line.contains("COPYBOOK")) continue;

            // ❌ Stop condition
            if (line.contains("END OF COPYBOOK")) {
                break;
            }

            // 🔥 FIXED PREFIX REMOVAL (IMPORTANT)
            line = line.replaceFirst("^\\s*[0-9]{5,}[A-Z]?\\s+", "");

            // 🔥 Start parsing when COBOL structure appears
            if (line.matches("^\\s*\\d{1,2}\\s+[A-Z].*")) {
                insideCopybook = true;
            }

            if (!insideCopybook) continue;

            Matcher matcher = varPattern.matcher(line);

            if (!matcher.find()) continue;

            String levelStr = matcher.group(1);
            String name = matcher.group(2);

            // 🔥 CLEAN NAME
            name = name.trim().replaceAll("\\.$", "");

            // 🔥 HANDLE EXTERNAL BLOCK
            if (levelStr.equals("01")) {
                currentBlockExternal = line.contains("EXTERNAL");
            }

            // ❌ Skip unwanted
            if (name.matches("IP\\d{6}")) continue;
            if (name.matches("\\d+")) continue;

            if (name.equals("RETURN-CODE") ||
                name.equals("PGMNAME") ||
                name.equals("CEEUOPT") ||
                name.equals("IEWBLIT") ||
                name.equals("OFFSET") ||
                name.equals("DEFINED") ||
                name.equals("PROGRAM-ID") ||
                name.equals("ENTRY")) continue;

            // ✅ Only business variables
            if (!name.contains("-")) continue;

            CobolVariable var = new CobolVariable();
            var.setLevel(Integer.parseInt(levelStr));
            var.setName(name);

            // 🔥 FIX: propagate EXTERNAL correctly
            boolean isExternal = line.contains("EXTERNAL") || currentBlockExternal;
            var.setExternal(isExternal);

            // ✅ PIC extraction
            Matcher picMatcher = picPattern.matcher(line);
            if (picMatcher.find()) {
                var.setPic("PIC " + picMatcher.group(1));
            }

            // ✅ Position fallback
            var.setPosition(String.valueOf(uniqueMap.size() + 1));

            uniqueMap.putIfAbsent(name, var);

            // 🔍 DEBUG
            System.out.println("Parsed: " + name +
                    " | Level: " + levelStr +
                    " | External: " + isExternal);
        }

        List<CobolVariable> variables = new ArrayList<>(uniqueMap.values());

        System.out.println("✅ Total Parsed Variables: " + variables.size());

        return variables;
    }
}