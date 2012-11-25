package com.sulaco.fringe.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark method parameter as partition key. It will be used, along with a key generator
 * defined in @PartitionInvoke annotation to generate partition key. Partition keys are used to
 * delegate execution to a partition node.
 * 
 * @author gibffe
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface PartitionKey {

	String property() default "";
}
