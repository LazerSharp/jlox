package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static com.craftinginterpreters.lox.TokenType.*;


/*


program        → declaration* EOF ;

declaration    → classDecl
               | funDecl
               | varDecl
               | statement ;

classDecl      → "class" IDENTIFIER "{" function* "}" ;
funDecl        → "fun" function ;
function       → IDENTIFIER inlineFun;
inlineFun      → "(" parameters? ")" block;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
statement      → exprStmt
               | forStmt
               | ifStmt
               | printStmt
               | whileStmt
               | block
               | returnStmt;
returnStmt     → "return" (expression) ";" ;
forStmt        → "for" "(" (varDecl | exprStmt | ";")
                    expression ";"
                    expression ";" ")" statement ;

whileStmt      → "while" "(" expression ")" statement ;
ifStmt         → "if" "(" expression ")" statement ("else" statement)? ;
block          → "{" declaration* "}" ;
exprStmt       → expression ";" ;
printStmt      → print expression ";" ;

expression     → assignment ;
assignment     → IDENTIFIER "=" assignment
                | logic_or;
logic_or       → logic_and (or logic_and)* ;
logic_and      → equality (or equality)* ;
equality       → comparison ( ( "!=" | "==" ) comparison ) * ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term ) * ;
term           → factor ( ( "-" | "+" ) factor ) * ;
factor         → unary ( ( "/" | "*" ) unary ) * ;
unary          → ( "!" | "-" ) unary
               | call ;
call           → primary ( "(" arguments? ")" )* ;
arguments      → expression ( "," expression )* ;
primary        → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")" | inlineFun ;

*/
public class Parser {

    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        try {
            return program();
        } catch (ParseError error) {
            return null;
        }
    }

    List<Stmt> program() {
        List<Stmt> statements = new ArrayList<>();
        while(!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if(match(FUN)) {
                return funDecl("function");
            }
            if (match(VAR)) {
                return varDecl();
            }
            if (match(CLASS)){
                return classDecl();
            }
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt.Class classDecl() {
        Token cls = consume(IDENTIFIER, "Expect class name (identifier)");
        consume(LEFT_BRACE, "Expected left brace");
        List<Stmt.Fun> methods = new ArrayList<>();
        while (peek().type == RIGHT_BRACE) {
            methods.add(funDecl("method"));
        }
        return new Stmt.Class(cls, methods);
    }

    private Stmt.Fun funDecl(String type) {
        Token fun = consume(IDENTIFIER, "Expect " + type + " name (identifier)");
        Expr.InlineFun fe = inlineFun();
        return new Stmt.Fun(fun, fe.parameters, fe.statements);
    }

    private Expr.InlineFun inlineFun() {
        consume(LEFT_PAREN, "Expect '('");
        List<Token> parameters = new ArrayList<>();
        if(check(IDENTIFIER)){
            do {
                parameters.add(consume(IDENTIFIER, "Expected Identifier"));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')'");
        consume(LEFT_BRACE, "Expect '{'");
        Stmt.Block body = block();
        return new Expr.InlineFun(parameters, body.statements);
    }

    private Stmt varDecl() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }


    Stmt statement() {
        if(match(IF)) return ifStmt();
        if(match(PRINT)) return printStmt();
        if(match(WHILE)) return whileStmt();
        if(match(FOR)) return forStmt();
        if(match(LEFT_BRACE)) return block();
        if(match(RETURN)) return returnStmt();
        return expressionStmt();
    }

    private Stmt returnStmt() {
        Token keyword = previous();
        Expr expr = null;
        if(!check(SEMICOLON)) {
            expr = expression();
        }
        consume(SEMICOLON, "return stmt: Semicolon expected ");
        return new Stmt.Return(keyword, expr);
    }

    private Stmt forStmt() {
        consume(LEFT_PAREN, "Expected '(' after for.");
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else {
            if (match(VAR)) {
                initializer = varDecl();
            } else {
                initializer = expressionStmt();
            }
        }

        Expr condition;
        if (match(SEMICOLON)) {
            condition = null;
        } else {
            condition = expression();
            consume(SEMICOLON, "Expected ';' after condition in for loop");
        }

        Expr increment;
        if (match(RIGHT_PAREN)) {
           increment = null;
        } else {
            increment = expression();
            consume(RIGHT_PAREN, "Expected ')' after increment in for loop");
        }
        Stmt body = statement();
        if(increment != null) {
            body = new Stmt.Block(Arrays.asList(
                    body,
                    new Stmt.Expression(increment)));
        }
        if(condition == null) {
            condition = new Expr.Literal(true);
        }

        Stmt loop = new Stmt.While(condition, body);

        if(initializer != null) {
            loop = new Stmt.Block(Arrays.asList(initializer, loop));
        }

        return loop;
    }

    Stmt whileStmt() {
        consume(LEFT_PAREN, "Expected '(' after while.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')' after while expression.");
        Stmt whileStmt = statement();
        return new Stmt.While(condition, whileStmt);
    }

    Stmt ifStmt() {
        consume(LEFT_PAREN, "Expected '(' after if block");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')' after if block expression");
        Stmt ifBranch = statement();
        Stmt elseBranch = null;
        if(match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, ifBranch, elseBranch);
    }

    private Stmt.Block block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expected '}' at the end of a block");
        return new Stmt.Block(statements);
    }

    private Stmt expressionStmt() {
        var expr = expression();
        consume(SEMICOLON, "Expected ';' after value.");
        return new Stmt.Expression(expr);
    }

    private Stmt printStmt() {
        var expr = expression();
        consume(SEMICOLON, "Expected ';' after print statement.");
        return new Stmt.Print(expr);
    }


    Expr expression() {
        return assignment();
    }

    Expr assignment() {
        Expr expr = logicalOr();
        if(match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if(expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr logicalOr() {
        return binarayExpr(this::logicalAnd, OR);
    }

    private Expr logicalAnd() {
        return binarayExpr(this::equality, AND);
    }

    private Expr equality() {
        return binarayExpr(this::comparison, BANG_EQUAL, EQUAL_EQUAL);
    }

    private Expr comparison() {
        return binarayExpr(this::term, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL);
    }

    private Expr term() {
        return binarayExpr(this::factor, MINUS, PLUS);
    }

    private Expr factor() {
        return binarayExpr(this::unary, STAR, SLASH);
    }

    private Expr unary() {
        if(match(MINUS, BANG)) {
            return new Expr.Unary(previous(), unary());
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (match(LEFT_PAREN)){
            expr = finishCall(expr);
        }
        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if(!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expected ')' after function arguments");
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {

        if(match(NIL)) return  new Expr.Literal(null);
        if(match(TRUE)) return  new Expr.Literal(true);
        if(match(FALSE)) return  new Expr.Literal(false);

        if(match(NUMBER, STRING)) {
            Object val = previous().literal;
            return new Expr.Literal(val);
        }
        if(match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if(match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if(match(FUN)) {
            return inlineFun();
        }
        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }


    private Expr binarayExpr(Supplier<Expr> highPrecExpSupplier, TokenType... tokenTypes) {
        Expr expr = highPrecExpSupplier.get();
        while (match(tokenTypes)) {
            Token op = previous();
            Expr right = highPrecExpSupplier.get();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private boolean match(TokenType... tokenTypes) {
        TokenType cTokenType = peek().type;
        for (TokenType type: tokenTypes) {
            if(check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }


    Token peek() {
        return tokens.get(current);
    }

    Token previous() {
        return tokens.get(current-1);
    }
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }



}
