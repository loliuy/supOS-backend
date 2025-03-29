package com.supos.common.utils;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class ExpressionFunctions {


    public static boolean hasFunction(String func) {
        return functions.contains(func);
    }

    public static Serializable compileExpression(String expression) {
        Serializable compiled = MVEL.compileExpression(expression, context);
        return compiled;
    }

    public static Object executeExpression(Object compiledExpression, Map vars) {
        Object result = MVEL.executeExpression(compiledExpression, vars);
        return result;
    }

    private static final ParserContext context = new ParserContext();
    private static final TreeSet<String> functions = new TreeSet<>();

    static {
        HashMap<String, Method> mathMethods = new HashMap<>(64);
        for (Method m : Math.class.getMethods()) {
            int mdf = m.getModifiers();
            if (Modifier.isStatic(mdf) && Modifier.isPublic(mdf)) {
                String name = m.getName();
                if ("abs".equals(name)) {// 三方库bug, 注册 ABS(double) 最终结果是 ABS(int)
                    continue;
                }
                name = name.toUpperCase();
                Method old = mathMethods.get(name);
                if (old != null) {
                    String oldStr = old.toString();
                    String currentStr = m.toString();
                    if (currentStr.compareTo(oldStr) >= 0) {
                        // 让方法按照参数类型  double,float,int,long 这样的顺序自然排列
                        continue;
                    }
                }
                mathMethods.put(name, m);
            }
        }
        for (Map.Entry<String, Method> entry : mathMethods.entrySet()) {
            String name = entry.getKey();
            functions.add(name);
            context.addImport(name, entry.getValue());
        }
        for (Method m : ExpressionFunctions.class.getMethods()) {
            int mdf = m.getModifiers();
            if (Modifier.isStatic(mdf) && Modifier.isPublic(mdf)) {
                String name = m.getName();
                if (name.equals(name.toUpperCase())) {
                    functions.add(name);
                    context.addImport(name.toUpperCase(), m);
                }
            }
        }
    }

    // 数学函数
    public static double ABS(double a) {
        return (a <= 0.0D) ? 0.0D - a : a;
    }

    public static double AVERAGE(double... vs) {
        if (vs == null || vs.length == 0) {
            return 0;
        }
        return SUM(vs) / vs.length;
    }

    public static int COUNT(Object... vs) {
        return vs != null ? vs.length : 0;
    }

    public static double FIXED(double x, int scale) {
        BigDecimal value = new BigDecimal(String.valueOf(x));
        BigDecimal roundedValue = value.setScale(scale, RoundingMode.HALF_UP);
        return roundedValue.doubleValue();
    }

    public static int INT(double x) {
        return (int) x;
    }

    public static double LOG(double x, int d) {
        return Math.log(x) / Math.log(d);
    }

    public static int MOD(int x, int y) {
        return x % y;
    }

    public static long MOD(long x, long y) {
        return x % y;
    }

    public static double MOD(double x, double y) {
        return x % y;
    }

    public static double POWER(double x, double y) {
        return Math.pow(x, y);
    }

    public static Number MAX(Number... vs) {
        if (vs == null || vs.length == 0) {
            return 0;
        }
        Number max = vs[0];
        for (int i = 1; i < vs.length; i++) {
            Number v = vs[i];
            if (v.longValue() > max.longValue()) {
                max = v;
            }
        }
        return max;
    }

    public static Number MIN(Number... vs) {
        if (vs == null || vs.length == 0) {
            return 0;
        }
        Number min = vs[0];
        for (int i = 1; i < vs.length; i++) {
            Number v = vs[i];
            if (v.longValue() < min.longValue()) {
                min = v;
            }
        }

        return min;
    }

    public static double PRODUCT(double... vs) {
        if (vs == null || vs.length == 0) {
            return 0;
        }
        double ji = 1;
        for (double v : vs) {
            ji *= v;
        }
        return ji;
    }

    public static double SUM(double... vs) {
        if (vs == null || vs.length == 0) {
            return 0;
        }
        double sum = 0;
        for (double v : vs) {
            sum += v;
        }
        return sum;
    }

    public static double SUMPRODUCT(double[] values, double[] weights) {
        if (values.length != weights.length) {
            throw new IllegalArgumentException("值和权重数组必须具有相同的长度");
        }
        double sum = 0.0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i] * weights[i];
        }
        return sum;
    }


    // 逻辑函数
    public boolean AND(boolean... vs) {
        if (vs == null || vs.length == 0) {
            return false;
        }
        boolean rs = vs[0];
        for (int i = 1; i < vs.length; i++) {
            rs = rs && vs[i];
            if (!rs) {
                return false;
            }
        }
        return true;
    }


    public boolean OR(boolean... vs) {
        if (vs == null || vs.length == 0) {
            return false;
        }
        boolean rs = vs[0];
        for (int i = 1; i < vs.length; i++) {
            rs = rs || vs[i];
            if (rs) {
                return true;
            }
        }
        return false;
    }

    public boolean TRUE() {
        return true;
    }

    public boolean FALSE() {
        return false;
    }

//    public long IF(boolean v, long a, long b) {
//        return v ? a : b;
//    }
//
//    public double IF(boolean v, double a, double b) {
//        return v ? a : b;
//    }
//
//    public boolean NOT(boolean v) {
//        return !v;
//    }
//
//    public boolean XOR(boolean... vs) {
//        if (vs == null || vs.length == 0) {
//            return false;
//        }
//        boolean rs = vs[0];
//        for (int i = 1; i < vs.length; i++) {
//            rs = rs ^ vs[i];
//        }
//        return false;
//    }
}
