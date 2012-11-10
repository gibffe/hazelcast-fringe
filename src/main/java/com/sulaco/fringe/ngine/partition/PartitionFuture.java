package com.sulaco.fringe.ngine.partition;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class PartitionFuture {

	protected Integer partitionId;
	protected Future<Object> future;
	
	public PartitionFuture(Integer partitionId, Future<Object> future) {
		this.partitionId = partitionId;
		this.future = future;
	}
	
	public Object get() throws InterruptedException, ExecutionException {
		return this.future.get();
	}

	public Integer getPartitionId() {
		return partitionId;
	}

	public void setPartitionId(Integer partitionId) {
		this.partitionId = partitionId;
	}

	public Future<Object> getFuture() {
		return future;
	}

	public void setFuture(Future<Object> future) {
		this.future = future;
	}
}
