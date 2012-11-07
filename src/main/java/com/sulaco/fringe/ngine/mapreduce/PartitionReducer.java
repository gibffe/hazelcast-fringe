package com.sulaco.fringe.ngine.mapreduce;

import java.util.Collection;

@SuppressWarnings("rawtypes")
public interface PartitionReducer {

	public void reduce(Integer partitionKey, Collection values);
	
	public Object collate();
	
}
