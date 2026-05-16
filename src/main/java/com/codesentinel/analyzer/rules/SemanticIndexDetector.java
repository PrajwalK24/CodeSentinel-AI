package com.codesentinel.analyzer.rules;

import com.codesentinel.analyzer.model.CodeIssue;
import com.codesentinel.model.Severity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemanticIndexDetector {
    private static final Pattern INT_ASSIGN = Pattern.compile("\\bint\\s+([a-zA-Z_]\\w*)\\s*=\\s*(-?\\d+)\\s*;");
    private static final Pattern ARRAY_LITERAL = Pattern.compile("\\b(?:int|long|double|float|char|byte|short|boolean|String)\\s*\\[\\s*\\]\\s+([a-zA-Z_]\\w*)\\s*=\\s*\\{([^}]*)\\}");
    private static final Pattern ARRAY_NEW = Pattern.compile("\\b(?:int|long|double|float|char|byte|short|boolean|String)\\s*\\[\\s*\\]\\s+([a-zA-Z_]\\w*)\\s*=\\s*new\\s+\\w+\\s*\\[\\s*([^\\]]+)\\s*\\]");
    private static final Pattern MATRIX_NEW = Pattern.compile("\\b(?:int|long|double|float|char|byte|short|boolean|String)\\s*\\[\\s*\\]\\s*\\[\\s*\\]\\s+([a-zA-Z_]\\w*)\\s*=\\s*new\\s+\\w+\\s*\\[\\s*([^\\]]+)\\s*\\]\\s*\\[\\s*([^\\]]+)\\s*\\]");
    private static final Pattern LENGTH_ASSIGN = Pattern.compile("\\bint\\s+([a-zA-Z_]\\w*)\\s*=\\s*([a-zA-Z_]\\w*)\\.length\\s*;");
    private static final Pattern FOR_LOOP = Pattern.compile("\\bfor\\s*\\(\\s*(?:int\\s+)?([a-zA-Z_]\\w*)\\s*=\\s*([^;]+);\\s*\\1\\s*(<=|>=|<|>)\\s*([^;]+);\\s*\\1\\s*(\\+\\+|--|[+\\-]=\\s*\\d+)\\s*\\)");
    private static final Pattern ARRAY_ACCESS = Pattern.compile("\\b([a-zA-Z_]\\w*)\\s*\\[\\s*([^\\]]+)\\s*]");
    private static final Pattern MATRIX_ACCESS = Pattern.compile("\\b([a-zA-Z_]\\w*)\\s*\\[\\s*([^\\]]+)\\s*]\\s*\\[\\s*([^\\]]+)\\s*]");

    public List<CodeIssue> detect(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        String clean = CodeSanitizer.stripCommentsAndStrings(code);
        String[] originalLines = code.split("\\R", -1);
        String[] lines = clean.split("\\R", -1);
        State state = discoverState(lines);

        for (int i = 0; i < lines.length; i++) {
            Matcher loop = FOR_LOOP.matcher(lines[i]);
            if (!loop.find()) continue;
            LoopRange range = loopRange(loop, state);
            if (range == null) continue;
            String block = blockFrom(lines, i);
            checkMatrixAccesses(issues, originalLines, i, block, range, state);
            checkArrayAccesses(issues, originalLines, i, block, range, state);
        }
        return issues;
    }

    private State discoverState(String[] lines) {
        State state = new State();
        for (String line : lines) {
            Matcher intValue = INT_ASSIGN.matcher(line);
            if (intValue.find()) {
                state.constants.put(intValue.group(1), Integer.parseInt(intValue.group(2)));
            }

            Matcher literal = ARRAY_LITERAL.matcher(line);
            if (literal.find()) {
                state.arraySizes.put(literal.group(1), Size.constant(countLiteralItems(literal.group(2))));
            }

            Matcher array = ARRAY_NEW.matcher(line);
            if (array.find()) {
                state.arraySizes.put(array.group(1), sizeOf(array.group(2).trim(), state));
            }

            Matcher matrix = MATRIX_NEW.matcher(line);
            if (matrix.find()) {
                state.matrixRows.put(matrix.group(1), sizeOf(matrix.group(2).trim(), state));
                state.matrixCols.put(matrix.group(1), sizeOf(matrix.group(3).trim(), state));
            }

            Matcher length = LENGTH_ASSIGN.matcher(line);
            if (length.find()) {
                Size arraySize = state.arraySizes.get(length.group(2));
                if (arraySize != null) {
                    state.symbolSizes.put(length.group(1), arraySize);
                }
            }
        }
        return state;
    }

    private LoopRange loopRange(Matcher loop, State state) {
        String variable = loop.group(1);
        Bound start = boundOf(loop.group(2).trim(), state);
        Bound limit = boundOf(loop.group(4).trim(), state);
        if (start == null || limit == null) return null;
        boolean increasing = loop.group(5).contains("++") || loop.group(5).contains("+=");
        String operator = loop.group(3);
        return new LoopRange(variable, start, limit, operator, increasing);
    }

    private void checkArrayAccesses(List<CodeIssue> issues, String[] originalLines, int loopLine, String block, LoopRange range, State state) {
        Matcher access = ARRAY_ACCESS.matcher(block);
        while (access.find()) {
            String arrayName = access.group(1);
            String index = access.group(2).trim();
            if (!index.equals(range.variable)) continue;
            Size size = state.arraySizes.get(arrayName);
            if (size == null) continue;
            IndexRisk risk = riskFor(range, size);
            if (risk == IndexRisk.UPPER_OUT_OF_BOUNDS) {
                addIssue(issues, originalLines, loopLine, "Array Index Out Of Bounds",
                        "The loop allows '" + range.variable + "' to reach the array length, but valid indexes end at length - 1.",
                        "Change the loop condition to use '<' instead of '<=', for example: for (int " + range.variable + " = 0; " + range.variable + " < " + arrayName + ".length; " + range.variable + "++) { ... }",
                        "When " + range.variable + " equals the array length, " + arrayName + "[" + range.variable + "] throws ArrayIndexOutOfBoundsException.");
            } else if (risk == IndexRisk.LOWER_OUT_OF_BOUNDS) {
                addIssue(issues, originalLines, loopLine, "Array Index Out Of Bounds",
                        "The loop can access a negative index, but Java array indexes must be zero or greater.",
                        "Start from 0 or guard before access, for example: if (" + range.variable + " >= 0) value = " + arrayName + "[" + range.variable + "];",
                        "A negative index throws ArrayIndexOutOfBoundsException at runtime.");
            }
        }
    }

    private void checkMatrixAccesses(List<CodeIssue> issues, String[] originalLines, int loopLine, String block, LoopRange range, State state) {
        Matcher access = MATRIX_ACCESS.matcher(block);
        while (access.find()) {
            String matrixName = access.group(1);
            String rowIndex = access.group(2).trim();
            String colIndex = access.group(3).trim();
            Size rows = state.matrixRows.get(matrixName);
            Size cols = state.matrixCols.get(matrixName);
            if (rowIndex.equals(range.variable) && rows != null && riskFor(range, rows) == IndexRisk.UPPER_OUT_OF_BOUNDS) {
                addIssue(issues, originalLines, loopLine, "Invalid Matrix Row Access",
                        "The loop can make row index '" + range.variable + "' equal to the row count.",
                        "Use '<' for the row bound, for example: for (int " + range.variable + " = 0; " + range.variable + " < " + matrixName + ".length; " + range.variable + "++) { ... }",
                        "Accessing row " + range.variable + " at the row count throws ArrayIndexOutOfBoundsException.");
            }
            if (colIndex.equals(range.variable) && cols != null && riskFor(range, cols) == IndexRisk.UPPER_OUT_OF_BOUNDS) {
                addIssue(issues, originalLines, loopLine, "Invalid Matrix Column Access",
                        "The loop can make column index '" + range.variable + "' equal to the column count.",
                        "Use '<' for the column bound, for example: for (int " + range.variable + " = 0; " + range.variable + " < " + matrixName + "[0].length; " + range.variable + "++) { ... }",
                        "Accessing column " + range.variable + " at the column count throws ArrayIndexOutOfBoundsException.");
            }
        }
    }

    private IndexRisk riskFor(LoopRange range, Size size) {
        if (!range.increasing) return IndexRisk.UNKNOWN;
        if (range.start.constant != null && range.start.constant < 0) return IndexRisk.LOWER_OUT_OF_BOUNDS;
        if (!range.operator.equals("<=")) return IndexRisk.SAFE;
        if (sameBound(range.limit, size)) return IndexRisk.UPPER_OUT_OF_BOUNDS;
        if (range.limit.constant != null && size.constant != null && range.limit.constant >= size.constant) {
            return IndexRisk.UPPER_OUT_OF_BOUNDS;
        }
        return IndexRisk.UNKNOWN;
    }

    private boolean sameBound(Bound bound, Size size) {
        if (bound.symbol != null && bound.symbol.equals(size.symbol)) return true;
        if (bound.constant != null && size.constant != null && bound.constant.equals(size.constant)) return true;
        return false;
    }

    private Bound boundOf(String expression, State state) {
        Integer constant = parseInteger(expression);
        if (constant != null) return Bound.constant(constant);
        if (state.constants.containsKey(expression)) return Bound.constant(state.constants.get(expression));
        Size symbolSize = state.symbolSizes.get(expression);
        if (symbolSize != null) return Bound.fromSize(symbolSize);
        Matcher length = Pattern.compile("([a-zA-Z_]\\w*)\\.length").matcher(expression);
        if (length.matches()) return Bound.symbol(length.group(1) + ".length");
        return Bound.symbol(expression);
    }

    private Size sizeOf(String expression, State state) {
        Integer constant = parseInteger(expression);
        if (constant != null) return Size.constant(constant);
        if (state.constants.containsKey(expression)) return Size.constant(state.constants.get(expression));
        return Size.symbol(expression);
    }

    private Integer parseInteger(String expression) {
        try {
            return Integer.parseInt(expression);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int countLiteralItems(String body) {
        String trimmed = body.trim();
        if (trimmed.isEmpty()) return 0;
        return trimmed.split(",").length;
    }

    private String blockFrom(String[] lines, int start) {
        StringBuilder block = new StringBuilder();
        int depth = 0;
        boolean sawBrace = false;
        for (int i = start; i < lines.length; i++) {
            for (char ch : lines[i].toCharArray()) {
                if (ch == '{') {
                    depth++;
                    sawBrace = true;
                }
                if (ch == '}') depth--;
            }
            block.append(lines[i]).append('\n');
            if (sawBrace && depth <= 0) break;
            if (!sawBrace) break;
        }
        return block.toString();
    }

    private void addIssue(List<CodeIssue> issues, String[] lines, int zeroBasedLine, String type, String why, String fix, String impact) {
        issues.add(new CodeIssue(zeroBasedLine + 1, type, Severity.CRITICAL, why, fix,
                lines[zeroBasedLine].trim(), impact, "High Confidence"));
    }

    private enum IndexRisk {
        SAFE,
        UNKNOWN,
        UPPER_OUT_OF_BOUNDS,
        LOWER_OUT_OF_BOUNDS
    }

    private static class State {
        private final Map<String, Integer> constants = new HashMap<>();
        private final Map<String, Size> arraySizes = new HashMap<>();
        private final Map<String, Size> matrixRows = new HashMap<>();
        private final Map<String, Size> matrixCols = new HashMap<>();
        private final Map<String, Size> symbolSizes = new HashMap<>();
    }

    private record LoopRange(String variable, Bound start, Bound limit, String operator, boolean increasing) {
    }

    private record Bound(Integer constant, String symbol) {
        static Bound constant(int value) {
            return new Bound(value, null);
        }

        static Bound symbol(String value) {
            return new Bound(null, value);
        }

        static Bound fromSize(Size size) {
            return new Bound(size.constant, size.symbol);
        }
    }

    private record Size(Integer constant, String symbol) {
        static Size constant(int value) {
            return new Size(value, null);
        }

        static Size symbol(String value) {
            return new Size(null, value);
        }
    }
}
