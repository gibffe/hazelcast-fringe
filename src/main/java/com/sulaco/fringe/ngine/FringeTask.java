package com.sulaco.fringe.ngine;

import java.util.Set;
import java.util.concurrent.Callable;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.Member;

public class FringeTask extends DistributedTask<Object> {

	public FringeTask(Callable<Object> callable, Set<Member> members) {
		super(callable, members);
	}

}
