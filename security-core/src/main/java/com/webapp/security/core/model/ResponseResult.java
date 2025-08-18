package com.webapp.security.core.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 统一API响应结果封装
 * 
 * @param <T> 数据类型
 */
@Getter
@Setter
public class ResponseResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private String code = "success";

    /**
     * 消息
     */
    private String message = "操作成功";

    /**
     * 数据
     */
    private T data;

    public ResponseResult() {
    }

    public ResponseResult(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ResponseResult(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public ResponseResult(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public ResponseResult(T data) {
        this.data = data;
    }

    /**
     * 成功返回结果
     */
    public static <T> ResponseResult<T> success() {
        return new ResponseResult<>();
    }

    /**
     * 成功返回结果
     * 
     * @param data 获取的数据
     */
    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(data);
    }

    /**
     * 成功返回结果
     * 
     * @param data    获取的数据
     * @param message 提示信息
     */
    public static <T> ResponseResult<T> success(T data, String message) {
        return new ResponseResult<>(message, data);
    }

    /**
     * 失败返回结果
     */
    public static <T> ResponseResult<T> failed() {
        return new ResponseResult<>(ErrorCode.INTERNAL, "操作失败");
    }

    /**
     * 失败返回结果
     *
     * @param code    状态码
     * @param message 提示信息
     */
    public static <T> ResponseResult<T> failed(String code, String message) {
        return new ResponseResult<>(code, message);
    }

    /**
     * 失败返回结果
     * 
     * @param message 提示信息
     */
    public static <T> ResponseResult<T> failed(String message) {
        return new ResponseResult<>(ErrorCode.INTERNAL, message);
    }
}