package com.sulaco.fringe;

import com.sulaco.fringe.TestServiceImpl.TestBean;

public interface TestService {

	public String getAccount1(Integer accountId);

	public String getAccount2(TestBean param);

	public String getAccount3(TestBean param);

	public String getAccount4(TestBean param);
}
