package com.sylhetic.ast;

import com.sylhetic.token.Token;
import java.util.List;

public final class Ast {
    private Ast() {}

    public record Program(List<Stmt> statements) {}

    public sealed interface Stmt permits VarDecl, Assign, IfStmt, WhileStmt, ForStmt,
            FunctionDecl, ReturnStmt, BreakStmt, ContinueStmt, ExprStmt {}

    public record VarDecl(Token name, Token typeToken, Expr initializer) implements Stmt {}
    public record Assign(Token name, Expr value) implements Stmt {}
    public record IfStmt(Expr condition, List<Stmt> thenBranch,
                         List<ElifBranch> elifBranches, List<Stmt> elseBranch) implements Stmt {}
    public record ElifBranch(Expr condition, List<Stmt> body) {}
    public record WhileStmt(Expr condition, List<Stmt> body) implements Stmt {}
    public record ForStmt(Token variable, CallExpr rangeCall, List<Stmt> body) implements Stmt {}
    public record FunctionDecl(Token name, List<Parameter> parameters,
                               Token returnType, List<Stmt> body) implements Stmt {}
    public record Parameter(Token name, Token typeToken) {}
    public record ReturnStmt(Token keyword, Expr value) implements Stmt {}
    public record BreakStmt(Token keyword) implements Stmt {}
    public record ContinueStmt(Token keyword) implements Stmt {}
    public record ExprStmt(Expr expression) implements Stmt {}

    public sealed interface Expr permits BinaryExpr, UnaryExpr, LiteralExpr, VariableExpr,
            GroupExpr, CallExpr {}

    public record BinaryExpr(Expr left, Token operator, Expr right) implements Expr {}
    public record UnaryExpr(Token operator, Expr right) implements Expr {}
    public record LiteralExpr(Token token, Object value) implements Expr {}
    public record VariableExpr(Token name) implements Expr {}
    public record GroupExpr(Expr expression) implements Expr {}
    public record CallExpr(Token callee, List<Expr> arguments, Token closingParen) implements Expr {}
}
