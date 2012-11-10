hazelcast-fringe
================

Java method level annotations enabling affinity based execution on top of hazelcast cluster and spring framework.

Examples:

	```java
	@PartitionInvoke
	public String testMethod(..., @PartitionKey Integer argument, ...)
	```
