package com.sylhetic.semantic;

import java.util.*;

public final class SymbolTable {
    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

    public SymbolTable() { scopes.push(new HashMap<>()); }
    public void enterScope() { scopes.push(new HashMap<>()); }
    public void exitScope() { if (scopes.size() > 1) scopes.pop(); }
    public int depth() { return scopes.size() - 1; }

    public boolean declare(Symbol symbol) {
        Map<String, Symbol> current = scopes.peek();
        if (current.containsKey(symbol.name())) return false;
        current.put(symbol.name(), symbol);
        return true;
    }

    public Symbol resolve(String name) {
        for (Map<String, Symbol> scope : scopes) {
            Symbol symbol = scope.get(name);
            if (symbol != null) return symbol;
        }
        return null;
    }

    public Symbol resolveCurrent(String name) { return scopes.peek().get(name); }
}
