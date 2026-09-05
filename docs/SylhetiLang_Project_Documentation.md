# SylhetiLang Project Documentation

## CSE-4114 Compiler Design and Construction Sessional

---

# 1. Project Identity

**Project name:** SylhetiLang  
**Project type:** Team compiler project  
**Team size:** 5 members  
**Compiler implementation language:** Java  
**Source language:** SylhetiLang (`.syl`)  
**Target language:** Python 3  
**IDE:** Visual Studio Code  
**Tested JDK:** Java 24.0.1  
**Java compatibility target:** Java 21 via `--release 21`  
**Tested Python:** Python 3.14.6  
**Build system:** Plain `javac` and shell scripts; no Maven/Gradle  
**Encoding:** UTF-8

SylhetiLang is a Bengali-script, Sylheti-inspired statically checked programming language. The language uses Bengali vocabulary and Bengali identifiers while keeping familiar programming symbols such as `+`, `-`, `*`, `/`, `=`, `==`, `>=`, parentheses, commas, and colons.

The compiler is not a string-replacement translator. It implements real compiler stages: lexical analysis, token generation, parsing, AST construction, semantic analysis, type checking, symbol-table management, error handling, and target code generation.

---

# 2. Course Goal and Project Goal

The course project requires an original Bangla programming language and a working compiler implemented in a compiled or managed language. The generated target must be executable Java or Python.

The project therefore has two goals:

1. Demonstrate compiler-design principles in a working system.
2. Present a language whose surface vocabulary is more accessible to Bengali/Sylheti speakers.

The current implementation exceeds the minimum core with four types, functions, recursion, a range-based for loop, static checking, Bengali diagnostics, and several built-ins.

---

# 3. Current Project Structure

```text
SylhetiLang_Run/
├── .gitignore
├── .vscode/
│   ├── settings.json
│   └── tasks.json
├── LICENSE
├── README.md
├── amar_codes/
│   ├── main.syl
│   └── main.py
├── build/
│   ├── classes/
│   └── sources.txt
├── build.sh
├── grammar.bnf
├── samples/
│   ├── err_test.syl
│   ├── functions.syl
│   ├── functions.py
│   ├── recursion.syl
│   ├── recursion.py
│   ├── syntax_err.syl
│   ├── test.syl
│   └── test.py
├── src/com/sylhetic/
│   ├── Main.java
│   ├── ast/
│   │   └── Ast.java
│   ├── codegen/
│   │   └── PythonCodeGenerator.java
│   ├── error/
│   │   ├── CompilerError.java
│   │   └── ErrorReporter.java
│   ├── lexer/
│   │   └── Lexer.java
│   ├── parser/
│   │   └── Parser.java
│   ├── semantic/
│   │   ├── SemanticAnalyzer.java
│   │   ├── Symbol.java
│   │   ├── SymbolTable.java
│   │   └── Type.java
│   └── token/
│       ├── Token.java
│       └── TokenType.java
├── sylhetic
└── test.sh
```

---

# 4. Actual End-to-End Flow

The current implementation follows this pipeline:

```text
SylhetiLang source (.syl)
        |
        v
UTF-8 source loading
        |
        v
Lexer
        |
        v
Token list
        |
        v
Recursive-descent Parser
        |
        v
AST
        |
        v
Semantic Analyzer + Type Checker + Symbol Table
        |
        v
Python Code Generator
        |
        v
Generated .py file
        |
        v
python3 process
        |
        v
Program output
```

There is no separate TAC, optimizer, assembly, register-allocation, or machine-code stage in the current version.

This is a source-to-source compiler: SylhetiLang is the source language and Python is the generated target language.

---

# 5. Build and Run

## Build compiler

From the project root:

```bash
./build.sh
```

`build.sh` performs these actions:

1. Determines the project root.
2. Creates `build/classes` if required.
3. Finds every Java source file under `src`.
4. Writes the source list to `build/sources.txt`.
5. Invokes `javac` using Java 21 compatibility and UTF-8.
6. Places `.class` files in `build/classes`.

