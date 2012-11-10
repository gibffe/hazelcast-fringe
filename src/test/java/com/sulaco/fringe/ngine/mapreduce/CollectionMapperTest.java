package com.sulaco.fringe.ngine.mapreduce;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.hazelcast.partition.Partition;
import com.hazelcast.partition.PartitionService;
import com.sulaco.fringe.ngine.partition.HashcodeKeyGenerator;

@SuppressWarnings("rawtypes")
public class CollectionMapperTest {

	private static final Integer N = 4; // number of partitions
	
	@Test
	public void testInputSplit() {
		
		// mock partition service
		PartitionService mockPartitionService = mockPartitionService();
				
		CollectionMapper mapper = new CollectionMapper();
		mapper.setKeygen(new HashcodeKeyGenerator());
		mapper.setPartitionService(mockPartitionService);
		
		Collection<Integer> input = new ArrayList<Integer>();
		for (int i = 0; i < 4; i++) {
			input.add(i);
		}
		
		Map<Integer, Collection> map = mapper.map(input);
		assertNotNull(map);
		assertEquals(4, map.keySet().size());
	}
	
	@Test
	public void testEmptyInputSplit() {
		
		// mock partition service
		PartitionService mockPartitionService = mockPartitionService();
		
		CollectionMapper mapper = new CollectionMapper();
		mapper.setKeygen(new HashcodeKeyGenerator());
		mapper.setPartitionService(mockPartitionService);
		
		Map<Integer, Collection> map = mapper.map(Collections.emptyList());
		assertNotNull(map);
		assertTrue(map.keySet().isEmpty());
	}
	
	@Test
	public void testNullInputSplit() {
		
		// mock partition service
		PartitionService mockPartitionService = mockPartitionService();
		
		CollectionMapper mapper = new CollectionMapper();
		mapper.setKeygen(new HashcodeKeyGenerator());
		mapper.setPartitionService(mockPartitionService);
		
		Map<Integer, Collection> map = mapper.map(null);
		assertNotNull(map);
		assertTrue(map.keySet().isEmpty());
	}

	private PartitionService mockPartitionService() {
		
		PartitionService mockPartitionService = mock(PartitionService.class);
		
		doAnswer(
				new Answer<Partition>() {

					@Override
					public Partition answer(InvocationOnMock invocation) throws Throwable {
						
						Integer key = (Integer) invocation.getArguments()[0];
						
						Partition mockPartition = mock(Partition.class);
						when(mockPartition.getPartitionId()).thenReturn(key % N);
						
						return mockPartition;
					}
				}
		).when(mockPartitionService).getPartition(anyInt());
		
		return mockPartitionService;
	}
}
