package com.webapp.security.core.exception;

import com.webapp.security.core.model.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class BizException extends BaseException {

    private static final long serialVersionUID = 1L;

    public BizException(String code, String message) {
        super(String.format(ErrorCode.BIZ, code), message);
    }
}