package com.webapp.security.admin.controller.permission.dto;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 菜单路径必填验证器
 */
public class MenuPathRequiredValidator implements ConstraintValidator<MenuPathRequired, Object> {

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }

        try {
            // 使用反射获取permType和permPath字段
            java.lang.reflect.Field permTypeField = obj.getClass().getDeclaredField("permType");
            java.lang.reflect.Field permPathField = obj.getClass().getDeclaredField("permPath");

            permTypeField.setAccessible(true);
            permPathField.setAccessible(true);

            Integer permType = (Integer) permTypeField.get(obj);
            String permPath = (String) permPathField.get(obj);

            // 如果是菜单类型（permType = 1），则路径必填
            if (permType != null && permType == 1) {
                return permPath != null && !permPath.trim().isEmpty();
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}