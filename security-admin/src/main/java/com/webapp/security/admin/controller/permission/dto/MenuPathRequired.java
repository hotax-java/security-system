package com.webapp.security.admin.controller.permission.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 自定义校验注解，校验权限类型为菜单时，路径是否必填
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = { MenuPathRequiredValidator.class })
public @interface MenuPathRequired {
    String message() default "菜单类型的权限必须设置路径";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}