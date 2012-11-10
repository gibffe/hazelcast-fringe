package com.sulaco.fringe;

import java.util.Collection;

import com.sulaco.fringe.TestServiceImpl.TestBean;

public interface TestService {

	public String getAccount1(Integer accountId);
	
	public String getAccount2(Integer a, Integer b, Integer c);

	public String getAccount3(TestBean param);

	public String getAccount4(TestBean param);

	public String getAccount5(TestBean param);
	
	public Collection<Integer> processCollection(Collection<Integer> collection);
}
