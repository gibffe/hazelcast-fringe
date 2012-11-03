package com.sulaco.fringe.ngine.partition;

public class HashcodeKeyGenerator implements PartitionKeyGenerator {

	@Override
	public int generate(PartitionKeyArgument key) {
		return key.hashCode();
	}

}