The effective Java compile configuration is based on:

```bash
javac --release 21 -encoding UTF-8 -d build/classes
```

## Compile and run SylhetiLang

```bash
./sylhetic amar_codes/main.syl
```

The current version performs both compilation and execution in one command.

It generates:

```text
amar_codes/main.py
```

and then automatically runs:

```text
python3 amar_codes/main.py
```

The automatic target execution is implemented in `Main.java` through `ProcessBuilder`.

## Run all tests

```bash
./test.sh
```

The test script checks valid programs, functions, recursion, semantic errors, and parser syntax errors.

---

# 6. Main.java: Compiler Driver

`src/com/sylhetic/Main.java` controls the overall compilation process.

Its responsibilities are:

1. Validate command-line arguments.
2. Require the `.syl` extension.
3. Verify the input exists as a regular file.
4. Read source as UTF-8.
5. Create the error reporter.
6. Run the lexer.
7. Run the parser.
8. Run semantic analysis if lexical/parsing stages have not already produced errors.
9. Stop if errors exist.
10. Generate Python.
11. Write the `.py` file beside the `.syl` source.
12. Print the generated file path.
13. Start Python 3 automatically.
14. Report target runtime failure if the generated program returns a nonzero exit code.
15. Catch unexpected compiler failures gracefully.

Important point for viva: `Main.java` is the orchestration layer. It does not itself perform lexical analysis, parsing, or type checking; it invokes the specialized components.

---

# 7. Lexical Analysis

## File

`src/com/sylhetic/lexer/Lexer.java`

## Purpose

The lexer converts raw Unicode characters into tokens.

Example source:

```text
ধরো বয়স : ফুরালম্বর = ২২
```

Important token sequence:

```text
DHORO IDENTIFIER COLON TYPE_INT ASSIGN INT NEWLINE
```

## Keyword recognition

The lexer contains a static keyword mapping. Important mappings include:

| SylhetiLang | Token type | Meaning |
|---|---|---|
| `ধরো` | `DHORO` | declaration |
| `জুদি` | `JUDI` | if |
| `আরজুদি` | `ARJUDI` | elif |
| `আন্নায়` | `ANNAY` | else |
| `যতবিল` | `JOTOBIL` | while |
| `লাগিরও` | `LAGIRWO` | for |
| `ভিতরে` | `BHITORE` | in |
| `আটকিজাও` | `ATKIJAO` | break |
| `ছালাইয়াজাও` | `CHALAIYAJAAO` | continue |
| `ফাংশন` | `FUNCTION` | function |
| `ফিরিজাও` | `RETURN` | return |
| `এবং` | `AND` | logical and |
| `অথবা` | `OR` | logical or |
| `নায়` | `NOT` | logical not |
| `হাছা` | `TRUE` | true |
| `মিছা` | `FALSE` | false |
| `কিচ্ছুনায়` | `NONE` | none |
| `ফুরালম্বর` | `TYPE_INT` | int |
| `বাঙ্গালম্বর` | `TYPE_FLOAT` | float |
| `দড়ি` | `TYPE_STRING` | string |
| `হাছামিছা` | `TYPE_BOOL` | boolean |
| `আউটফুট` | `OUTPUT` | output |
| `ইনফুট` | `INPUT` | input |
| `রেঞ্জ` | `RANGE` | range |
| `সবরহুরু` | `MIN` | min |
| `সবরবড়` | `MAX` | max |
| `গোল` | `ROUND` | round |

## Bengali identifiers

The current language restricts user identifiers to the Bengali Unicode block. A scanned Bengali word becomes a keyword if it is found in the keyword table; otherwise it becomes `IDENTIFIER`.

## Numeric literals

Both ASCII and Bengali digits are recognized:

```text
123
১২৩
12.5
১২.৫
```

Bengali digits are normalized to ASCII before Java parses the numeric value.

## String literals

Strings use double quotes. The lexer supports escape processing including newline, tab, double quote, and backslash.

## Comments

A `#` causes the remaining text on that line to be ignored by the lexer.

## Indentation

