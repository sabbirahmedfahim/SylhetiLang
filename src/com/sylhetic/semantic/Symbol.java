package com.sylhetic.semantic;

import java.util.List;

public record Symbol(String name, Kind kind, Type type, List<Type> parameterTypes) {
    public enum Kind { VARIABLE, FUNCTION }

    public static Symbol variable(String name, Type type) {
        return new Symbol(name, Kind.VARIABLE, type, List.of());
    }

    public static Symbol function(String name, Type returnType, List<Type> params) {
        return new Symbol(name, Kind.FUNCTION, returnType, List.copyOf(params));
    }
}
