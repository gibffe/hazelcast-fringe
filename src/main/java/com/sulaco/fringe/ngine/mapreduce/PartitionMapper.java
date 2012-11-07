package com.sulaco.fringe.ngine.mapreduce;

import java.util.Collection;
import java.util.Map;

@SuppressWarnings("rawtypes")
public interface PartitionMapper {

	public Map<Integer, Collection> map(Collection input);
	
}