Blocks are indentation-based. The lexer requires four-space indentation levels and rejects tabs.

The lexer uses a stack of indentation widths:

```text
0
4
8
...
```

When indentation increases, it emits `INDENT`. When indentation decreases, it emits one or more `DEDENT` tokens.

This is a lexical design decision because the parser needs explicit tokens to represent blocks even though the source uses whitespace instead of braces.

---

# 8. Tokens

## TokenType.java

`TokenType.java` defines every possible token category.

Categories include:

- structural: `NEWLINE`, `INDENT`, `DEDENT`, `EOF`
- literals: `INT`, `FLOAT`, `STRING`
- identifiers: `IDENTIFIER`
- keywords
- built-ins
- delimiters
- arithmetic operators
- assignment and comparison operators

## Token.java

Every token stores:

```text
type
lexeme
literal
line
column
```

A conceptual token may look like:

```text
Token(DHORO, "ধরো", null, 1, 1)
```

Line and column information is carried forward so later phases can report useful diagnostics.

---

# 9. Parsing and Syntax Analysis

## File

`src/com/sylhetic/parser/Parser.java`

## Technique

Handwritten recursive-descent parser.

Each significant grammar level is represented by a parser method.

Important methods include:

```text
parse
declaration
varDeclaration
functionDeclaration
statement
assignmentStatement
ifStatement
whileStatement
forStatement
returnStatement
block
expression
or
and
not
comparison
term
factor
unary
call
primary
```

## Why recursive descent

It maps naturally to the BNF, is easy to debug, and is easy to defend during a live review because each grammar production can be associated with a parser method.

## Blocks

A source block is represented using:

```text
NEWLINE INDENT ... DEDENT
```

The parser does not count spaces itself. That responsibility belongs to the lexer.

## Assignment recognition

An identifier followed by `=` is parsed as assignment using token lookahead.

## Operator precedence

The expression parser uses several levels:

```text
অথবা
এবং
নায়
comparisons
+ -
* / %
unary -
call / primary
```

This means:

```text
২ + ৩ * ৪
```

is structurally understood as:

```text
২ + (৩ * ৪)
```

rather than:

```text
(২ + ৩) * ৪
```

---

# 10. Grammar

The formal grammar is stored in `grammar.bnf`.

Major grammar areas are:

- program and top-level statements
- indentation-based blocks
- typed declaration
- assignment
- return
- break
- continue
- expression statement
- if/elif/else
- while
- range-based for
- function declaration
- parameter lists
- optional function return type
- type names
- logical expressions
- comparisons
- arithmetic expressions
- unary expressions
- calls
- literals and identifiers

The grammar intentionally excludes lists, classes, exceptions, exponentiation, and chained comparisons.

---

# 11. AST: Abstract Syntax Tree

## File

`src/com/sylhetic/ast/Ast.java`

The parser directly creates AST nodes.

## Root node

```text
Program
```

## Statement nodes

```text
VarDecl
Assign
IfStmt
WhileStmt
ForStmt
FunctionDecl
ReturnStmt
BreakStmt
ContinueStmt
ExprStmt
```

## Expression nodes

```text
BinaryExpr
UnaryExpr
LiteralExpr
VariableExpr
GroupExpr
CallExpr
```

The AST uses Java records to represent immutable compiler structures and sealed interfaces to define the permitted statement/expression node families.

## Example AST idea

For:

```text
ক + খ * ২
```

roughly:

```text
BinaryExpr(+)
├── VariableExpr(ক)
└── BinaryExpr(*)
    ├── VariableExpr(খ)
    └── LiteralExpr(২)
```

The AST is the central structured representation shared between parsing, semantic analysis, and code generation.

---

# 12. Semantic Analysis

## File

`src/com/sylhetic/semantic/SemanticAnalyzer.java`

Semantic analysis checks rules that grammar alone cannot guarantee.

It handles:

- duplicate declarations
- undefined identifiers
- assignment compatibility
- expression type checking
- numeric promotion
- string concatenation rule
- comparison rules
- boolean condition rules
- function declaration rules
- argument count checking
- argument type checking
- function return checking
- range argument checking
- break/continue context checking
- function/procedure distinction

