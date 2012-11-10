package com.sulaco.fringe;

import java.util.ArrayList;
import java.util.Collection;

import com.sulaco.fringe.annotation.PartitionInvoke;
import com.sulaco.fringe.annotation.PartitionKey;
import com.sulaco.fringe.annotation.PartitionMapReduce;
import com.sulaco.fringe.ngine.partition.PartitionKeyArgument;
import com.sulaco.fringe.ngine.partition.PartitionKeyGenerator;

public class TestServiceImpl implements TestService {

	@PartitionInvoke
	public String getAccount1(@PartitionKey Integer accountId) {
		return "account1";
	}
	
	@Override
	public String getAccount2(Integer a, @PartitionKey Integer b, Integer c) {
		return "account2";
	}	
	
	@PartitionInvoke
	public String getAccount3(@PartitionKey TestBean param) {
		return "account2";
	}
	
	@PartitionInvoke
	public String getAccount4(@PartitionKey(property="param1") TestBean param) {
		return "account3";
	}
	
	@PartitionInvoke(keygen=CustomPartitionKeyGen.class)
	public String getAccount5(@PartitionKey TestBean param) {
		return "account4";
	}
	
	@PartitionMapReduce
	public Collection<Integer> processCollection(@PartitionKey Collection<Integer> input) {
		return input;
	}

	public static class TestBean {
		private String param1;
		private String param2;
		
		public TestBean(String param1, String param2) {
			this.param1 = param1;
			this.param2 = param2;
		}

		public String getParam1() {
			return param1;
		}

		public String getParam2() {
			return param2;
		}
	}
	
	public static class CustomPartitionKeyGen implements PartitionKeyGenerator {

		public int generate(PartitionKeyArgument arg) {
			TestBean tb = arg.getTarget();
			return tb.getParam2().hashCode();
		}
		
	}

}
