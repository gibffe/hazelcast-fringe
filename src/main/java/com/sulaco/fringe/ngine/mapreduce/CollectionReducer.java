package com.sulaco.fringe.ngine.mapreduce;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings({"rawtypes","unchecked"})
public class CollectionReducer implements PartitionReducer {

	protected ConcurrentMap<Integer, Collection> results = new ConcurrentHashMap<Integer, Collection>();
	
	@Override
	public void reduce(Integer partitionKey, Object result) {
		if (result != null) {
			this.results.put(partitionKey, (Collection) result);
		}
	}

	@Override
	public Object collate() {
		Collection result = new ArrayList();
		for (Collection value : results.values()) {
			result.addAll(value);
		}
		return result;
	}

}
