package com.sulaco.fringe.ngine.aop;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sulaco.fringe.TestService;

@SuppressWarnings("all")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"aop-auto.xml"})
public class PartitionInvokeAspectTest {

	@Autowired TestService service;
	
	@Test
	public void testPrimitivePartitionKey() {
		int a =3;
		service.getAccount(1);
	}
	
	@Test
	public void testBeanParam() {
		
	}
	
	public static void main(String[] args) {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("/com/sulaco/fringe/ngine/aop/aop-auto.xml");

		TestService service = (TestService) ctx.getBean("testService");
		service.getAccount(1);

	}

}
