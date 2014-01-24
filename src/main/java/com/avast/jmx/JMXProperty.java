/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avast.jmx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Tomas Rehak <rehak@avast.com>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JMXProperty {

    public String name() default "";

    public boolean readeable() default true;

    public boolean setable() default false;

    public String description() default "";
}
