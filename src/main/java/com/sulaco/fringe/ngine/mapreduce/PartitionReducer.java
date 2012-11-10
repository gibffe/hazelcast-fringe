package com.sulaco.fringe.ngine.mapreduce;


@SuppressWarnings("rawtypes")
public interface PartitionReducer {

	public void reduce(Integer partitionId, Object result);
	
	public Object collate();
	
}
