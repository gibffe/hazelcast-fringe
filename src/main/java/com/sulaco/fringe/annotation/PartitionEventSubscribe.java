package com.sulaco.fringe.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event handler. The method should have only 1 parameter of the event type to
 * subscribe to. This method will be called asynchronously upon event arrival at local member partition.
 * Engine will scan code and subscribe objects for events automatically; 
 * 
 * @author gibffe
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface PartitionEventSubscribe {

}
