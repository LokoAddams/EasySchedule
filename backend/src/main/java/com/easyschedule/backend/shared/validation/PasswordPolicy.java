package com.easyschedule.backend.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PasswordPolicyValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordPolicy {
    String message() default "La contrasenia debe tener minimo 8 caracteres, incluir una mayuscula, una minuscula, un numero y un caracter especial";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
