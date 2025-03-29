package com.supos.uns.config;

import com.supos.common.dto.BaseResult;
import com.supos.uns.exception.BussinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
@Order(1)
public class RestApiExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public BaseResult onMissingParamException(MissingServletRequestParameterException ex) {
        return new BaseResult(400, ex.getMessage());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler({BussinessException.class})
    @ResponseStatus(HttpStatus.OK)
    public BaseResult handleServiceException(BussinessException e) {
        log.error(e.getMessage());
        return new BaseResult(500, e.getMessage());
    }
}
