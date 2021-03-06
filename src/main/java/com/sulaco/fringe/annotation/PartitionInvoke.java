package com.sulaco.fringe.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sulaco.fringe.ngine.partition.HashcodeKeyGenerator;
import com.sulaco.fringe.ngine.partition.PartitionKeyGenerator;

/**
 * Marks a method as capable of partition-aware invocation. This in conjuction with PartionKey annotated
 * parameter will be used by the underlying engine to route execution to proper partition node.
 * 
 * Keygenerator can be specified that will generate a partition key from @PartitionKey argument.
 * 
 * @author gibffe
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface PartitionInvoke {

	Class<? extends PartitionKeyGenerator> keygen() default HashcodeKeyGenerator.class;	
	
	boolean blocking() default true;

}
