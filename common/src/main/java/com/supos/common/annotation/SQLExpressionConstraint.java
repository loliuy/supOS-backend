package com.supos.common.annotation;

import cn.hutool.core.collection.CollectionUtil;
import com.supos.common.utils.ExpressionUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static com.supos.common.utils.ConstraintErrTipUtils.setErrMsg;

public class SQLExpressionConstraint implements ConstraintValidator<SQLExpressionValidator, String> {
    private SQLExpressionValidator expressionValidator;

    public void initialize(SQLExpressionValidator constraintAnnotation) {
        expressionValidator = constraintAnnotation;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.length() == 0) {
            return true;
        }
        boolean ok = false;
        try {
            ExpressionUtils.ParseResult rs = ExpressionUtils.parseExpression(value);
            if (rs.isBooleanResult) {
                if (expressionValidator.isHaving()) {// having 条件， 只能含有聚合函数
                    if (!rs.functions.isEmpty() && CollectionUtil.isEqualList(rs.functions, rs.aggregateFunctions)) {
                        ok = true;
                    }
                } else {// where 条件，不能含有聚合函数
                    if (rs.aggregateFunctions.isEmpty()) {
                        ok = true;
                    }
                }
            } else {
                setErrMsg(context, expressionValidator.field(), "uns.invalid.stream.NotBoolCondition", value);
                return false;
            }
        } catch (Exception ex) {
        }
        if (!ok) {
            setErrMsg(context, expressionValidator.field(), "uns.invalid.stream.invalidCondition", value);
        }
        return ok;
    }
}