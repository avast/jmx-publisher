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
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JMXPropertySetter {

    public String name() default "";

}