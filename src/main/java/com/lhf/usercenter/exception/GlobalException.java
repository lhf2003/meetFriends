package com.lhf.usercenter.exception;

import com.lhf.usercenter.common.BaseResponse;
import com.lhf.usercenter.common.ErrorCode;
import com.lhf.usercenter.common.utils.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @description: 全局异常处理
 * @author: liuhf
 */
@RestControllerAdvice
@Slf4j
public class GlobalException {
    @ExceptionHandler(BusinessException.class)
    public BaseResponse businessExcption(BusinessException e) {
        log.error("BusinessException：{}", e.getMessage());
        return ResultUtil.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse runtimeExcption(RuntimeException e) {
        log.error("RuntimeException：{}", e.getMessage());
        return ResultUtil.error(ErrorCode.ERROR, e.getMessage(), "");
    }
}