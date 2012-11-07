package com.sulaco.fringe.ngine.aop;


import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.JoinPoint.StaticPart;
import org.aspectj.lang.annotation.Aspect;
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

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.partition.Partition;
import com.hazelcast.partition.PartitionService;
import com.sulaco.fringe.TestService;
import com.sulaco.fringe.annotation.PartitionInvoke;
import com.sulaco.fringe.ngine.aop.PartitionInvokeAspect.PartitionKeyTrace;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("all")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"aop-auto.xml"})
public class PartitionInvokeAspectTest {

	@Autowired TestService service;
	@Autowired PartitionInvokeAspect aspect;
	
	@Test
	public void testLocalInvocation() throws Throwable {
		
		Method method = service.getClass().getMethod("getAccount1", Integer.class);
		
		// get declared annotation
		PartitionInvoke pi =  method.getAnnotation(PartitionInvoke.class);
		
		// & create a nasty ProceedingJoinPoint mock 
		ProceedingJoinPoint mockJoinPoint = mockJoinPoint(method, new Object[]{new Integer(1)});
		
		// & create a nasty Hazelcast mock simulating local partition invocation
		Member local = mock(Member.class);
		HazelcastInstance mockHazelcast = mockHazelcast(local, local);
		
		
		aspect.setHazelcast(mockHazelcast);
		
		String result = (String) aspect.partitionInvocation(mockJoinPoint, pi);
		verify(mockJoinPoint).proceed();
	}
	
	@Test
	public void testRemoteInvocation() throws Throwable {
		
		Method method = service.getClass().getMethod("getAccount1", Integer.class);
		
		// get declared annotation
		PartitionInvoke pi =  method.getAnnotation(PartitionInvoke.class);
		
		// & create a nasty ProceedingJoinPoint mock 
		ProceedingJoinPoint mockJoinPoint = mockJoinPoint(method, new Object[]{new Integer(1)});
		
		// & create a nasty Hazelcast mock simulating local partition invocation
		Member local  = mock(Member.class);
		Member remote = mock(Member.class);
		HazelcastInstance mockHazelcast = mockHazelcast(local, remote);
		
		
		aspect.setHazelcast(mockHazelcast);
		
		String result = (String) aspect.partitionInvocation(mockJoinPoint, pi);
		verify(mockHazelcast).getExecutorService();
	}
	
	@Test
	public void testGetPartitionKeyTrace() throws Throwable {
		
		Method method = service.getClass().getMethod("getAccount2", Integer.class, Integer.class, Integer.class);
		MethodSignature mockSignature = mockMethodSignature(method);
		
		PartitionKeyTrace trace = aspect.getPartitionKeyTrace(mockSignature);
		assertNotNull(trace);
		assertNotNull(trace.getPk());
		assertEquals(1, trace.getPidx());
		
		
		method = service.getClass().getMethod("getAccount1", Integer.class);
		mockSignature = mockMethodSignature(method);
		
		trace = aspect.getPartitionKeyTrace(mockSignature);
		assertNotNull(trace);
		assertNotNull(trace.getPk());
		assertEquals(0, trace.getPidx());
	}
	
	
	private HazelcastInstance mockHazelcast(Member local, Member partitionOwner) {
		Cluster mockCluster = mock(Cluster.class);
		when(mockCluster.getLocalMember()).thenReturn(local);
		
		Partition mockPartition = mock(Partition.class);
		when(mockPartition.getOwner()).thenReturn(partitionOwner);
		
		PartitionService mockPartitionService = mock(PartitionService.class);
		when(mockPartitionService.getPartition(anyObject())).thenReturn(mockPartition);

		ExecutorService mockExe = mock(ExecutorService.class);
		when(mockExe.submit(any(Callable.class))).thenReturn(mock(Future.class));
		
		HazelcastInstance mockHazelcast = mock(HazelcastInstance.class);
		when(mockHazelcast.getCluster()).thenReturn(mockCluster);
		when(mockHazelcast.getPartitionService()).thenReturn(mockPartitionService);
		when(mockHazelcast.getExecutorService()).thenReturn(mockExe);
		
		return mockHazelcast;
	}
	
	private ProceedingJoinPoint mockJoinPoint(Method method, Object[] args) throws Throwable {
		MethodSignature mockSignature = mockMethodSignature(method);
				
		StaticPart mockStaticPart = mock(StaticPart.class);
		when(mockStaticPart.getSignature()).thenReturn(mockSignature);
		
		ProceedingJoinPoint mockPjp = mock(ProceedingJoinPoint.class);
		when(mockPjp.getStaticPart()).thenReturn(mockStaticPart);
		when(mockPjp.getArgs()).thenReturn(args);
		
		return mockPjp;
	}
	
	private MethodSignature mockMethodSignature(Method method) {
		MethodSignature mockSignature = mock(MethodSignature.class);
		
		when(mockSignature.getMethod()).thenReturn(method);
		when(mockSignature.getName()).thenReturn(method.getName());
		when(mockSignature.getDeclaringTypeName()).thenReturn(method.getDeclaringClass().getName());
		when(mockSignature.getParameterTypes()).thenReturn(method.getParameterTypes());
		
		return mockSignature;
	}
	
	public static void main(String[] args) {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("/com/sulaco/fringe/ngine/aop/aop-auto.xml");

		TestService service = (TestService) ctx.getBean("testService");
		service.getAccount1(1);

	}

}
