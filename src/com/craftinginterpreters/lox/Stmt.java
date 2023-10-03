package com.craftinginterpreters.lox;

import java.util.List;

abstract class Stmt {
  interface Visitor<R> {
    R visitIfStmt(If stmt);
    R visitWhileStmt(While stmt);
    R visitBlockStmt(Block stmt);
    R visitExpressionStmt(Expression stmt);
    R visitPrintStmt(Print stmt);
    R visitVarStmt(Var stmt);
    R visitFunStmt(Fun stmt);
    R visitReturnStmt(Return stmt);
  }
  static class If extends Stmt {
    If(Expr condition, Stmt ifBranch, Stmt elseBranch) {
      this.condition = condition;
      this.ifBranch = ifBranch;
      this.elseBranch = elseBranch;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }

    final Expr condition;
    final Stmt ifBranch;
    final Stmt elseBranch;
  }
  static class While extends Stmt {
    While(Expr condition, Stmt whileStmt) {
      this.condition = condition;
      this.whileStmt = whileStmt;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

    final Expr condition;
    final Stmt whileStmt;
  }
  static class Block extends Stmt {
    Block(List<Stmt> statements) {
      this.statements = statements;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }

    final List<Stmt> statements;
  }
  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    final Expr expression;
  }
  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }

    final Expr expression;
  }
  static class Var extends Stmt {
    Var(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }

    final Token name;
    final Expr initializer;
  }
  static class Fun extends Stmt {
    Fun(Token name, List<Token> parameters, List<Stmt> block) {
      this.name = name;
      this.parameters = parameters;
      this.block = block;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunStmt(this);
    }

    final Token name;
    final List<Token> parameters;
    final List<Stmt> block;
  }
  static class Return extends Stmt {
    Return(Token keyword, Expr expression) {
      this.keyword = keyword;
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }

    final Token keyword;
    final Expr expression;
  }

  abstract <R> R accept(Visitor<R> visitor);
}
