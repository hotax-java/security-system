package com.webapp.security.admin.controller.permission.dto;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * 菜单路径必填验证注解
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MenuPathRequiredValidator.class)
@Documented
public @interface MenuPathRequired {
    String message() default "菜单类型的权限必须填写路径";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}