Example:

```text
ধরো সংখ্যা : ফুরালম্বর = "ভুল"
```

The parser accepts this because its grammar is valid. The semantic analyzer rejects it because the declared type is integer and the initializer type is string.

This distinction is important:

```text
Parser = Is the sentence grammatically valid?
Semantic Analyzer = Does the valid sentence make sense under language rules?
```

---

# 13. Type System

## File

`src/com/sylhetic/semantic/Type.java`

User-visible types:

| SylhetiLang | Internal semantic type |
|---|---|
| `ফুরালম্বর` | `INT` |
| `বাঙ্গালম্বর` | `FLOAT` |
| `দড়ি` | `STRING` |
| `হাছামিছা` | `BOOL` |

Internal support types also exist:

```text
NONE
RANGE
VOID
ERROR
```

## Main rules

### Explicit declaration

```text
ধরো বয়স : ফুরালম্বর = ২২
```

The type annotation is mandatory.

### Same-type assignment

Allowed.

### Int to float

Allowed.

```text
ধরো ক : ফুরালম্বর = ৫
ধরো ফল : বাঙ্গালম্বর = ক
```

### Float to int

Not automatically allowed.

### Arithmetic

`+`, `-`, `*`, `%` require numeric operands, except `string + string`, which is supported as concatenation.

### Division

`/` requires numeric operands and its semantic result type is float.

### Equality

Same types may be compared, and int/float combinations may also be compared.

### Ordering

`>`, `<`, `>=`, `<=` are numeric comparisons.

### Logical operations

`এবং`, `অথবা`, and `নায়` operate on boolean values.

### Conditions

If and while conditions must be boolean.

---

# 14. Symbol Table

## Files

- `Symbol.java`
- `SymbolTable.java`

A symbol represents either a variable or function.

A variable symbol stores:

```text
name
kind = VARIABLE
type
```

A function symbol stores:

```text
name
kind = FUNCTION
return type
parameter types
```

## Scope representation

The symbol table uses:

```text
Deque<Map<String, Symbol>>
```

Conceptually:

```text
current local scope
outer scope
...
global scope
```

`declare()` inserts into the current scope and rejects duplicates there.

`resolve()` searches from current scope outward.

`enterScope()` creates a nested scope.

`exitScope()` leaves it.

---

# 15. Functions, Procedures, Forward Calls and Recursion

## Typed function

```text
ফাংশন যোগ(ক : ফুরালম্বর, খ : ফুরালম্বর) -> ফুরালম্বর:
    ফিরিজাও ক + খ
```

Parameters are typed.

A value-returning function declares a return type after `->`.

## Procedure

```text
ফাংশন শুভেচ্ছা(নাম : দড়ি):
    আউটফুট("স্বাগতম " + নাম)
    ফিরিজাও
```

No return annotation means it is treated as a procedure.

## Function checking

The semantic analyzer checks:

- duplicate function name
- parameter declarations
- argument count
- argument types
- return-value compatibility
- return used in correct context

## Forward declarations

The analyzer first records function signatures before analyzing bodies and ordinary statements. This allows calls to functions whose definitions are later in the source.

## Recursion

The same signature-predeclaration design also allows a function to resolve its own name during body analysis.

`samples/recursion.syl` demonstrates recursive factorial.

---

# 16. Control Flow

## If

```text
জুদি বয়স >= ১৮:
    আউটফুট("প্রাপ্তবয়স্ক")
আরজুদি বয়স >= ১৩:
    আউটফুট("কিশোর")
আন্নায়:
    আউটফুট("শিশু")
```

## While

```text
যতবিল কাউন্ট > ০:
    আউটফুট(কাউন্ট)
    কাউন্ট = কাউন্ট - ১
```

## For

```text
লাগিরও সংখ্যা ভিতরে রেঞ্জ(১, ৪):
    আউটফুট(সংখ্যা)
```

The current `for` form is intentionally restricted to `রেঞ্জ(...)`.

## Break

