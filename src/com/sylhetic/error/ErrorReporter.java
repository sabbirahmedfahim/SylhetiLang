package com.sylhetic.error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ErrorReporter {
    private final List<CompilerError> errors = new ArrayList<>();

    public void report(String category, int line, int column, String message) {
        errors.add(new CompilerError(category, line, column, message));
    }

    public boolean hasErrors() { return !errors.isEmpty(); }
    public int count() { return errors.size(); }
    public List<CompilerError> all() { return Collections.unmodifiableList(errors); }

    public void printAll() {
        for (CompilerError error : errors) {
            System.err.println(error);
        }
    }
}
