package edu.vu.isis.ammo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation makes it possible to specify where software patterns are
 * used.
 * 
 * 
 * Used to determine the effectiveness of design pattern implementation.
 * 
 * <p>
 * For example:
 * <p>
 * 
 * <code>
 * DesignPattern.Specification ( instanceName = "foo", 
 *    namespace = "posa2", 
 *    patternName = "proactor")
 * </code>
 * 
 * <p>
 * The fully qualified name for the design pattern instance is (namespace, patternName, instanceName).
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR,
		ElementType.ANNOTATION_TYPE, ElementType.PACKAGE, ElementType.FIELD,
		ElementType.LOCAL_VARIABLE })
@Documented
public @interface DesignPattern {

    public @interface Specification {
        /** the pattern instance name */
        String instanceName();

        /**
         * the name of the defining source
         * <dl>
         * <dt>gof</dt>
         * <dd>"Design Patterns", Gamma et al</dd>
         * <dt>posa2</dt>
         * <dd>"Pattern-Oriented Software Architecture, V2", Schmidt et al</dd>
         * </dl>
         */
        String namespace();

        /** the primary name */
        String patternName();

        /** the context specific name */
        String alias();
        
        /** the pattern implementation model */
        String impl() default "";
    }
    
    public @interface Role {
        /** the context specific name */
        String alias();
        
        /** the pattern specific role played by objects of this class */
        String role();
        
        /** documentation about the details of this implementation */
        String detail() default "";
        
        /** how is the detail encoded */
        String detailType() default "text/html";
    }
    
}
