package com.sulaco.fringe.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sulaco.fringe.ngine.partition.HashcodeKeyGenerator;
import com.sulaco.fringe.ngine.partition.PartitionKeyGenerator;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface PartitionInvoke {

	Class<? extends PartitionKeyGenerator> keygen() default HashcodeKeyGenerator.class;	
	
	boolean blocking() default true;

}
