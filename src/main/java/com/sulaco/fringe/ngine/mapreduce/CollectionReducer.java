package com.sulaco.fringe.ngine.mapreduce;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("rawtypes")
public class CollectionReducer implements PartitionReducer {

	protected ConcurrentMap<Integer, Collection> results = new ConcurrentHashMap<>();
	
	@Override
	public void reduce(Integer partitionKey, Collection values) {
		this.results.put(partitionKey, values);
	}

	@Override
	public Object collate() {
		return results.values();
	}

}
