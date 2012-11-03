package com.sulaco.fringe.ngine.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import com.sulaco.fringe.annotation.PartitionKey;
import com.sulaco.fringe.annotation.PartitionMapReduce;

@Aspect
public class MapReduceInvokeAspect {


	@Around("@annotation(pmr) && @args(pk,..)")
	public void mapreduceInvocation(ProceedingJoinPoint pjp, PartitionMapReduce pmr, PartitionKey pk) {
		
	}
}
