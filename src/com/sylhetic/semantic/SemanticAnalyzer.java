package com.sylhetic.semantic;

import com.sylhetic.ast.Ast;
import com.sylhetic.error.ErrorReporter;
import com.sylhetic.token.Token;
import com.sylhetic.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public final class SemanticAnalyzer {
    private final ErrorReporter errors;
    private final SymbolTable symbols = new SymbolTable();

    private int loopDepth = 0;
    private boolean insideFunction = false;
    private Type currentReturnType = Type.VOID;
    private String currentFunctionName = null;
    private boolean sawValueReturn = false;

    public SemanticAnalyzer(ErrorReporter errors) {
        this.errors = errors;
    }

    public void analyze(Ast.Program program) {
        for (Ast.Stmt stmt : program.statements()) {
            if (stmt instanceof Ast.FunctionDecl fn) {
                declareFunctionSignature(fn);
            }
        }
        for (Ast.Stmt stmt : program.statements()) {
            analyzeStmt(stmt, true);
        }
    }

    private void declareFunctionSignature(Ast.FunctionDecl fn) {
        if (symbols.resolveCurrent(fn.name().lexeme()) != null) {
            duplicate(fn.name(), "'" + fn.name().lexeme() + "' নামটি top-level-এ আগে ঘোষণা করা হয়েছে।");
            return;
        }
        List<Type> paramTypes = new ArrayList<>();
        for (Ast.Parameter p : fn.parameters()) {
            paramTypes.add(Type.fromTypeToken(p.typeToken()));
        }
        Type returnType = fn.returnType() == null ? Type.VOID : Type.fromTypeToken(fn.returnType());
        symbols.declare(Symbol.function(fn.name().lexeme(), returnType, paramTypes));
    }

    private void analyzeStmt(Ast.Stmt stmt, boolean topLevel) {
        if (stmt instanceof Ast.VarDecl s) {
            analyzeVarDecl(s);
            return;
        }

        if (stmt instanceof Ast.Assign s) {
            Symbol sym = symbols.resolve(s.name().lexeme());
            Type actual = typeOf(s.value());
            if (sym == null) {
                undefined(s.name(), "ভেরিয়েবল '" + s.name().lexeme() + "' আগে 'ধরো' দিয়ে ঘোষণা করা হয়নি।");
            } else if (sym.kind() != Symbol.Kind.VARIABLE) {
                semantic(s.name(), "ফাংশনের নামে সরাসরি মান assign করা যাবে না।");
            } else if (!assignable(sym.type(), actual)) {
                typeError(s.name(), "'" + sym.type().display() + "' ভেরিয়েবলে '" + actual.display() + "' মান রাখা যাবে না।");
            }
            return;
        }

        if (stmt instanceof Ast.ExprStmt s) {
            typeOf(s.expression());
            return;
        }

        if (stmt instanceof Ast.IfStmt s) {
            requireBool(s.condition(), "'জুদি' শর্ত");
            analyzeBlock(s.thenBranch());
            for (Ast.ElifBranch e : s.elifBranches()) {
                requireBool(e.condition(), "'আরজুদি' শর্ত");
                analyzeBlock(e.body());
            }
            if (!s.elseBranch().isEmpty()) {
                analyzeBlock(s.elseBranch());
            }
            return;
        }

        if (stmt instanceof Ast.WhileStmt s) {
            requireBool(s.condition(), "'যতবিল' শর্ত");
            loopDepth++;
            analyzeBlock(s.body());
            loopDepth--;
            return;
        }

        if (stmt instanceof Ast.ForStmt s) {
            Type iterable = typeOf(s.rangeCall());
            if (iterable != Type.RANGE && iterable != Type.ERROR) {
                typeError(s.variable(), "for-loop এ 'রেঞ্জ(...)' দরকার।");
            }

            loopDepth++;
            symbols.enterScope();
            if (symbols.resolve(s.variable().lexeme()) != null) {
                duplicate(s.variable(), "loop variable '" + s.variable().lexeme() + "' দৃশ্যমান scope-এ আগে থেকেই আছে; v1 shadowing সমর্থন করে না।");
            } else {
                symbols.declare(Symbol.variable(s.variable().lexeme(), Type.INT));
            }
            for (Ast.Stmt inner : s.body()) {
                analyzeStmt(inner, false);
            }
            symbols.exitScope();
            loopDepth--;
            return;
        }

        if (stmt instanceof Ast.FunctionDecl fn) {
            if (!topLevel) {
                semantic(fn.name(), "v1-এ ফাংশন শুধু top-level-এ ঘোষণা করা যাবে।");
                return;
            }
            analyzeFunctionBody(fn);
            return;
        }

        if (stmt instanceof Ast.ReturnStmt s) {
            analyzeReturn(s);
            return;
        }

        if (stmt instanceof Ast.BreakStmt s) {
            if (loopDepth == 0) {
                semantic(s.keyword(), "'আটকিজাও' শুধু loop-এর ভিতরে ব্যবহার করা যাবে।");
            }
            return;
        }

        if (stmt instanceof Ast.ContinueStmt s) {
            if (loopDepth == 0) {
                semantic(s.keyword(), "'ছালাইয়াজাও' শুধু loop-এর ভিতরে ব্যবহার করা যাবে।");
            }
        }
    }

    private void analyzeVarDecl(Ast.VarDecl s) {
        Type declared = Type.fromTypeToken(s.typeToken());
        Type actual = typeOf(s.initializer());

        if (!assignable(declared, actual)) {
            typeError(s.name(), "'" + declared.display() + "' ভেরিয়েবলে '" + actual.display() + "' মান রাখা যাবে না।");
        }

        if (symbols.resolve(s.name().lexeme()) != null) {
            duplicate(s.name(), "'" + s.name().lexeme() + "' দৃশ্যমান scope-এ আগে থেকেই ঘোষণা করা হয়েছে; v1 shadowing সমর্থন করে না।");
            return;
        }

        symbols.declare(Symbol.variable(s.name().lexeme(), declared));
    }

    private void analyzeFunctionBody(Ast.FunctionDecl fn) {
        boolean oldInside = insideFunction;
        Type oldReturn = currentReturnType;
        String oldName = currentFunctionName;
        boolean oldSawReturn = sawValueReturn;
        int oldLoopDepth = loopDepth;

        insideFunction = true;
        currentReturnType = fn.returnType() == null ? Type.VOID : Type.fromTypeToken(fn.returnType());
        currentFunctionName = fn.name().lexeme();
        sawValueReturn = false;
        loopDepth = 0;

        symbols.enterScope();
        for (Ast.Parameter p : fn.parameters()) {
            if (symbols.resolveCurrent(p.name().lexeme()) != null) {
                duplicate(p.name(), "parameter '" + p.name().lexeme() + "' একই ফাংশনে একাধিকবার আছে।");
            } else {
                symbols.declare(Symbol.variable(p.name().lexeme(), Type.fromTypeToken(p.typeToken())));
            }
        }

        for (Ast.Stmt inner : fn.body()) {
            analyzeStmt(inner, false);
        }
        symbols.exitScope();

        if (currentReturnType != Type.VOID && !sawValueReturn) {
            semantic(fn.name(), "value-returning ফাংশন '" + fn.name().lexeme() + "' এ অন্তত একটি 'ফিরিজাও <মান>' থাকতে হবে।");
        }

        insideFunction = oldInside;
        currentReturnType = oldReturn;
        currentFunctionName = oldName;
        sawValueReturn = oldSawReturn;
        loopDepth = oldLoopDepth;
    }

    private void analyzeReturn(Ast.ReturnStmt s) {
        if (!insideFunction) {
            semantic(s.keyword(), "'ফিরিজাও' শুধু ফাংশনের ভিতরে ব্যবহার করা যাবে।");
            if (s.value() != null) typeOf(s.value());
            return;
        }

        if (currentReturnType == Type.VOID) {
            if (s.value() != null) {
                Type actual = typeOf(s.value());
                typeError(s.keyword(), "procedure '" + currentFunctionName + "' কোনো মান return করতে পারে না; পাওয়া গেছে '" + actual.display() + "'।");
            }
            return;
        }

        if (s.value() == null) {
            typeError(s.keyword(), "ফাংশন '" + currentFunctionName + "' কে '" + currentReturnType.display() + "' মান return করতে হবে।");
            return;
        }

        Type actual = typeOf(s.value());
        sawValueReturn = true;
        if (!assignable(currentReturnType, actual)) {
            typeError(s.keyword(), "ফাংশনের return type '" + currentReturnType.display() + "', কিন্তু পাওয়া গেছে '" + actual.display() + "'।");
        }
    }

    private void analyzeBlock(List<Ast.Stmt> body) {
        symbols.enterScope();
        for (Ast.Stmt stmt : body) {
            analyzeStmt(stmt, false);
        }
        symbols.exitScope();
    }

    private Type typeOf(Ast.Expr expr) {
        if (expr instanceof Ast.LiteralExpr e) {
            return switch (e.token().type()) {
                case INT -> Type.INT;
                case FLOAT -> Type.FLOAT;
                case STRING -> Type.STRING;
                case TRUE, FALSE -> Type.BOOL;
                case NONE -> Type.NONE;
                default -> Type.ERROR;
            };
        }

        if (expr instanceof Ast.VariableExpr e) {
            if (isBuiltin(e.name().type())) {
                semantic(e.name(), "built-in '" + e.name().lexeme() + "' কে function call হিসেবে ব্যবহার করুন।");
                return Type.ERROR;
            }
            Symbol s = symbols.resolve(e.name().lexeme());
            if (s == null) {
                undefined(e.name(), "'" + e.name().lexeme() + "' ঘোষণা করা হয়নি।");
                return Type.ERROR;
            }
            if (s.kind() == Symbol.Kind.FUNCTION) {
                semantic(e.name(), "ফাংশন '" + e.name().lexeme() + "' ব্যবহার করতে (...) দিয়ে call করুন।");
                return Type.ERROR;
            }
            return s.type();
        }

        if (expr instanceof Ast.GroupExpr e) {
            return typeOf(e.expression());
        }

        if (expr instanceof Ast.UnaryExpr e) {
            Type right = typeOf(e.right());
            if (e.operator().type() == TokenType.MINUS) {
                if (!right.isNumeric() && right != Type.ERROR) {
                    typeError(e.operator(), "unary '-' শুধু সংখ্যার সাথে ব্যবহার করা যাবে।");
                    return Type.ERROR;
                }
                return right;
            }
            if (e.operator().type() == TokenType.NOT) {
                if (right != Type.BOOL && right != Type.ERROR) {
                    typeError(e.operator(), "'নায়' শুধু হাছামিছা expression-এর সাথে ব্যবহার করা যাবে।");
                    return Type.ERROR;
                }
                return Type.BOOL;
            }
            return Type.ERROR;
        }

        if (expr instanceof Ast.BinaryExpr e) {
            return binaryType(e);
        }

        if (expr instanceof Ast.CallExpr e) {
            return callType(e);
        }

        return Type.ERROR;
    }

    private Type binaryType(Ast.BinaryExpr e) {
        Type left = typeOf(e.left());
        Type right = typeOf(e.right());
        TokenType op = e.operator().type();

        if (left == Type.ERROR || right == Type.ERROR) return Type.ERROR;

        if (op == TokenType.AND || op == TokenType.OR) {
            if (left != Type.BOOL || right != Type.BOOL) {
                typeError(e.operator(), "logical operator-এর দুই পাশেই হাছামিছা দরকার।");
                return Type.ERROR;
            }
            return Type.BOOL;
        }

        if (op == TokenType.PLUS && left == Type.STRING && right == Type.STRING) {
            return Type.STRING;
        }

        if (op == TokenType.PLUS || op == TokenType.MINUS || op == TokenType.STAR || op == TokenType.PERCENT) {
            if (!left.isNumeric() || !right.isNumeric()) {
                typeError(e.operator(), "এই arithmetic operator শুধু সংখ্যার সাথে ব্যবহার করা যাবে।");
                return Type.ERROR;
            }
            return Type.promoteNumeric(left, right);
        }

        if (op == TokenType.SLASH) {
            if (!left.isNumeric() || !right.isNumeric()) {
                typeError(e.operator(), "'/' শুধু সংখ্যার সাথে ব্যবহার করা যাবে।");
                return Type.ERROR;
            }
            return Type.FLOAT;
        }

        if (op == TokenType.EQEQ || op == TokenType.NEQ) {
            if (!(left == right || (left.isNumeric() && right.isNumeric()))) {
                typeError(e.operator(), "'=='/'!=' তুলনায় compatible type দরকার।");
                return Type.ERROR;
            }
            return Type.BOOL;
        }

        if (op == TokenType.GT || op == TokenType.LT || op == TokenType.GTE || op == TokenType.LTE) {
            if (!left.isNumeric() || !right.isNumeric()) {
                typeError(e.operator(), "ordering comparison শুধু সংখ্যার জন্য সমর্থিত।");
                return Type.ERROR;
            }
            return Type.BOOL;
        }

        return Type.ERROR;
    }

    private Type callType(Ast.CallExpr call) {
        Token callee = call.callee();
        List<Type> args = new ArrayList<>();
        for (Ast.Expr arg : call.arguments()) {
            args.add(typeOf(arg));
        }

        switch (callee.type()) {
            case OUTPUT:
                return Type.VOID;

            case INPUT:
                if (args.size() > 1) {
                    arity(callee, "ইনফুট() ০ বা ১টি argument নেয়।");
                }
                if (args.size() == 1 && args.get(0) != Type.STRING && args.get(0) != Type.ERROR) {
                    typeError(callee, "ইনফুট() prompt হলে তা দড়ি হতে হবে।");
                }
                return Type.STRING;

            case RANGE:
                if (args.size() < 1 || args.size() > 3) {
                    arity(callee, "রেঞ্জ() ১, ২ অথবা ৩টি argument নেয়।");
                }
                for (Type t : args) {
                    if (t != Type.INT && t != Type.ERROR) {
                        typeError(callee, "রেঞ্জ()-এর সব argument ফুরালম্বর হতে হবে।");
                        break;
                    }
                }
                return Type.RANGE;

            case MIN:
            case MAX:
                if (args.size() < 2) {
                    arity(callee, "সবরহুরু/সবরবড় কমপক্ষে ২টি numeric argument নেয়।");
                }
                Type result = Type.INT;
                for (Type t : args) {
                    if (!t.isNumeric() && t != Type.ERROR) {
                        typeError(callee, "সবরহুরু/সবরবড় শুধু numeric argument নেয়।");
                        return Type.ERROR;
                    }
                    if (t == Type.FLOAT) result = Type.FLOAT;
                }
                return result;

            case ROUND:
                if (args.size() < 1 || args.size() > 2) {
                    arity(callee, "গোল() ১ অথবা ২টি argument নেয়।");
                    return Type.ERROR;
                }
                if (!args.get(0).isNumeric() && args.get(0) != Type.ERROR) {
                    typeError(callee, "গোল()-এর প্রথম argument numeric হতে হবে।");
                    return Type.ERROR;
                }
                if (args.size() == 2 && args.get(1) != Type.INT && args.get(1) != Type.ERROR) {
                    typeError(callee, "গোল()-এর দ্বিতীয় argument ফুরালম্বর হতে হবে।");
                    return Type.ERROR;
                }
                return args.size() == 1 ? Type.INT : args.get(0);

            default:
                Symbol fn = symbols.resolve(callee.lexeme());
                if (fn == null) {
                    undefined(callee, "ফাংশন '" + callee.lexeme() + "' ঘোষণা করা হয়নি।");
                    return Type.ERROR;
                }
                if (fn.kind() != Symbol.Kind.FUNCTION) {
                    semantic(callee, "'" + callee.lexeme() + "' একটি ফাংশন নয়।");
                    return Type.ERROR;
                }
                if (fn.parameterTypes().size() != args.size()) {
                    arity(callee, "ফাংশনটি " + fn.parameterTypes().size() + "টি argument চায়, কিন্তু দেওয়া হয়েছে " + args.size() + "টি।");
                }
                int count = Math.min(fn.parameterTypes().size(), args.size());
                for (int i = 0; i < count; i++) {
                    if (!assignable(fn.parameterTypes().get(i), args.get(i))) {
                        typeError(callee, (i + 1) + " নম্বর argument-এর type হওয়া উচিত '" + fn.parameterTypes().get(i).display() + "'।");
                    }
                }
                return fn.type();
        }
    }

    private void requireBool(Ast.Expr expr, String context) {
        Type t = typeOf(expr);
        if (t != Type.BOOL && t != Type.ERROR) {
            typeError(tokenOf(expr), context + " অবশ্যই হাছামিছা type হতে হবে।");
        }
    }

    private static boolean assignable(Type target, Type actual) {
        if (target == Type.ERROR || actual == Type.ERROR) return true;
        if (target == actual) return true;
        return target == Type.FLOAT && actual == Type.INT;
    }

    private static boolean isBuiltin(TokenType type) {
        return type == TokenType.OUTPUT || type == TokenType.INPUT || type == TokenType.RANGE
                || type == TokenType.MIN || type == TokenType.MAX || type == TokenType.ROUND;
    }

    private Token tokenOf(Ast.Expr expr) {
        if (expr instanceof Ast.LiteralExpr e) return e.token();
        if (expr instanceof Ast.VariableExpr e) return e.name();
        if (expr instanceof Ast.UnaryExpr e) return e.operator();
        if (expr instanceof Ast.BinaryExpr e) return e.operator();
        if (expr instanceof Ast.CallExpr e) return e.callee();
        if (expr instanceof Ast.GroupExpr e) return tokenOf(e.expression());
        throw new IllegalStateException("Unknown expression node");
    }

    private void typeError(Token t, String m) {
        errors.report("টাইপ ত্রুটি", t.line(), t.column(), m);
    }

    private void semantic(Token t, String m) {
        errors.report("সেমান্টিক ত্রুটি", t.line(), t.column(), m);
    }

    private void undefined(Token t, String m) {
        errors.report("অঘোষিত পরিচয়", t.line(), t.column(), m);
    }

    private void duplicate(Token t, String m) {
        errors.report("ডুপ্লিকেট ঘোষণা", t.line(), t.column(), m);
    }

    private void arity(Token t, String m) {
        errors.report("সেমান্টিক ত্রুটি", t.line(), t.column(), m);
    }
}
