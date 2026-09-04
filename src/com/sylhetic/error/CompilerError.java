package com.sylhetic.error;

public record CompilerError(String category, int line, int column, String message) {
    @Override
    public String toString() {
        return "[" + category + "] লাইন " + line + ", কলাম " + column + ": " + message;
    }
}
