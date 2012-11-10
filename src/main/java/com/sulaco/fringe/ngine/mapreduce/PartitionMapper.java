package com.sulaco.fringe.ngine.mapreduce;

import java.util.Collection;
import java.util.Map;

@SuppressWarnings("rawtypes")
public interface PartitionMapper {

	/**
	 * Splits incoming input by partition id it has affinity to
	 * 
	 * @param input collection of elements
	 * @return map keyed by partition id, with values representing original collection subsets
	 */
	public Map<Integer, Collection> map(Collection input);
	
	/**
	 * Returns a partition key for object supplied - it has to use same keygen the map method used
	 * 
	 * @param object object we want to generate partition key for
	 * @return partition key
	 */
	public Integer getPartitionKey(Object object);
		
}
