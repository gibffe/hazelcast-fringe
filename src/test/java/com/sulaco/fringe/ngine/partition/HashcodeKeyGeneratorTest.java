package com.sulaco.fringe.ngine.partition;

import static org.junit.Assert.*;

import org.junit.Test;

public class HashcodeKeyGeneratorTest {

	private HashcodeKeyGenerator keygen = new HashcodeKeyGenerator();
	
	@Test
	public void testGenerateKeyFromObject() {
		
		A a = new A();
		PartitionKeyArgument arg = new PartitionKeyArgument(a, "");
		
		int key = keygen.generate(arg);
		
		assertEquals(662, key);
	}
	
	@Test
	public void testGenerateKeyFromProperty() {
		
		A a = new A();
		a.setB(new B());
		
		PartitionKeyArgument arg = new PartitionKeyArgument(a, "b");
		
		int key = keygen.generate(arg);
		
		assertEquals(993, key);
	}
	
	private static class A {

		private B b;
		
		public B getB() {
			return b;
		}
		
		public void setB(B b) {
			this.b = b;
		}

		@Override
		public int hashCode() {
			return 1;
		}
	}
	
	private static class B {
		
		@Override
		public int hashCode() {
			return 2;
		}
		
	}

}
