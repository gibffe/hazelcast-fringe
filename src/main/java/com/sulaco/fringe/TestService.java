package com.sulaco.fringe;

import com.sulaco.fringe.TestServiceImpl.TestBean;

public interface TestService {

	public String getAccount(Integer accountId);

	public String getAccount(TestBean param);

	public String getAccount1(TestBean param);

	public String getAccount2(TestBean param);
}
