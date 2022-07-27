package com.dust.annotion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.RetentionPolicy.*;

@Target(ElementType.TYPE)
@Retention(RUNTIME)
public @interface ServiceScan {
    public String value() default "";
}

