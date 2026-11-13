package com.mov.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SenhaForteValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaForte {
    String message() default "A senha deve ter no mínimo 12 caracteres, incluir maiúsculas, minúsculas, números e símbolos, e não pode ter sido exposta em vazamentos de dados (HIBP).";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
