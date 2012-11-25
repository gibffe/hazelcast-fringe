package com.sulaco.fringe.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sulaco.fringe.ngine.partition.HashcodeKeyGenerator;
import com.sulaco.fringe.ngine.partition.PartitionKeyGenerator;

/**
 * Marks a class as event. Supplied keygen will produce partition key. Events will be routed 
 * to a partition and registered subscribers will receive it asynchronously.
 * 
 * @author gibffe
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface PartitionEvent {

	Class<? extends PartitionKeyGenerator> keygen() default HashcodeKeyGenerator.class;	
	
}
