package com.supos.adapter.mqtt;

import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.sql.visitor.SQLASTVisitor;
import com.supos.common.utils.ExpressionUtils;
import org.junit.Test;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.util.MethodStub;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class CalcTest {

    @Test
    public void testCalcExpression() throws Exception {
        String expression = "(a+c) != 0 and abs(`b`) >100";// "x + ceil(y) + b.count";

//        String expression = "(a+c) != 0";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser("select " + expression + " from __table", null);
        SQLSelectItem selectItem = parser.getExprParser().parseSelectItem();

        SQLASTVisitor visitor = (SQLASTVisitor) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{SQLASTVisitor.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("visit") && args.length > 0) {
                    String op = "";
                    if (args[0] instanceof SQLBinaryOpExpr) {
                        SQLBinaryOpExpr operator = (SQLBinaryOpExpr) args[0];
                        SQLBinaryOperator bop = operator.getOperator();
                        op = bop.name + ", isRelational: " + bop.isRelational() + ", isBoo? " + bop.isLogical();
                    }
                    System.out.printf("visit(%s %s %s)\n", method.getParameterTypes()[0].getSimpleName(), args[0], op);
                }
                if (method.getReturnType() == boolean.class) {
                    return true;
                }
                return null;
            }
        });
        selectItem.getExpr().accept(visitor);


        System.out.println("加括号结果：" + ExpressionUtils.encloseExpressionVars(expression, '`'));
    }

    @Test
    public void testFunc() throws Exception {
//        System.out.println(Math.sin(60));
        HashMap<String, Object> bean = new HashMap<>(4);
        bean.put("count", 19);
        String expression = "x + ceil(y) + c.count";
//        String expression = "  now()";
        Map<String, Object> vars = new HashMap<>();
        vars.put("x", 5.0);
        vars.put("y", 10.3);
        vars.put("id", 1);
        vars.put("c", bean);

        ParserContext context = new ParserContext();
        TreeSet<String> func = new TreeSet<>();
        for (Method m : Math.class.getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                func.add(m.getName());
                context.addImport(m.getName(), m);
            }
        }
        System.out.println("func: " + func);

        context.addImport("now", new MethodStub(getClass(), "now"));
        Serializable compiled = MVEL.compileExpression(expression, context);
        Object result = MVEL.executeExpression(compiled, vars);
        System.out.println("result = " + result);
        System.out.println("result = " + MVEL.eval("2 *  id", vars));

    }

//    @Test
//    public void testSQL() {
//        SQLExprParser parser = new SQLExprParser("select x+abs(y) from t");
//        SQLASTVisitor visitor = (SQLASTVisitor) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{SQLASTVisitor.class}, new InvocationHandler() {
//            @Override
//            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//                if (method.getName().equals("visit")) {
//                    String argType = "";
//                    if (method.getParameterCount() == 1) {
//                        argType = method.getParameterTypes()[0].getSimpleName();
//                    }
//                    System.out.printf("%s(%s): %s\n", method.getName(), argType, Arrays.toString(args));
//                }
//                if (method.getReturnType() == boolean.class) {
//                    return true;
//                }
//                return null;
//            }
//        });
//        parser.parseSelectItem().getExpr().accept(visitor);
//    }
}
