package com.sulaco.fringe;

import com.sulaco.fringe.annotation.PartitionInvoke;
import com.sulaco.fringe.annotation.PartitionKey;
import com.sulaco.fringe.ngine.partition.PartitionKeyArgument;
import com.sulaco.fringe.ngine.partition.PartitionKeyGenerator;

public class TestServiceImpl implements TestService {

	@PartitionInvoke
	public String getAccount(@PartitionKey Integer accountId) {
		return "account";
	}
	
	@PartitionInvoke
	public String getAccount(@PartitionKey TestBean param) {
		return "account";
	}
	
	@PartitionInvoke
	public String getAccount1(@PartitionKey(property="param1") TestBean param) {
		return "account";
	}
	
	@PartitionInvoke(keygen=CustomPartitionKeyGen.class)
	public String getAccount2(@PartitionKey TestBean param) {
		return "account";
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
