package com.sylhetic;

import com.sylhetic.ast.Ast;
import com.sylhetic.codegen.PythonCodeGenerator;
import com.sylhetic.error.ErrorReporter;
import com.sylhetic.lexer.Lexer;
import com.sylhetic.parser.Parser;
import com.sylhetic.semantic.SemanticAnalyzer;
import com.sylhetic.token.Token;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public final class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("ব্যবহার: ./sylhetic <program.syl>");
            System.exit(64);
        }

        Path input = Paths.get(args[0]);
        if (!input.toString().endsWith(".syl")) {
            System.err.println("[ফাইল ত্রুটি] সোর্স ফাইলের extension অবশ্যই .syl হতে হবে।");
            System.exit(64);
        }
        if (!Files.isRegularFile(input)) {
            System.err.println("[ফাইল ত্রুটি] ফাইল পাওয়া যায়নি: " + input);
            System.exit(66);
        }

        try {
            String source = Files.readString(input, StandardCharsets.UTF_8);
            ErrorReporter errors = new ErrorReporter();
            List<Token> tokens = new Lexer(source, errors).scanTokens();
            Ast.Program program = new Parser(tokens, errors).parse();

            if (!errors.hasErrors()) {
                new SemanticAnalyzer(errors).analyze(program);
            }

            if (errors.hasErrors()) {
                errors.printAll();
                System.err.println("\nকম্পাইল ব্যর্থ: মোট " + errors.count() + "টি ত্রুটি পাওয়া গেছে।");
                System.exit(1);
            }

            String python = new PythonCodeGenerator().generate(program);
            Path output = outputPath(input);
            Files.writeString(output, python, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("কম্পাইল সফল: " + output);

            Process process = new ProcessBuilder("python3", output.toString()).inheritIO().start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("[রানটাইম ত্রুটি] প্রোগ্রাম exit code " + exitCode + " দিয়ে বন্ধ হয়েছে।");
                System.exit(exitCode);
            }
        } catch (IOException e) {
            System.err.println("[ফাইল ত্রুটি] " + e.getMessage());
            System.exit(74);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[রানটাইম ত্রুটি] প্রোগ্রাম চালানো বাধাগ্রস্ত হয়েছে।");
            System.exit(130);
        } catch (Exception e) {
            System.err.println("[কম্পাইলার ত্রুটি] অপ্রত্যাশিত সমস্যা হয়েছে।");
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            System.exit(70);
        }
    }

    private static Path outputPath(Path input) {
        String name = input.getFileName().toString();
        String stem = name.substring(0, name.length() - 4);
        Path parent = input.toAbsolutePath().getParent();
        return parent.resolve(stem + ".py");
    }
}
