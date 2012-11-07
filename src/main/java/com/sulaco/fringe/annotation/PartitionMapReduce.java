package com.sulaco.fringe.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sulaco.fringe.ngine.mapreduce.CollectionMapper;
import com.sulaco.fringe.ngine.mapreduce.CollectionReducer;
import com.sulaco.fringe.ngine.mapreduce.PartitionMapper;
import com.sulaco.fringe.ngine.mapreduce.PartitionReducer;
import com.sulaco.fringe.ngine.partition.HashcodeKeyGenerator;
import com.sulaco.fringe.ngine.partition.PartitionKeyGenerator;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface PartitionMapReduce {

	Class<? extends PartitionKeyGenerator> keygen() default HashcodeKeyGenerator.class;	
	Class<? extends PartitionMapper>  mapper()  default CollectionMapper.class;
	Class<? extends PartitionReducer> reducer() default CollectionReducer.class;

}
