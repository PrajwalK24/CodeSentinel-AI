package com.codesentinel.analyzer.rules;

import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.Severity;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NullPointerDetector {
    private static final Pattern METHOD_CALL = Pattern.compile("\\b([a-zA-Z_][\\w]*)\\s*\\.\\s*[a-zA-Z_]\\w*\\s*\\(");
    private static final Pattern NULL_GUARD = Pattern.compile("\\b([a-zA-Z_][\\w]*)\\s*(?:!=\\s*null|==\\s*null)|Objects\\.nonNull\\s*\\(\\s*([a-zA-Z_]\\w*)");
    private static final Pattern RETURN_NULL = Pattern.compile("\\breturn\\s+null\\s*;");
    private static final Pattern ARRAY_ACCESS = Pattern.compile("\\b([a-zA-Z_][\\w]*)\\s*\\[[^\\]]+\\]");
    private static final Set<String> ARRAY_TYPE_WORDS = Set.of(
            "byte", "short", "int", "long", "float", "double", "char", "boolean", "String");
    private static final Set<String> SAFE_QUALIFIERS = Set.of(
            "System", "Objects", "String", "Math", "Collections", "List", "Map", "Optional",
            "this", "super", "out", "err", "console", "logger", "log");

    public List<CodeIssue> detect(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        String[] lines = code.split("\\R", -1);
        Deque<String> recentGuards = new ArrayDeque<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher guard = NULL_GUARD.matcher(line);
            while (guard.find()) {
                recentGuards.addLast(Optional.ofNullable(guard.group(1)).orElse(guard.group(2)));
            }
            while (recentGuards.size() > 12) recentGuards.removeFirst();

            Matcher call = METHOD_CALL.matcher(line);
            while (call.find()) {
                String object = call.group(1);
                if (!SAFE_QUALIFIERS.contains(object)
                        && !isFrameworkOutputChain(line, object)
                        && !recentGuards.contains(object)
                        && !line.contains(object + " != null")) {
                    issues.add(new CodeIssue(i + 1, "Possible Null Pointer", Severity.MAJOR,
                            "Object '" + object + "' is used without a nearby null guard.",
                            "Validate the object before dereferencing it or use Optional/null-safe control flow.",
                            line.trim()));
                    break;
                }
            }

            if (RETURN_NULL.matcher(line).find() && !hasNearbyComment(lines, i)) {
                issues.add(new CodeIssue(i + 1, "Return Null", Severity.MAJOR,
                        "Method returns null without nearby documentation.",
                        "Return an empty object/collection or document the null contract clearly.",
                        line.trim()));
            }

            Matcher array = ARRAY_ACCESS.matcher(line);
            while (array.find()) {
                String arrayName = array.group(1);
                if (!isProbablySafeArrayAccess(lines, i, line, arrayName)) {
                    issues.add(new CodeIssue(i + 1, "Unchecked Array Access", Severity.MAJOR,
                            "Array access may occur without a length or bounds check.",
                            "Check the array length or index bounds before accessing the element.",
                            line.trim()));
                    break;
                }
            }
        }
        return issues;
    }

    private boolean isFrameworkOutputChain(String line, String object) {
        String compact = line.replaceAll("\\s+", "");
        return compact.contains("System." + object + ".")
                || compact.contains("console." + object + ".")
                || compact.contains("logger." + object + ".")
                || compact.contains("log." + object + ".");
    }

    private boolean hasNearbyComment(String[] lines, int i) {
        for (int j = Math.max(0, i - 2); j <= i; j++) {
            if (lines[j].contains("//") || lines[j].contains("/*") || lines[j].contains("*")) return true;
        }
        return false;
    }

    private boolean isProbablySafeArrayAccess(String[] lines, int i, String line, String arrayName) {
        if (ARRAY_TYPE_WORDS.contains(arrayName) || line.contains(".length")) {
            return true;
        }
        String nearby = nearbyCode(lines, i, 10);
        String compact = nearby.replaceAll("\\s+", "");
        String compactArray = Pattern.quote(arrayName);
        if (compact.contains(arrayName + ".length")) {
            return true;
        }
        if (compact.matches("(?s).*" + compactArray + "\\[[^\\]]+\\]\\.length.*")) {
            return true;
        }
        if (compact.matches("(?s).*(?:if|while)\\([^)]*" + compactArray + "[^)]*\\.length[^)]*\\).*")) {
            return true;
        }
        return compact.contains(arrayName + "!=null") || compact.contains(arrayName + "==null");
    }

    private String nearbyCode(String[] lines, int i, int lookback) {
        StringBuilder builder = new StringBuilder();
        for (int j = Math.max(0, i - lookback); j <= i; j++) {
            builder.append(lines[j]).append('\n');
        }
        return builder.toString();
    }
}
