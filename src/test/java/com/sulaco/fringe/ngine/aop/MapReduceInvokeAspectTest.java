package com.sulaco.fringe.ngine.aop;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.JoinPoint.StaticPart;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.partition.Partition;
import com.hazelcast.partition.PartitionService;
import com.sulaco.fringe.TestService;
import com.sulaco.fringe.TestServiceImpl;
import com.sulaco.fringe.annotation.PartitionMapReduce;
import com.sulaco.fringe.ngine.FringeEvent;

@SuppressWarnings("all")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"aop-auto.xml"})
public class MapReduceInvokeAspectTest {

	@Autowired TestService service;
	@Autowired MapReduceInvokeAspect aspect;
	
	@Test
	public void testInvocation() throws Throwable {
		
		final int N = 3; // partition count
		
		Method method = Class.forName("com.sulaco.fringe.TestServiceImpl").getMethod("processCollection", Collection.class);
		
		// get declared annotation
		PartitionMapReduce pmr = method.getAnnotation(PartitionMapReduce.class);
		
		// & create a nasty ProceedingJoinPoint mock 
		Collection<Integer> col = new ArrayList<Integer>();
		for (int i = 0; i < 10; i++) {
			col.add(i);
		}
		ProceedingJoinPoint mockJoinPoint = mockJoinPoint(method, new Object[]{col});
		
		// mock hazelcast
		HazelcastInstance mockHazelcast = mock(HazelcastInstance.class);
		Cluster mockCluster = mock(Cluster.class);
		final Member mockLocalMember = mock(Member.class);
		PartitionService mockPartitionService = mock(PartitionService.class);
		doAnswer(
				new Answer<Object>() {

					private Map<Integer, Partition> partitions = new HashMap<Integer, Partition>();
					
					public Object answer(InvocationOnMock invocation) throws Throwable {
						
						Integer input = (Integer) invocation.getArguments()[0];
						Integer partitionId = input % N;
						
						if (!partitions.containsKey(partitionId)) {
							Partition mockPartition = mock(Partition.class);
							when(mockPartition.getPartitionId()).thenReturn(partitionId);
							if (partitionId == 0) { // local
								when(mockPartition.getOwner()).thenReturn(mockLocalMember);
							}
							else {
								when(mockPartition.getOwner()).thenReturn(mock(Member.class));
							}
							partitions.put(partitionId, mockPartition);
						}
						
						return partitions.get(partitionId);
					}
				}
		).when(mockPartitionService).getPartition(anyInt());
		
		when(mockCluster.getLocalMember()).thenReturn(mockLocalMember);
		when(mockHazelcast.getCluster()).thenReturn(mockCluster);
		when(mockHazelcast.getPartitionService()).thenReturn(mockPartitionService);
		ExecutorService mockExecutor = mock(ExecutorService.class);
		doAnswer(
				new Answer<Object>() {
					public Object answer(InvocationOnMock invocation) throws Throwable {
						FringeEvent fevent = (FringeEvent) invocation.getArguments()[0];
						
						Future mockFuture = mock(Future.class);
						when(mockFuture.get()).thenReturn(fevent.getParamValues()[0]);
						
						return mockFuture;
					}
				}
		).when(mockExecutor).submit(any(Callable.class));
		
		when(mockHazelcast.getExecutorService()).thenReturn(mockExecutor);
		aspect.setHazelcast(mockHazelcast);
		
		Collection result = (Collection) aspect.mapreduceInvocation(mockJoinPoint, pmr);
		assertNotNull(result);
		assertEquals(10, result.size());
	}

	private ProceedingJoinPoint mockJoinPoint(final Method method, final Object[] args) throws Throwable {
		MethodSignature mockSignature = mockMethodSignature(method);
				
		StaticPart mockStaticPart = mock(StaticPart.class);
		when(mockStaticPart.getSignature()).thenReturn(mockSignature);
		
		ProceedingJoinPoint mockPjp = mock(ProceedingJoinPoint.class);
		when(mockPjp.getStaticPart()).thenReturn(mockStaticPart);
		when(mockPjp.getArgs()).thenReturn(args);
		when(mockPjp.getTarget()).thenReturn(new TestServiceImpl());
		// mock proceed(args) so it returns slice supplied
		doAnswer(
				new Answer<Object>() {
					public Object answer(InvocationOnMock invocation) throws Throwable {
						return ((Object[])invocation.getArguments()[0])[0];
					}
				}
		).when(mockPjp).proceed(any(Object[].class));
		
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
}
