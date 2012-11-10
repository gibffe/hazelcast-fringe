package com.sulaco.fringe.ngine.mapreduce;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.hazelcast.partition.PartitionService;
import com.sulaco.fringe.ngine.partition.PartitionKeyArgument;
import com.sulaco.fringe.ngine.partition.PartitionKeyGenerator;

@SuppressWarnings({"unchecked", "rawtypes"})
public class CollectionMapper implements PartitionMapper {

	protected PartitionService partitionService;
	
	protected PartitionKeyGenerator keygen;
	
	protected Map<Integer, Collection> splits = new HashMap<Integer, Collection>();
	
	public CollectionMapper() {
		super();
	}
	
	public CollectionMapper(PartitionKeyGenerator keygen, PartitionService partitionService) {
		this();
		this.keygen = keygen;
		this.partitionService = partitionService;
	}
	
	@Override
	public Map<Integer, Collection> map(Collection input) {
	
		Integer key, partitionId;
		PartitionKeyArgument arg = new PartitionKeyArgument();
		
		// split incoming collection by partition id it belongs to
		//
		if (input != null) {
			for (Object element : input) {
				arg.setTarget(element);
				key = keygen.generate(arg);
				
				partitionId = partitionService.getPartition(key).getPartitionId();
				
				// update split for this partition key
				if (!splits.containsKey(partitionId)) {
					splits.put(partitionId, new ArrayList());
				}
				splits.get(partitionId).add(element);
			}
		}
		//
		return this.splits;
	}
	
	@Override
	public Integer getPartitionKey(Object object) {
		PartitionKeyArgument arg = new PartitionKeyArgument(object);
		return keygen.generate(arg);
	}

	public void setKeygen(PartitionKeyGenerator keygen) {
		this.keygen = keygen;
	}

	public void setPartitionService(PartitionService partitionService) {
		this.partitionService = partitionService;
	}
	
}
