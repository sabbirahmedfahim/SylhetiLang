package com.sylhetic.lexer;

import com.sylhetic.error.ErrorReporter;
import com.sylhetic.token.Token;
import com.sylhetic.token.TokenType;

import java.util.*;

public final class Lexer {
    private final String source;
    private final ErrorReporter errors;
    private final List<Token> tokens = new ArrayList<>();
    private final Deque<Integer> indents = new ArrayDeque<>();

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
        Map.entry("ধরো", TokenType.DHORO),
        Map.entry("জুদি", TokenType.JUDI),
        Map.entry("আরজুদি", TokenType.ARJUDI),
        Map.entry("আন্নায়", TokenType.ANNAY),
        Map.entry("যতবিল", TokenType.JOTOBIL),
        Map.entry("লাগিরও", TokenType.LAGIRWO),
        Map.entry("ভিতরে", TokenType.BHITORE),
        Map.entry("আটকিজাও", TokenType.ATKIJAO),
        Map.entry("ছালাইয়াজাও", TokenType.CHALAIYAJAAO),
        Map.entry("ফাংশন", TokenType.FUNCTION),
        Map.entry("ফিরিজাও", TokenType.RETURN),
        Map.entry("এবং", TokenType.AND),
        Map.entry("অথবা", TokenType.OR),
        Map.entry("নায়", TokenType.NOT),
        Map.entry("হাছা", TokenType.TRUE),
        Map.entry("মিছা", TokenType.FALSE),
        Map.entry("কিচ্ছুনায়", TokenType.NONE),
        Map.entry("ফুরালম্বর", TokenType.TYPE_INT),
        Map.entry("বাঙ্গালম্বর", TokenType.TYPE_FLOAT),
        Map.entry("দড়ি", TokenType.TYPE_STRING),
        Map.entry("হাছামিছা", TokenType.TYPE_BOOL),
        Map.entry("আউটফুট", TokenType.OUTPUT),
        Map.entry("ইনফুট", TokenType.INPUT),
        Map.entry("রেঞ্জ", TokenType.RANGE),
        Map.entry("সবরহুরু", TokenType.MIN),
        Map.entry("সবরবড়", TokenType.MAX),
        Map.entry("গোল", TokenType.ROUND)
    );

    public Lexer(String source, ErrorReporter errors) {
        this.source = source.replace("\r\n", "\n").replace('\r', '\n');
        this.errors = errors;
        indents.push(0);
    }

    public List<Token> scanTokens() {
        String[] lines = source.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            scanLine(lines[i], i + 1);
        }
        while (indents.size() > 1) {
            indents.pop();
            tokens.add(new Token(TokenType.DEDENT, "", null, lines.length, 1));
        }
        tokens.add(new Token(TokenType.EOF, "", null, lines.length, 1));
        return tokens;
    }

    private void scanLine(String line, int lineNo) {
        int pos = 0;
        int spaces = 0;
        while (pos < line.length() && (line.charAt(pos) == ' ' || line.charAt(pos) == '\t')) {
            if (line.charAt(pos) == '\t') {
                errors.report("লেক্সিক্যাল ত্রুটি", lineNo, pos + 1, "ট্যাব ব্যবহার করা যাবে না; প্রতি স্তরে ৪টি স্পেস ব্যবহার করুন।");
            } else {
                spaces++;
            }
            pos++;
        }

        String rest = line.substring(pos).trim();
        if (rest.isEmpty() || rest.startsWith("#")) return;

        if (spaces % 4 != 0) {
            errors.report("লেক্সিক্যাল ত্রুটি", lineNo, 1, "ইন্ডেন্টেশন অবশ্যই ৪ স্পেসের গুণিতক হতে হবে।");
        }
        handleIndent(spaces, lineNo);

        while (pos < line.length()) {
            char c = line.charAt(pos);
            int col = pos + 1;
            if (c == ' ' || c == '\t') { pos++; continue; }
            if (c == '#') break;

            switch (c) {
                case ':' -> { add(TokenType.COLON, ":", null, lineNo, col); pos++; }
                case ',' -> { add(TokenType.COMMA, ",", null, lineNo, col); pos++; }
                case '(' -> { add(TokenType.LPAREN, "(", null, lineNo, col); pos++; }
                case ')' -> { add(TokenType.RPAREN, ")", null, lineNo, col); pos++; }
                case '+' -> { add(TokenType.PLUS, "+", null, lineNo, col); pos++; }
                case '*' -> { add(TokenType.STAR, "*", null, lineNo, col); pos++; }
                case '/' -> { add(TokenType.SLASH, "/", null, lineNo, col); pos++; }
                case '%' -> { add(TokenType.PERCENT, "%", null, lineNo, col); pos++; }
                case '-' -> {
                    if (pos + 1 < line.length() && line.charAt(pos + 1) == '>') {
                        add(TokenType.ARROW, "->", null, lineNo, col); pos += 2;
                    } else { add(TokenType.MINUS, "-", null, lineNo, col); pos++; }
                }
                case '=' -> {
                    if (peek(line, pos + 1) == '=') { add(TokenType.EQEQ, "==", null, lineNo, col); pos += 2; }
                    else { add(TokenType.ASSIGN, "=", null, lineNo, col); pos++; }
                }
                case '!' -> {
                    if (peek(line, pos + 1) == '=') { add(TokenType.NEQ, "!=", null, lineNo, col); pos += 2; }
                    else { errors.report("লেক্সিক্যাল ত্রুটি", lineNo, col, "'!' একা বৈধ অপারেটর নয়; '!=' ব্যবহার করুন।"); pos++; }
                }
                case '>' -> {
                    if (peek(line, pos + 1) == '=') { add(TokenType.GTE, ">=", null, lineNo, col); pos += 2; }
                    else { add(TokenType.GT, ">", null, lineNo, col); pos++; }
                }
                case '<' -> {
                    if (peek(line, pos + 1) == '=') { add(TokenType.LTE, "<=", null, lineNo, col); pos += 2; }
                    else { add(TokenType.LT, "<", null, lineNo, col); pos++; }
                }
                case '"' -> pos = scanString(line, pos, lineNo);
                default -> {
                    if (isDigit(c)) pos = scanNumber(line, pos, lineNo);
                    else if (isBanglaIdentifierStart(c)) pos = scanIdentifier(line, pos, lineNo);
                    else {
                        errors.report("লেক্সিক্যাল ত্রুটি", lineNo, col, "অজানা অক্ষর: '" + c + "'");
                        pos++;
                    }
                }
            }
        }
        tokens.add(new Token(TokenType.NEWLINE, "\\n", null, lineNo, line.length() + 1));
    }

    private void handleIndent(int spaces, int lineNo) {
        int current = indents.peek();
        if (spaces > current) {
            if (spaces != current + 4) {
                errors.report("লেক্সিক্যাল ত্রুটি", lineNo, 1, "একবারে এক ইন্ডেন্ট স্তর (৪ স্পেস) বাড়াতে হবে।");
            }
            indents.push(spaces);
            tokens.add(new Token(TokenType.INDENT, "", null, lineNo, 1));
        } else if (spaces < current) {
            while (indents.size() > 1 && spaces < indents.peek()) {
                indents.pop();
                tokens.add(new Token(TokenType.DEDENT, "", null, lineNo, 1));
            }
            if (spaces != indents.peek()) {
                errors.report("লেক্সিক্যাল ত্রুটি", lineNo, 1, "ইন্ডেন্টেশন আগের কোনো বৈধ স্তরের সাথে মিলছে না।");
            }
        }
    }

    private int scanString(String line, int start, int lineNo) {
        StringBuilder value = new StringBuilder();
        int i = start + 1;
        while (i < line.length() && line.charAt(i) != '"') {
            if (line.charAt(i) == '\\' && i + 1 < line.length()) {
                char n = line.charAt(i + 1);
                switch (n) {
                    case 'n' -> value.append('\n');
                    case 't' -> value.append('\t');
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    default -> { value.append(n); }
                }
                i += 2;
            } else {
                value.append(line.charAt(i++));
            }
        }
        if (i >= line.length()) {
            errors.report("লেক্সিক্যাল ত্রুটি", lineNo, start + 1, "স্ট্রিং-এর শেষ ডাবল কোট (\") পাওয়া যায়নি।");
            return line.length();
        }
        String lexeme = line.substring(start, i + 1);
        add(TokenType.STRING, lexeme, value.toString(), lineNo, start + 1);
        return i + 1;
    }

    private int scanNumber(String line, int start, int lineNo) {
        int i = start;
        while (i < line.length() && isDigit(line.charAt(i))) i++;
        boolean isFloat = false;
        if (i < line.length() && line.charAt(i) == '.' && i + 1 < line.length() && isDigit(line.charAt(i + 1))) {
            isFloat = true; i++;
            while (i < line.length() && isDigit(line.charAt(i))) i++;
        }
        String raw = line.substring(start, i);
        String normalized = normalizeDigits(raw);
        try {
            if (isFloat) add(TokenType.FLOAT, raw, Double.parseDouble(normalized), lineNo, start + 1);
            else add(TokenType.INT, raw, Long.parseLong(normalized), lineNo, start + 1);
        } catch (NumberFormatException ex) {
            errors.report("লেক্সিক্যাল ত্রুটি", lineNo, start + 1, "অবৈধ সংখ্যা: " + raw);
        }
        return i;
    }

    private int scanIdentifier(String line, int start, int lineNo) {
        int i = start;
        while (i < line.length() && isBanglaIdentifierPart(line.charAt(i))) i++;
        String text = line.substring(start, i);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
        add(type, text, null, lineNo, start + 1);
        return i;
    }

    private static char peek(String s, int i) { return i < s.length() ? s.charAt(i) : '\0'; }
    private void add(TokenType type, String lexeme, Object literal, int line, int col) {
        tokens.add(new Token(type, lexeme, literal, line, col));
    }

    private static boolean isDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= '০' && c <= '৯');
    }

    private static String normalizeDigits(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= '০' && c <= '৯') b.append((char) ('0' + (c - '০')));
            else b.append(c);
        }
        return b.toString();
    }

    private static boolean isBanglaIdentifierStart(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.BENGALI && Character.getType(c) != Character.NON_SPACING_MARK;
    }

    private static boolean isBanglaIdentifierPart(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        int t = Character.getType(c);
        return block == Character.UnicodeBlock.BENGALI || t == Character.NON_SPACING_MARK || t == Character.COMBINING_SPACING_MARK;
    }
}