```text
আটকিজাও
```

Legal only inside a loop.

## Continue

```text
ছালাইয়াজাও
```

Legal only inside a loop.

---

# 17. Built-ins and I/O

| SylhetiLang | Generated Python | Semantic behavior |
|---|---|---|
| `আউটফুট` | `print` | console output |
| `ইনফুট` | `input` | returns string |
| `রেঞ্জ` | `range` | 1-3 integer args |
| `সবরহুরু` | `min` | at least 2 numeric args |
| `সবরবড়` | `max` | at least 2 numeric args |
| `গোল` | `round` | 1 or 2 args |

## Console output

```text
আউটফুট("হ্যালো")
```

becomes:

```python
print("হ্যালো")
```

## Console input

```text
ধরো নাম : দড়ি = ইনফুট("নাম: ")
```

becomes a Python `input(...)` call.

The generated target process inherits the terminal's standard input, output, and error streams, so interactive input works in the same terminal.

## File I/O

File creation, file reading, file writing, and file deletion are not language features in the current version.

---

# 18. Code Generation

## File

`src/com/sylhetic/codegen/PythonCodeGenerator.java`

The generator traverses AST nodes and emits Python.

Important mappings:

| SylhetiLang | Python |
|---|---|
| `জুদি` | `if` |
| `আরজুদি` | `elif` |
| `আন্নায়` | `else` |
| `যতবিল` | `while` |
| `লাগিরও` | `for` |
| `ভিতরে` | `in` |
| `ফাংশন` | `def` |
| `ফিরিজাও` | `return` |
| `আটকিজাও` | `break` |
| `ছালাইয়াজাও` | `continue` |
| `এবং` | `and` |
| `অথবা` | `or` |
| `নায়` | `not` |
| `হাছা` | `True` |
| `মিছা` | `False` |
| `কিচ্ছুনায়` | `None` |

Bengali identifiers remain valid Unicode identifiers in the generated Python.

Bengali numeric digits are converted to ASCII digits.

The generator does not copy SylhetiLang type annotations into Python, because those annotations have already served their purpose during static analysis.

---

# 19. Error System

## Files

- `CompilerError.java`
- `ErrorReporter.java`

A compiler error stores:

```text
category
line
column
message
```

The output format is conceptually:

```text
[category] লাইন X, কলাম Y: message
```

The reporter accumulates multiple errors.

## Lexical errors

Examples:

- tab indentation
- invalid indentation width
- unmatched indentation level
- unknown character
- unterminated string
- invalid numeric literal

## Syntax errors

Examples:

- missing `:`
- missing `)`
- missing expression
- malformed declaration

## Semantic/type errors

Examples:

- incompatible assignment
- duplicate declaration
- undefined identifier
- nonboolean condition
- invalid arithmetic types
- wrong function argument count
- wrong function argument type
- invalid return
- break outside loop
- continue outside loop

## No target generation after compiler errors

If any errors exist after analysis, `Main.java` prints all errors, reports the number of errors, and exits without generating/running target code.

---

# 20. Error Recovery

The parser contains a synchronization mechanism.

The purpose is to avoid this behavior:

```text
first syntax error -> entire compiler stops immediately
```

Instead, it attempts:

```text
syntax error -> report -> move to safe boundary -> continue parsing
```

This allows multiple syntax problems to be discovered in a single compile attempt.

`samples/syntax_err.syl` is the included parser-recovery demonstration.

---

# 21. Current Test Programs

## `samples/test.syl`

Demonstrates:

- four user types
- output
- arithmetic
- division
- if/elif/else
- while
- for/range

## `samples/functions.syl`

Demonstrates:

- typed function
- procedure
- function call
- return value
- string concatenation

## `samples/recursion.syl`

Demonstrates:

- recursive factorial
- recursive function resolution
- typed recursive result

## `samples/err_test.syl`

Demonstrates multiple semantic/type errors:

- assigning string to int
- duplicate declaration
- undefined identifier
- nonboolean if condition
- break outside a loop

## `samples/syntax_err.syl`

