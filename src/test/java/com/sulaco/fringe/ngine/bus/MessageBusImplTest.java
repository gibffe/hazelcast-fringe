package com.sulaco.fringe.ngine.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.partition.Partition;
import com.hazelcast.partition.PartitionService;
import com.sulaco.fringe.annotation.PartitionEvent;
import com.sulaco.fringe.annotation.PartitionEventSubscribe;

@SuppressWarnings("all")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"context.xml"})
public class MessageBusImplTest {

	@Autowired private TestEventDrivenBean bean;
	@Autowired private MessageBusImpl	   bus;
	
	private ExecutorService exe = Executors.newSingleThreadExecutor();

	@Before
	public void setup() {
		bean.reset();
	}
	
	@Test
	public void testBeanPostprocessing() {
		
		assertNotNull(bus.getSubscribers().get("com.sulaco.fringe.ngine.bus.MessageBusImplTest$TestEvent1"));
		assertNotNull(bus.getSubscribers().get("com.sulaco.fringe.ngine.bus.MessageBusImplTest$TestEvent2"));
		
		assertEquals(1, bus.getSubscribers().get("com.sulaco.fringe.ngine.bus.MessageBusImplTest$TestEvent1").size());
		assertEquals(1, bus.getSubscribers().get("com.sulaco.fringe.ngine.bus.MessageBusImplTest$TestEvent2").size());
	}

	@Test
	public void testEmitLocal() {

		// create a nasty hazelcast mock
		Member mockLocalMember = mock(Member.class);
		
		Partition mockPartition = mock(Partition.class);
		when(mockPartition.getOwner()).thenReturn(mockLocalMember);
		
		Cluster mockCluster = mock(Cluster.class);
		when(mockCluster.getLocalMember()).thenReturn(mockLocalMember);
		
		PartitionService mockPartitionService = mock(PartitionService.class);
		when(mockPartitionService.getPartition(any())).thenReturn(mockPartition);
		
		HazelcastInstance mockHazelcast = mock(HazelcastInstance.class);
		when(mockHazelcast.getPartitionService()).thenReturn(mockPartitionService);
		when(mockHazelcast.getCluster()).thenReturn(mockCluster);
		//
		bus.setHazelcast(mockHazelcast);
		
		bus.emit(new TestEvent1());
		bus.emit(new TestEvent1());
		
		bus.emit(new TestEvent2());
		
		waitForAsyncOperation();
		assertEquals(2, bean.type1);
		assertEquals(1, bean.type2);
	}
	
	@Test
	public void testEmitDistributed() {
		
		MessageBusImpl bus = new MessageBusImpl();
		
		// create a nasty hazelcast mock
		Member mockLocalMember = mock(Member.class);
		Member mockRemoteMember = mock(Member.class);
		
		Partition mockPartition = mock(Partition.class);
		when(mockPartition.getOwner()).thenReturn(mockRemoteMember);
		
		Cluster mockCluster = mock(Cluster.class);
		when(mockCluster.getLocalMember()).thenReturn(mockLocalMember);
		
		PartitionService mockPartitionService = mock(PartitionService.class);
		when(mockPartitionService.getPartition(any())).thenReturn(mockPartition);
		
		HazelcastInstance mockHazelcast = mock(HazelcastInstance.class);
		when(mockHazelcast.getPartitionService()).thenReturn(mockPartitionService);
		when(mockHazelcast.getCluster()).thenReturn(mockCluster);
		when(mockHazelcast.getExecutorService()).thenReturn(exe);
		//
		bus.setHazelcast(mockHazelcast);
		
		bus.emit(new TestEvent1());
		bus.emit(new TestEvent1());
		
		bus.emit(new TestEvent2());
		
		waitForAsyncOperation();
		assertEquals(2, bean.type1);
		assertEquals(1, bean.type2);
	}

	@Test
	public void testBroadcast() {

		// TODO: !
	}
	
	public static class TestEventDrivenBean {
		
		public int type1 = 0;
		public int type2 = 0;
		
		@PartitionEventSubscribe
		public void onEvent(TestEvent1 event) {
			type1++;
		}
		
		@PartitionEventSubscribe
		public void onEvent(TestEvent2 event) {
			type2++;
		}
		
		public void reset() {
			type1 = type2 = 0;
		}
	}
	
	@PartitionEvent
	private static class TestEvent1 {}
		
	@PartitionEvent
	private static class TestEvent2 {}
	
	
	private void waitForAsyncOperation() {
		try {
			Thread.sleep(1000);
		} 
		catch (InterruptedException e) {}

		this.exe.submit(
				new Runnable() {
					public void run() {}		
				}
		);
	}
}
