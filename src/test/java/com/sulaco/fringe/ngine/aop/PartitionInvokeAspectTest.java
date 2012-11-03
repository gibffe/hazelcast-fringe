package com.sulaco.fringe.ngine.aop;


import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.JoinPoint.StaticPart;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sulaco.fringe.TestService;
import com.sulaco.fringe.annotation.PartitionInvoke;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("all")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"aop-auto.xml"})
public class PartitionInvokeAspectTest {

	@Autowired TestService service;
	@Autowired PartitionInvokeAspect aspect;
	
	@Test
	public void test_getAccount1() throws Throwable {
		
		Method method = service.getClass().getMethod("getAccount1", Integer.class);
		
		// get declared annotation
		PartitionInvoke pi =  method.getAnnotation(PartitionInvoke.class);
		
		// & create a nasty ProceedingJoinPoint mock 
		MethodSignature mockSignature = mock(MethodSignature.class);
		when(mockSignature.getMethod()).thenReturn(method);
		when(mockSignature.getName()).thenReturn(method.getName());
		when(mockSignature.getDeclaringTypeName()).thenReturn(method.getDeclaringClass().getName());
		when(mockSignature.getParameterTypes()).thenReturn(method.getParameterTypes());
		
		StaticPart mockStaticPart = mock(StaticPart.class);
		when(mockStaticPart.getSignature()).thenReturn(mockSignature);
		
		ProceedingJoinPoint mockPjp = mock(ProceedingJoinPoint.class);
		when(mockPjp.getStaticPart()).thenReturn(mockStaticPart);
		when(mockPjp.getArgs()).thenReturn(new Object[]{new Integer(1)});

		String result = (String) aspect.partitionInvocation(mockPjp, pi);
		assertEquals("account1", result);
	}
	

	
	public static void main(String[] args) {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("/com/sulaco/fringe/ngine/aop/aop-auto.xml");

		TestService service = (TestService) ctx.getBean("testService");
		service.getAccount1(1);

	}

}
