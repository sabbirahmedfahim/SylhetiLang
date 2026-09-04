package com.sylhetic.parser;

import com.sylhetic.ast.Ast;
import com.sylhetic.error.ErrorReporter;
import com.sylhetic.token.Token;
import com.sylhetic.token.TokenType;

import java.util.ArrayList;
import java.util.List;

import static com.sylhetic.token.TokenType.*;

public final class Parser {
    private final List<Token> tokens;
    private final ErrorReporter errors;
    private int current = 0;

    private static final class ParseFailure extends RuntimeException {}

    public Parser(List<Token> tokens, ErrorReporter errors) {
        this.tokens = tokens;
        this.errors = errors;
    }

    public Ast.Program parse() {
        List<Ast.Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            if (match(NEWLINE, DEDENT)) continue;
            Ast.Stmt stmt = declaration();
            if (stmt != null) statements.add(stmt);
        }
        return new Ast.Program(statements);
    }

    private Ast.Stmt declaration() {
        try {
            if (match(DHORO)) return varDeclaration();
            if (match(FUNCTION)) return functionDeclaration();
            return statement();
        } catch (ParseFailure ignored) {
            synchronize();
            return null;
        }
    }

    private Ast.Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "'ধরো'-র পরে একটি বাংলা ভেরিয়েবল নাম দরকার।");
        consume(COLON, "ভেরিয়েবলের নামের পরে ':' দরকার।");
        Token type = consumeType("':'-এর পরে একটি বৈধ টাইপ দরকার।");
        consume(ASSIGN, "ঘোষণায় '=' দরকার।");
        Ast.Expr init = expression();
        endStatement("ভেরিয়েবল ঘোষণার পরে নতুন লাইন দরকার।");
        return new Ast.VarDecl(name, type, init);
    }

    private Ast.Stmt functionDeclaration() {
        Token name = consume(IDENTIFIER, "'ফাংশন'-এর পরে ফাংশনের নাম দরকার।");
        consume(LPAREN, "ফাংশনের নামের পরে '(' দরকার।");
        List<Ast.Parameter> params = new ArrayList<>();
        if (!check(RPAREN)) {
            do {
                Token pName = consume(IDENTIFIER, "প্যারামিটারের বাংলা নাম দরকার।");
                consume(COLON, "প্যারামিটার নামের পরে ':' দরকার।");
                Token pType = consumeType("প্যারামিটারের টাইপ দরকার।");
                params.add(new Ast.Parameter(pName, pType));
            } while (match(COMMA));
        }
        consume(RPAREN, "প্যারামিটার তালিকার শেষে ')' দরকার।");
        Token returnType = null;
        if (match(ARROW)) returnType = consumeType("'->'-এর পরে রিটার্ন টাইপ দরকার।");
        consume(COLON, "ফাংশন হেডারের শেষে ':' দরকার।");
        List<Ast.Stmt> body = block();
        return new Ast.FunctionDecl(name, params, returnType, body);
    }

    private Ast.Stmt statement() {
        if (match(JUDI)) return ifStatement();
        if (match(JOTOBIL)) return whileStatement();
        if (match(LAGIRWO)) return forStatement();
        if (match(RETURN)) return returnStatement(previous());
        if (match(ATKIJAO)) {
            Token t = previous(); endStatement("'আটকিজাও'-এর পরে নতুন লাইন দরকার।"); return new Ast.BreakStmt(t);
        }
        if (match(CHALAIYAJAAO)) {
            Token t = previous(); endStatement("'ছালাইয়াজাও'-এর পরে নতুন লাইন দরকার।"); return new Ast.ContinueStmt(t);
        }
        if (check(IDENTIFIER) && checkNext(ASSIGN)) return assignmentStatement();
        Ast.Expr expr = expression();
        endStatement("এক্সপ্রেশনের পরে নতুন লাইন দরকার।");
        return new Ast.ExprStmt(expr);
    }

    private Ast.Stmt assignmentStatement() {
        Token name = advance();
        consume(ASSIGN, "অ্যাসাইনমেন্টে '=' দরকার।");
        Ast.Expr value = expression();
        endStatement("অ্যাসাইনমেন্টের পরে নতুন লাইন দরকার।");
        return new Ast.Assign(name, value);
    }

    private Ast.Stmt ifStatement() {
        Ast.Expr condition = expression();
        consume(COLON, "'জুদি' শর্তের পরে ':' দরকার।");
        List<Ast.Stmt> thenBranch = block();

        List<Ast.ElifBranch> elifs = new ArrayList<>();
        while (match(ARJUDI)) {
            Ast.Expr c = expression();
            consume(COLON, "'আরজুদি' শর্তের পরে ':' দরকার।");
            elifs.add(new Ast.ElifBranch(c, block()));
        }

        List<Ast.Stmt> elseBranch = List.of();
        if (match(ANNAY)) {
            consume(COLON, "'আন্নায়'-এর পরে ':' দরকার।");
            elseBranch = block();
        }
        return new Ast.IfStmt(condition, thenBranch, elifs, elseBranch);
    }

    private Ast.Stmt whileStatement() {
        Ast.Expr condition = expression();
        consume(COLON, "'যতবিল' শর্তের পরে ':' দরকার।");
        return new Ast.WhileStmt(condition, block());
    }

    private Ast.Stmt forStatement() {
        Token variable = consume(IDENTIFIER, "'লাগিরও'-এর পরে লুপ ভেরিয়েবল দরকার।");
        consume(BHITORE, "লুপ ভেরিয়েবলের পরে 'ভিতরে' দরকার।");
        Ast.Expr iterable = expression();
        if (!(iterable instanceof Ast.CallExpr call) || call.callee().type() != RANGE) {
            error(variable, "v1-এ 'লাগিরও' শুধু 'রেঞ্জ(...)' এর সাথে ব্যবহার করা যাবে।");
            throw new ParseFailure();
        }
        consume(COLON, "for লুপের শেষে ':' দরকার।");
        return new Ast.ForStmt(variable, (Ast.CallExpr) iterable, block());
    }

    private Ast.Stmt returnStatement(Token keyword) {
        Ast.Expr value = null;
        if (!check(NEWLINE) && !check(EOF)) value = expression();
        endStatement("'ফিরিজাও'-এর পরে নতুন লাইন দরকার।");
        return new Ast.ReturnStmt(keyword, value);
    }

    private List<Ast.Stmt> block() {
        consume(NEWLINE, "':'-এর পরে নতুন লাইন দরকার।");
        consume(INDENT, "ব্লকের জন্য ঠিক ৪ স্পেস ইন্ডেন্ট দরকার।");
        List<Ast.Stmt> body = new ArrayList<>();
        while (!check(DEDENT) && !check(EOF)) {
            if (match(NEWLINE)) continue;
            Ast.Stmt stmt = declaration();
            if (stmt != null) body.add(stmt);
        }
        consume(DEDENT, "ব্লক সঠিকভাবে শেষ হয়নি।");
        return body;
    }

    private Ast.Expr expression() { return or(); }

    private Ast.Expr or() {
        Ast.Expr expr = and();
        while (match(OR)) expr = new Ast.BinaryExpr(expr, previous(), and());
        return expr;
    }

    private Ast.Expr and() {
        Ast.Expr expr = not();
        while (match(AND)) expr = new Ast.BinaryExpr(expr, previous(), not());
        return expr;
    }

    private Ast.Expr not() {
        if (match(NOT)) return new Ast.UnaryExpr(previous(), not());
        return comparison();
    }

    private Ast.Expr comparison() {
        Ast.Expr expr = term();
        if (match(EQEQ, NEQ, GT, LT, GTE, LTE)) {
            Token op = previous();
            Ast.Expr right = term();
            expr = new Ast.BinaryExpr(expr, op, right);
            if (check(EQEQ) || check(NEQ) || check(GT) || check(LT) || check(GTE) || check(LTE)) {
                error(peek(), "চেইনড comparison v1-এ সমর্থিত নয়।");
                throw new ParseFailure();
            }
        }
        return expr;
    }

    private Ast.Expr term() {
        Ast.Expr expr = factor();
        while (match(PLUS, MINUS)) expr = new Ast.BinaryExpr(expr, previous(), factor());
        return expr;
    }

    private Ast.Expr factor() {
        Ast.Expr expr = unary();
        while (match(STAR, SLASH, PERCENT)) expr = new Ast.BinaryExpr(expr, previous(), unary());
        return expr;
    }

    private Ast.Expr unary() {
        if (match(MINUS)) return new Ast.UnaryExpr(previous(), unary());
        return call();
    }

    private Ast.Expr call() {
        Ast.Expr expr = primary();
        while (match(LPAREN)) {
            if (!(expr instanceof Ast.VariableExpr v)) {
                error(previous(), "শুধু ফাংশন/বিল্ট-ইন নাম কল করা যাবে।");
                throw new ParseFailure();
            }
            List<Ast.Expr> args = new ArrayList<>();
            if (!check(RPAREN)) {
                do { args.add(expression()); } while (match(COMMA));
            }
            Token close = consume(RPAREN, "ফাংশন কলের শেষে ')' দরকার।");
            expr = new Ast.CallExpr(v.name(), args, close);
        }
        return expr;
    }

    private Ast.Expr primary() {
        if (match(INT, FLOAT, STRING)) return new Ast.LiteralExpr(previous(), previous().literal());
        if (match(TRUE)) return new Ast.LiteralExpr(previous(), true);
        if (match(FALSE)) return new Ast.LiteralExpr(previous(), false);
        if (match(NONE)) return new Ast.LiteralExpr(previous(), null);
        if (match(IDENTIFIER, OUTPUT, INPUT, RANGE, MIN, MAX, ROUND)) return new Ast.VariableExpr(previous());
        if (match(LPAREN)) {
            Ast.Expr expr = expression();
            consume(RPAREN, "বন্ধনী ')' দরকার।");
            return new Ast.GroupExpr(expr);
        }
        error(peek(), "একটি expression প্রত্যাশিত ছিল।");
        throw new ParseFailure();
    }

    private void endStatement(String message) {
        consume(NEWLINE, message);
    }

    private Token consumeType(String message) {
        if (match(TYPE_INT, TYPE_FLOAT, TYPE_STRING, TYPE_BOOL)) return previous();
        error(peek(), message);
        throw new ParseFailure();
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        error(peek(), message);
        throw new ParseFailure();
    }

    private void error(Token token, String message) {
        errors.report("সিনট্যাক্স ত্রুটি", token.line(), token.column(), message);
    }

    private void synchronize() {
        if (isAtEnd() || check(DEDENT)) return;

        while (!isAtEnd() && !check(NEWLINE) && !check(DEDENT)) {
            advance();
        }
        if (check(NEWLINE)) advance();
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) { advance(); return true; }
        }
        return false;
    }
    private boolean check(TokenType type) { return !isAtEnd() && peek().type() == type; }
    private boolean checkNext(TokenType type) { return current + 1 < tokens.size() && tokens.get(current + 1).type() == type; }
    private Token advance() { if (!isAtEnd()) current++; return previous(); }
    private boolean isAtEnd() { return peek().type() == EOF; }
    private Token peek() { return tokens.get(current); }
    private Token previous() { return tokens.get(current - 1); }
    private Token previousSafe() { return current == 0 ? tokens.get(0) : tokens.get(current - 1); }
}
