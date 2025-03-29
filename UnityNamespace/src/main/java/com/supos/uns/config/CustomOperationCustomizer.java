package com.supos.uns.config;

import com.alibaba.fastjson2.reader.FieldReader;
import com.supos.common.utils.I18nUtils;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.apache.commons.collections4.CollectionUtils;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomOperationCustomizer implements OperationCustomizer {

    private static final String OPEN_API = "openapi";
    private static final String SUMMARY = "$summary";
    private static final String DESCRIPTION = "$description";
    private static final String PARAMETER = "$parameter";
    private static final String BODY = "$body";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        String methodName = handlerMethod.getMethod().getName();
        // 根据当前语言环境动态加载国际化文案
        String summary = I18nUtils.getMessage(summaryCode(methodName));
        if (!summary.contains("$")){
            operation.setSummary(summary);
        }
        String description = I18nUtils.getMessage(descriptionCode(methodName));
        if (!description.contains("$")){
            operation.setDescription(description);
        }

        if (null != operation.getRequestBody()){
            RequestBody requestBody = operation.getRequestBody();
            String bodyDesc = I18nUtils.getMessage(bodyCode(methodName));
            if (!bodyDesc.contains("$")){
                requestBody.setDescription(bodyDesc);
                operation.setRequestBody(requestBody);
            }
        }

        if (CollectionUtils.isNotEmpty(operation.getParameters())){
            List<Parameter> parameters = operation.getParameters();
            for (Parameter param : parameters) {
                String paramDesc = I18nUtils.getMessage(parameterCode(methodName,param.getName()));
                if (!paramDesc.contains("$")){
                    param.setDescription(paramDesc);
                }
            }
            operation.setParameters(parameters);
        }
        if (CollectionUtils.isNotEmpty(operation.getTags())){
            List<String> tags = operation.getTags().stream().map(I18nUtils::getMessage).collect(Collectors.toList());
            operation.setTags(tags);
        }
        return operation;
    }

    private String summaryCode(String methodName){
        return OPEN_API + "." + methodName + "." + SUMMARY;
    }

    private String descriptionCode(String methodName){
        return OPEN_API + "." + methodName + "." + DESCRIPTION;
    }

    private String parameterCode(String methodName,String paramName){
        return OPEN_API + "." + methodName + "." + PARAMETER + "." + paramName;
    }

    private String bodyCode(String methodName){
        return OPEN_API + "." + methodName + "." + BODY;
    }
}
