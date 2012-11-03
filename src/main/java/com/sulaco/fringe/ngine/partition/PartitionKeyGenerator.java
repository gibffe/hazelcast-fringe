package com.sulaco.fringe.ngine.partition;


public interface PartitionKeyGenerator {

	public int generate(PartitionKeyArgument key);
}
