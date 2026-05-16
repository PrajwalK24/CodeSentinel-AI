package com.codesentinel.analyzer.rules;

final class CodeSanitizer {
    private CodeSanitizer() {
    }

    static String stripComments(String code) {
        StringBuilder clean = new StringBuilder(code.length());
        boolean lineComment = false;
        boolean blockComment = false;
        boolean string = false;
        boolean character = false;
        boolean escaped = false;

        for (int i = 0; i < code.length(); i++) {
            char ch = code.charAt(i);
            char next = i + 1 < code.length() ? code.charAt(i + 1) : '\0';

            if (lineComment) {
                if (ch == '\n' || ch == '\r') {
                    lineComment = false;
                    clean.append(ch);
                } else {
                    clean.append(' ');
                }
                continue;
            }

            if (blockComment) {
                if (ch == '*' && next == '/') {
                    clean.append("  ");
                    i++;
                    blockComment = false;
                } else {
                    clean.append(ch == '\n' || ch == '\r' ? ch : ' ');
                }
                continue;
            }

            if (!string && !character && ch == '/' && next == '/') {
                clean.append("  ");
                i++;
                lineComment = true;
                continue;
            }

            if (!string && !character && ch == '/' && next == '*') {
                clean.append("  ");
                i++;
                blockComment = true;
                continue;
            }

            clean.append(ch);

            if (escaped) {
                escaped = false;
            } else if ((string || character) && ch == '\\') {
                escaped = true;
            } else if (!character && ch == '"') {
                string = !string;
            } else if (!string && ch == '\'') {
                character = !character;
            }
        }
        return clean.toString();
    }

    static String stripCommentsAndStrings(String code) {
        String noComments = stripComments(code);
        StringBuilder clean = new StringBuilder(noComments.length());
        boolean string = false;
        boolean character = false;
        boolean escaped = false;

        for (int i = 0; i < noComments.length(); i++) {
            char ch = noComments.charAt(i);
            if (escaped) {
                clean.append(' ');
                escaped = false;
                continue;
            }
            if ((string || character) && ch == '\\') {
                clean.append(' ');
                escaped = true;
                continue;
            }
            if (!character && ch == '"') {
                clean.append('"');
                string = !string;
                continue;
            }
            if (!string && ch == '\'') {
                clean.append('\'');
                character = !character;
                continue;
            }
            clean.append(string || character ? (ch == '\n' || ch == '\r' ? ch : ' ') : ch);
        }
        return clean.toString();
    }
}
