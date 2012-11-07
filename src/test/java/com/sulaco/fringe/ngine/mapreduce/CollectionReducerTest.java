package com.sulaco.fringe.ngine.mapreduce;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

@SuppressWarnings({"unused","unchecked"})
public class CollectionReducerTest {

	@Test
	public void testReduce() {
	
		CollectionReducer reducer = new CollectionReducer();
		
		for (int i = 0; i < 4; i++) {
			reducer.reduce(i, Arrays.asList(i));
		}
		
		Collection<Integer> result = (Collection<Integer>) reducer.collate();
		assertNotNull(result);
		assertEquals(4, result.size());
	}

}
