package com.sylhetic.codegen;

import com.sylhetic.ast.Ast;
import com.sylhetic.token.TokenType;

import java.util.List;
import java.util.stream.Collectors;

import static com.sylhetic.token.TokenType.*;

public final class PythonCodeGenerator {
    private final StringBuilder out = new StringBuilder();
    private int indent = 0;

    public String generate(Ast.Program program) {
        out.setLength(0);
        for (Ast.Stmt stmt : program.statements()) emitStmt(stmt);
        return out.toString();
    }

    private void emitStmt(Ast.Stmt stmt) {
        if (stmt instanceof Ast.VarDecl s) {
            line(s.name().lexeme() + " = " + expr(s.initializer()));
        } else if (stmt instanceof Ast.Assign s) {
            line(s.name().lexeme() + " = " + expr(s.value()));
        } else if (stmt instanceof Ast.ExprStmt s) {
            line(expr(s.expression()));
        } else if (stmt instanceof Ast.IfStmt s) {
            line("if " + expr(s.condition()) + ":");
            emitBlock(s.thenBranch());
            for (Ast.ElifBranch e : s.elifBranches()) {
                line("elif " + expr(e.condition()) + ":");
                emitBlock(e.body());
            }
            if (!s.elseBranch().isEmpty()) {
                line("else:");
                emitBlock(s.elseBranch());
            }
        } else if (stmt instanceof Ast.WhileStmt s) {
            line("while " + expr(s.condition()) + ":");
            emitBlock(s.body());
        } else if (stmt instanceof Ast.ForStmt s) {
            line("for " + s.variable().lexeme() + " in " + expr(s.rangeCall()) + ":");
            emitBlock(s.body());
        } else if (stmt instanceof Ast.FunctionDecl s) {
            String params = s.parameters().stream().map(p -> p.name().lexeme()).collect(Collectors.joining(", "));
            line("def " + s.name().lexeme() + "(" + params + "):");
            emitBlock(s.body());
        } else if (stmt instanceof Ast.ReturnStmt s) {
            line(s.value() == null ? "return" : "return " + expr(s.value()));
        } else if (stmt instanceof Ast.BreakStmt) {
            line("break");
        } else if (stmt instanceof Ast.ContinueStmt) {
            line("continue");
        }
    }

    private void emitBlock(List<Ast.Stmt> body) {
        indent++;
        if (body.isEmpty()) line("pass");
        else for (Ast.Stmt stmt : body) emitStmt(stmt);
        indent--;
    }

    private String expr(Ast.Expr e) {
        if (e instanceof Ast.LiteralExpr x) {
            return switch (x.token().type()) {
                case STRING -> quote((String) x.value());
                case TRUE -> "True";
                case FALSE -> "False";
                case NONE -> "None";
                case INT, FLOAT -> asciiDigits(x.token().lexeme());
                default -> "None";
            };
        }
        if (e instanceof Ast.VariableExpr x) return mappedName(x.name().type(), x.name().lexeme());
        if (e instanceof Ast.GroupExpr x) return "(" + expr(x.expression()) + ")";
        if (e instanceof Ast.UnaryExpr x) {
            String op = x.operator().type() == NOT ? "not " : "-";
            return "(" + op + expr(x.right()) + ")";
        }
        if (e instanceof Ast.BinaryExpr x) {
            return "(" + expr(x.left()) + " " + mappedOperator(x.operator().type()) + " " + expr(x.right()) + ")";
        }
        if (e instanceof Ast.CallExpr x) {
            String args = x.arguments().stream().map(this::expr).collect(Collectors.joining(", "));
            return mappedName(x.callee().type(), x.callee().lexeme()) + "(" + args + ")";
        }
        throw new IllegalStateException("Unknown expression: " + e);
    }

    private String mappedName(TokenType type, String lexeme) {
        return switch (type) {
            case OUTPUT -> "print";
            case INPUT -> "input";
            case RANGE -> "range";
            case MIN -> "min";
            case MAX -> "max";
            case ROUND -> "round";
            default -> lexeme;
        };
    }

    private String mappedOperator(TokenType type) {
        return switch (type) {
            case AND -> "and";
            case OR -> "or";
            case PLUS -> "+";
            case MINUS -> "-";
            case STAR -> "*";
            case SLASH -> "/";
            case PERCENT -> "%";
            case EQEQ -> "==";
            case NEQ -> "!=";
            case GT -> ">";
            case LT -> "<";
            case GTE -> ">=";
            case LTE -> "<=";
            default -> throw new IllegalArgumentException("Unsupported operator " + type);
        };
    }

    private void line(String text) {
        out.append("    ".repeat(indent)).append(text).append('\n');
    }

    private static String asciiDigits(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= '০' && c <= '৯') b.append((char)('0' + c - '০'));
            else b.append(c);
        }
        return b.toString();
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
}
