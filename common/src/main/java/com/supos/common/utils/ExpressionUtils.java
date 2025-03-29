package com.supos.common.utils;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.sql.visitor.SQLASTVisitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;

public class ExpressionUtils {

    public static class ParseResult {
        public final Collection<String> vars;
        public final Collection<String> functions;

        public final Collection<String> aggregateFunctions;

        public final boolean isBooleanResult;

        ParseResult(Collection<String> vars, Collection<String> functions, Collection<String> aggregateFunctions, boolean isBooleanResult) {
            this.vars = vars;
            this.functions = functions;
            this.aggregateFunctions = aggregateFunctions;
            this.isBooleanResult = isBooleanResult;
        }
    }

    public static ParseResult parseExpression(String expression) {
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser("select " + expression + " from `__table`", DbType.mysql);
        SQLSelectItem selectItem = parser.getExprParser().parseSelectItem();
        Collection<String> vars = new HashSet<>();
        Collection<String> functions = new HashSet<>();
        Collection<String> aggregateFunctions = new HashSet<>();
        SQLBinaryOpExpr[] binaryOpExprs = new SQLBinaryOpExpr[1];
        selectItem.getExpr().accept(new SQLASTVisitor() {
            @Override
            public boolean visit(SQLIdentifierExpr x) {
                String name = x.getName();
                if (!(x.getParent() instanceof SQLExprTableSource)) {
                    vars.add(name);
                }
                return true;
            }

            @Override
            public boolean visit(SQLMethodInvokeExpr x) {
                functions.add(x.getMethodName());
                return true;
            }

            public boolean visit(SQLAggregateExpr x) {
                functions.add(x.getMethodName());
                aggregateFunctions.add(x.getMethodName());
                return true;
            }

            public boolean visit(SQLBinaryOpExpr x) {
                if (binaryOpExprs[0] == null) {
                    binaryOpExprs[0] = x;
                }
                return true;
            }
        });
        boolean isBooleanResult = false;
        if (binaryOpExprs[0] != null) {
            SQLBinaryOperator operator = binaryOpExprs[0].getOperator();
            isBooleanResult = operator.isRelational() || operator.isLogical();
        }
        return new ParseResult(vars, functions, aggregateFunctions, isBooleanResult);
    }

    public static String replaceExpression(String expression, Map<String, String> varReplacer) {
        return replaceExpression(expression, varReplacer::get);
    }

    public static String encloseExpressionVars(String expression, char escape) {
        return replaceExpression(expression, name -> name.charAt(0) != escape ? escape + name + escape : name);
    }

    public static String replaceExpression(String expression, Function<String, String> varReplacer) {
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser("select " + expression + " from t", DbType.mysql);
        SQLSelectItem selectItem = parser.getExprParser().parseSelectItem();
        selectItem.getExpr().accept(new SQLASTVisitor() {
            @Override
            public boolean visit(SQLIdentifierExpr x) {
                String name = x.getName();
                if (!(x.getParent() instanceof SQLExprTableSource)) {
                    String rep = varReplacer.apply(name);
                    if (rep != null) {
                        x.setName(rep);
                    }
                }
                return true;
            }
        });
        String exp = selectItem.getExpr().getChildren().get(0).toString();
        int b = exp.indexOf(' ');
        int lax = exp.lastIndexOf(' ', exp.length() - 2);
        return exp.substring(b + 1, lax - 4).trim();
    }


}