Demonstrates parser errors and recovery using malformed syntax.

## `amar_codes/main.syl`

A personal working program used to verify normal compile-and-run flow.

---

# 22. Full Simulation: One Program Through the Compiler

Consider:

```text
ধরো বয়স : ফুরালম্বর = ২২
জুদি বয়স >= ১৮:
    আউটফুট("প্রাপ্তবয়স্ক")
```

## Phase 1: Source loading

`Main.java` reads the file using UTF-8.

## Phase 2: Lexer

Important tokens:

```text
DHORO IDENTIFIER COLON TYPE_INT ASSIGN INT NEWLINE
JUDI IDENTIFIER GTE INT COLON NEWLINE
INDENT OUTPUT LPAREN STRING RPAREN NEWLINE
DEDENT EOF
```

## Phase 3: Parser

The parser builds approximately:

```text
Program
├── VarDecl
│   ├── name = বয়স
│   ├── type = ফুরালম্বর
│   └── LiteralExpr(২২)
└── IfStmt
    ├── BinaryExpr(>=)
    │   ├── VariableExpr(বয়স)
    │   └── LiteralExpr(১৮)
    └── ExprStmt
        └── CallExpr(আউটফুট)
```

## Phase 4: Semantic analysis

The analyzer:

1. Checks `বয়স` is not already declared.
2. Converts `ফুরালম্বর` to semantic `INT`.
3. Checks initializer `২২` is `INT`.
4. Stores `বয়স` in the symbol table.
5. Resolves `বয়স` in the condition.
6. Checks `>=` has numeric operands.
7. Determines the comparison result is `BOOL`.
8. Accepts the if condition.
9. Checks the output call.

## Phase 5: Code generation

Target Python becomes conceptually:

```python
বয়স = 22
if (বয়স >= 18):
    print("প্রাপ্তবয়স্ক")
```

## Phase 6: Target execution

`Main.java` starts `python3` with the generated file. The terminal displays:

```text
প্রাপ্তবয়স্ক
```

---

# 23. Compiler Principles Used in This Project

| Compiler-course principle | Where used |
|---|---|
| Source language design | SylhetiLang keywords, types, syntax |
| Formal grammar | `grammar.bnf` |
| Lexical analysis | `Lexer.java` |
| Tokenization | `TokenType.java`, `Token.java` |
| Lookahead | Lexer operators and parser assignment/calls |
| Indentation tokenization | `INDENT`, `DEDENT` |
| Syntax analysis | `Parser.java` |
| Recursive descent | expression and statement parser methods |
| Operator precedence | parser expression hierarchy |
| AST | `Ast.java` |
| Semantic analysis | `SemanticAnalyzer.java` |
| Symbol table | `SymbolTable.java` |
| Static type system | `Type.java`, semantic rules |
| Scope | stack of symbol maps |
| Function signatures | `Symbol.java`, analyzer predeclaration |
| Error detection | lexer/parser/semantic analyzer |
| Error recovery | parser synchronization |
| Target code generation | `PythonCodeGenerator.java` |
| Runtime launch | `Main.java` ProcessBuilder |
| Regression testing | `test.sh`, samples |

---

# 24. Compiler Concepts Not Implemented

The current project intentionally does not implement:

- separate parse tree
- TAC / three-address code
- general IR layer beyond the AST
- optimization passes
- control-flow graph
- data-flow analysis
- assembly generation
- machine-code generation
- register allocation
- linker
- custom runtime/VM
- garbage collector
- classes/OOP
- arrays/lists
- exception handling
- modules
- WebAssembly

If asked about TAC:

> The current compiler uses the AST as its internal structured representation and directly generates Python after semantic analysis. TAC would be a valid future intermediate stage between semantic analysis and code generation, but it is not required by this implementation.

---

# 25. Team Responsibilities

The available project context confirms a five-member team but does not contain explicit member names or already-agreed role assignments. Therefore, the following is a **recommended allocation template**, not a claim about current assignments.

