package com.sylhetic.semantic;

import com.sylhetic.token.Token;
import com.sylhetic.token.TokenType;

public enum Type {
    INT("ফুরালম্বর"), FLOAT("বাঙ্গালম্বর"), STRING("দড়ি"), BOOL("হাছামিছা"),
    NONE("কিচ্ছুনায়"), RANGE("রেঞ্জ"), VOID("procedure"), ERROR("ত্রুটি");

    private final String display;
    Type(String display) { this.display = display; }
    public String display() { return display; }

    public static Type fromTypeToken(Token token) {
        return switch (token.type()) {
            case TYPE_INT -> INT;
            case TYPE_FLOAT -> FLOAT;
            case TYPE_STRING -> STRING;
            case TYPE_BOOL -> BOOL;
            default -> ERROR;
        };
    }

    public boolean isNumeric() { return this == INT || this == FLOAT; }
    public static Type promoteNumeric(Type a, Type b) {
        if (!a.isNumeric() || !b.isNumeric()) return ERROR;
        return (a == FLOAT || b == FLOAT) ? FLOAT : INT;
    }
}
