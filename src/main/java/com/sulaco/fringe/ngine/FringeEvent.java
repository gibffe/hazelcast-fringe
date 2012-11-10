package com.sulaco.fringe.ngine;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import com.hazelcast.core.PartitionAware;

public class FringeEvent implements Callable<Object>, PartitionAware<Integer>, Serializable {

	private Integer partitionKey;
	
	private String className;
	private String methodName;
	
	private Class<?>[] paramTypes;
	private Object[]   paramValues;
	
	public FringeEvent(	Integer	   partitionKey, 
						String 	   className, 
						String 	   methodName, 
						Class<?>[] paramTypes,
						Object[]   paramValues
	) {
		this.partitionKey = partitionKey; // for event grid routing
	
		this.className  = className;
		this.methodName = methodName;
		
		this.paramTypes  = paramTypes;
		this.paramValues = paramValues;
	}
	
	public Integer getPartitionKey() {
		return this.partitionKey;
	}

	public Object call() throws Exception {
		Object result = null;
		
		Object target = FringeContext.getBean(this.className);
		if (target != null) {
			// get hold of the method
			Method m = FringeContext.getMethod(target, this.methodName, this.paramTypes);
			if (m != null) {
				result = m.invoke(target, this.paramValues); 
			}
		}
		//
		return result;
	}
	
	public Object[] getParamValues() {
		return paramValues;
	}

	private static final long serialVersionUID = 8702323710846384175L;

}