| Member | Recommended primary responsibility | Must still understand |
|---|---|---|
| Member 1 | Token model + Lexer | full pipeline |
| Member 2 | Parser + BNF grammar | full pipeline |
| Member 3 | AST + Semantic Analyzer | full pipeline |
| Member 4 | Symbol Table + Type System + errors | full pipeline |
| Member 5 | Python Code Generator + build/run/testing + docs | full pipeline |

Replace `Member 1` through `Member 5` with actual names after the team agrees.

A practical contribution split can be:

### Member 1: Lexer and tokens

Files:

```text
TokenType.java
Token.java
Lexer.java
```

Topics to defend:

```text
lexeme
token
Unicode
keyword recognition
number scanning
string scanning
INDENT/DEDENT
lexical errors
```

### Member 2: Parser and grammar

Files:

```text
Parser.java
grammar.bnf
```

Topics:

```text
BNF
recursive descent
lookahead
precedence
blocks
syntax errors
recovery
```

### Member 3: AST and semantic traversal

Files:

```text
Ast.java
SemanticAnalyzer.java
```

Topics:

```text
AST nodes
semantic rules
expression type checking
control-context checks
function-body checking
```

### Member 4: Type system, symbols, errors

Files:

```text
Type.java
Symbol.java
SymbolTable.java
CompilerError.java
ErrorReporter.java
```

Topics:

```text
static types
assignability
scopes
identifier resolution
duplicate detection
diagnostics
```

### Member 5: Target generation and integration

Files:

```text
PythonCodeGenerator.java
Main.java
build.sh
sylhetic
test.sh
samples/
README.md
```

Topics:

```text
code generation
source-to-source compilation
build process
CLI
automatic runtime launch
testing
project demonstration
```

Because the course review may ask any member about any feature, role allocation should be treated as contribution ownership, not knowledge isolation.

---

# 26. Environment Context to Remember

Current verified machine environment:

```text
MacBook Air M4
Apple Silicon
uname -m -> arm64
java -> 24.0.1
javac -> 24.0.1
python3 -> 3.14.6
IDE -> VS Code
```

The build uses:

```text
--release 21
```

Therefore the installed JDK can be newer while the project stays Java-21-compatible at compile time.

---

# 27. Important Commands

## Verify environment

```bash
java -version
javac -version
python3 --version
uname -m
```

## Build

```bash
./build.sh
```

## Run normal program

```bash
./sylhetic amar_codes/main.syl
```

## Run core sample

```bash
./sylhetic samples/test.syl
```

## Run function sample

```bash
./sylhetic samples/functions.syl
```

## Run recursion sample

```bash
./sylhetic samples/recursion.syl
```

## Run semantic errors

```bash
./sylhetic samples/err_test.syl
```

## Run syntax errors

```bash
./sylhetic samples/syntax_err.syl
```

## Run complete suite

```bash
./test.sh
```

---

# 28. Common Mistakes During Demonstration

## Mistake: Running `.syl` with Python

Wrong:

```bash
python3 amar_codes/main.syl
```

Correct:

```bash
./sylhetic amar_codes/main.syl
```

Reason: Python cannot parse SylhetiLang syntax.

## Mistake: Running `cd SylhetiLang_Run` while already inside it

Check:

```bash
pwd
```

If the path already ends with `SylhetiLang_Run`, do not `cd SylhetiLang_Run` again.

## Mistake: Tabs

Use exactly spaces for indentation. Tabs are rejected by the lexer.

## Mistake: Missing colon

Control statements and function declarations require `:` before their block.

## Mistake: Trying to use a numeric input directly

`ইনফুট()` currently returns string.

## Mistake: Assuming target `.py` is the compiler

The Java program is the compiler. The `.py` file is generated target code.

---

# 29. Current Language Cheat Sheet

## Declaration

```text
ধরো নাম : দড়ি = "সাব্বির"
ধরো বয়স : ফুরালম্বর = ২২
ধরো দাম : বাঙ্গালম্বর = ১২.৫
ধরো চালু : হাছামিছা = হাছা
```

## Assignment

```text
বয়স = ২৩
```

## Arithmetic

