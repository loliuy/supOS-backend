package com.supos.common.utils;

import com.supos.common.Constants;
import com.supos.common.dto.InstanceField;
import com.supos.common.dto.protocol.OpcUAConfigDTO;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ExpressionUtilsTest {

    @Test
    public void test_abs() {

        Map<String, Object> vars = new HashMap<>();
//        vars.put("x", 5.0);
        vars.put("a1", 1.5d);
//
//        vars.put("a", 5123.1415);
        vars.put("b", Integer.valueOf(-7));
//        vars.put("c", 3);
        eval("ABS(b)", vars);
        eval("ABS(a1)", vars);
    }

    @Test
    public void test_reduceExpressionVar() {
        String expression = "100 * a1 + 10 * a5 + a7";
        ExpressionUtils.ParseResult rs = ExpressionUtils.parseExpression(expression);
        System.out.println("vars = " + rs.vars);
        System.out.println("functions = " + rs.functions);
        InstanceField[] fields = new InstanceField[]{
                new InstanceField("t1", "f1"),
                new InstanceField("t2", "f2"),
                new InstanceField("t3", "f3"),
                new InstanceField("t4", "f4"),
                new InstanceField("t5", "f5"),
                new InstanceField("t6", "f6"),
                new InstanceField("t7", "f7"),
        };
        int countRefs = fields != null ? fields.length : 0;
        HashMap<String, Object> testMap = new HashMap<>();
        HashSet<Integer> indexes = new HashSet<>(Math.max(4, rs.vars.size()));
        for (String var : rs.vars) {
            String errMsg = null;
            if (var != null && var.length() > 1 && var.startsWith(Constants.VAR_PREV)) {
                Integer refIndex = IntegerUtils.parseInt(var.substring(1));
                if (refIndex == null) {
                    errMsg = I18nUtils.getMessage("uns.topic.calc.expression.fields.ref.invalid", var);
                } else if (refIndex > countRefs) {
                    errMsg = I18nUtils.getMessage("uns.topic.calc.expression.fields.ref.indexOutOfBounds", refIndex, countRefs);
                } else {
                    indexes.add(refIndex);
                }
            } else {
                errMsg = I18nUtils.getMessage("uns.topic.calc.expression.fields.ref.invalid", var);
            }
            Assert.assertNull(errMsg);

            testMap.put(var, 1);
        }
        if (rs.vars.size() < countRefs) {
            HashMap<String, String> replaceVar = new HashMap<>(4);
            int countEmpty = 0;
            boolean prevIsEmpty = false;
            for (int i = 1; i <= countRefs; i++) {
                if (!indexes.contains(i)) {
                    fields[i - 1] = null;  // 删除多余的引用
                    countEmpty++;
                    prevIsEmpty = true;
                } else if (prevIsEmpty) {
                    replaceVar.put(Constants.VAR_PREV + i, Constants.VAR_PREV + (i - countEmpty));
                    prevIsEmpty = false;
                }
            }
            String exp = ExpressionUtils.replaceExpression(expression, replaceVar);
            InstanceField[] newFs = new InstanceField[countRefs - countEmpty];
            for (int i = 0, k = 0; i < fields.length; i++) {
                if (fields[i] != null) {
                    newFs[k++] = fields[i];
                }
            }
            System.out.printf("exp = %s\n", exp);
            Assert.assertEquals("100 * a1 + 10 * a2 + a3", exp);
            Assert.assertArrayEquals(new InstanceField[]{
                    new InstanceField("t1", "f1"),
                    new InstanceField("t5", "f5"),
                    new InstanceField("t7", "f7"),
            }, newFs);
        }
    }

    @Test
    public void test_replaceExpression() {
        String expression = "x + ceil(abs(y))";
        HashMap<String, String> var = new HashMap<>(4);
        var.put("x", "\"/dev/x\".x");
        var.put("y", "\"/dev/y\".y");
        String exp = ExpressionUtils.replaceExpression(expression, var);
        System.out.printf("exp='%s'\n", exp);
    }

    @Test
    public void testGson() {
        OpcUAConfigDTO dto = new OpcUAConfigDTO();
        dto.setProtocol("rest");
        dto.setServerName("serv");
        System.out.println(JsonUtil.toJsonUseFields(dto));

        System.out.println(JsonUtil.toJson(dto));
    }

    @Test
    public void testCalcExpression() {
        String expression = "x + ceil(abs(y)) + b.count";
        ExpressionUtils.ParseResult rs = ExpressionUtils.parseExpression(expression);
        System.out.println("vars = " + rs.vars);
        System.out.println("functions = " + rs.functions);
        Assert.assertArrayEquals(new String[]{"b", "x", "y"}, rs.vars.stream().sorted().toArray(n -> new String[n]));
        Assert.assertArrayEquals(new String[]{"abs", "ceil"}, rs.functions.stream().sorted().toArray(n -> new String[n]));


        rs = ExpressionUtils.parseExpression("x > 0 && z >0");
        System.out.println("vars = " + rs.vars);
        System.out.println("functions = " + rs.functions);

        rs = ExpressionUtils.parseExpression("IF(a>b, 0,c)");
        System.out.println("vars = " + rs.vars);
        System.out.println("functions = " + rs.functions);

        rs = ExpressionUtils.parseExpression("NOT(a>b, 0,c)");
        System.out.println("vars = " + rs.vars);
        System.out.println("functions = " + rs.functions);
//        ExprLexer lexer = new ExprLexer(CharStreams.fromString(expression));
//        CommonTokenStream tokens = new CommonTokenStream(lexer);
//        ExprParser parser = new ExprParser(tokens);
//
//        ParseTree tree = parser.expr();
//        EvalVisitor evaluator = new EvalVisitor();
//        int result = evaluator.visit(tree);

//        System.out.println("Result: " + result);
    }

    @Test
    public void testDivideZero() {
        Object expr = ExpressionFunctions.compileExpression("9/0)");
        System.out.println("expr = " + expr + ", " + expr.getClass());
    }

    @Test
    public void testExecuteExpression() {
//        {
//            String expression = "x + NOT)U";
//            Object expr = ExpressionFunctions.compileExpression(expression);
//            System.out.println("expr = "+expr);
//        }
        String expression = "x + CEIL(ABS(y))";
        Object expr = ExpressionFunctions.compileExpression(expression);

        Map<String, Object> vars = new HashMap<>();
        vars.put("x", 5.0);
        vars.put("y", 69.46588);

        vars.put("a", 5123.1415);
        vars.put("b", 7);
        vars.put("c", 3);
        Object result = ExpressionFunctions.executeExpression(expr, vars);
        System.out.println(result);

        eval("ABS(y)", vars);

        eval("MOD(a,c)", vars);
        eval("MIN(a,b,c)", vars);
        eval("MAX(a,b,c)", vars);
        eval("AVERAGE(a,b,c)", vars);

        eval("a> 0 && b<9", vars);
//        eval("AND(a> 0, b<9)", vars);
//        eval("AND(a> 9, b<9)", vars);
        eval("a > 1", vars);
        eval("a > 9", vars);
        eval("SUM(a,b,c)", vars);

        eval("FIXED(a, 2)", vars);
        eval("FIXED(a, -2)", vars);
        eval("FIXED(a, \"1\")", vars);
        eval("FIXED(a, \"r1A\")", vars);

        eval("RANDOM()", vars);
        eval("RANDOM(111,22)", vars);
    }

    private static void eval(String expression, Map<String, Object> vars) {
//        ExpressionUtils.ParseResult rs = ExpressionUtils.parseExpression(expression);
//        System.out.println(rs.functions);
        Object expr = ExpressionFunctions.compileExpression(expression);
        Object result = ExpressionFunctions.executeExpression(expr, vars);
        System.out.println(expression + " = " + result);
    }
}