```text
ক + খ
ক - খ
ক * খ
ক / খ
ক % খ
```

## Comparison

```text
==
!=
>
<
>=
<=
```

## Logic

```text
এবং
অথবা
নায়
```

## If

```text
জুদি condition:
    statement
আরজুদি condition:
    statement
আন্নায়:
    statement
```

## While

```text
যতবিল condition:
    statement
```

## For

```text
লাগিরও সংখ্যা ভিতরে রেঞ্জ(১, ৬):
    statement
```

## Function

```text
ফাংশন যোগ(ক : ফুরালম্বর, খ : ফুরালম্বর) -> ফুরালম্বর:
    ফিরিজাও ক + খ
```

## Procedure

```text
ফাংশন দেখাও(বার্তা : দড়ি):
    আউটফুট(বার্তা)
    ফিরিজাও
```

## Break

```text
আটকিজাও
```

## Continue

```text
ছালাইয়াজাও
```

---

# 30. Project Limitations and Future Work

Current limitations can become future-development points:

1. Lists/arrays.
2. Explicit numeric conversion functions for input.
3. User-defined file I/O.
4. Classes and objects.
5. Modules/imports.
6. Better diagnostics with source snippets and carets.
7. More comprehensive automated unit tests.
8. Separate IR or TAC.
9. Optimization passes.
10. Optional WebAssembly target.
11. Debug mode to print tokens and AST.
12. REPL/interpreter mode in addition to compilation.
13. Packaging the launcher globally so `sylhetic file.syl` works outside the repository.
14. Syntax highlighting extension for VS Code.

---

# 31. Important Architectural Answer for Defense

If asked to explain the entire project in one answer:

> SylhetiLang is a Bengali-script statically checked source language whose compiler is implemented in Java. The source file is read in UTF-8. `Lexer.java` scans the Unicode text and generates tokens defined by `TokenType` and `Token`, including INDENT and DEDENT tokens for four-space blocks. `Parser.java` uses recursive descent according to `grammar.bnf`, handles operator precedence, and builds the AST defined in `Ast.java`. `SemanticAnalyzer.java` traverses that AST and uses `SymbolTable`, `Symbol`, and `Type` to check declarations, scope, type compatibility, function calls, returns, conditions, and loop-control rules. Errors are accumulated using `ErrorReporter`. If there are no compiler errors, `PythonCodeGenerator.java` traverses the checked AST and writes executable Python. `Main.java` then starts Python 3 automatically, so a single `./sylhetic file.syl` command compiles and runs the source. The project currently does not use TAC or optimization; it directly generates Python from the semantically checked AST.

---

# 32. What Every Team Member Should Be Able to Explain

Every member should know at least these points even if responsibilities are divided:

1. Why `.syl` cannot be run by `python3` directly.
2. Exact compile/run command.
3. What the lexer receives and produces.
4. Difference between token and lexeme.
5. Why INDENT/DEDENT exist.
6. What the parser receives and produces.
7. What recursive descent means.
8. How precedence is implemented.
9. What an AST is.
10. Difference between syntax error and semantic error.
11. What a symbol table stores.
12. What scope means.
13. Four language types.
14. Int-to-float promotion rule.
15. Why if/while need boolean conditions.
16. How function calls are checked.
17. Why recursion works.
18. What code generator does.
19. Why this is a compiler despite Python executing the result.
20. Why TAC is absent and where it could be added.
21. How errors are collected.
22. What `test.sh` demonstrates.
23. Why UTF-8 matters.
24. Why JDK 24 can build Java-21-compatible code.
25. What happens from pressing Enter on `./sylhetic file.syl` until output appears.

---

# 33. Documentation Accuracy Note

This document describes the current project implementation in the provided `SylhetiLang_Run` codebase. One behavior to remember is that the current `Main.java` automatically executes generated Python after successful compilation. Therefore the actual current behavior is:

```text
./sylhetic file.syl
-> compile
-> generate file.py
-> run file.py automatically
```

If an older README sentence says to run `python3 file.py` manually, treat that as an older instruction; the current source code is the authoritative behavior.

