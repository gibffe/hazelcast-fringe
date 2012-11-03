package com.sulaco.fringe.ngine;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class FringeContext implements ApplicationContextAware {

	private static ApplicationContext ctx = null;
	
	private static ConcurrentMap<String, Method> methodCache = new ConcurrentHashMap<>();
	
	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		FringeContext.ctx = ctx;
	}
	
	public static Object getBean(String className) throws BeansException, ClassNotFoundException {
		return ctx.getBean(Class.forName(className));
	}
	
	public static Method getMethod(Object target, String methodName, Class<?>[] paramTypes) {
		
		StringBuilder sb = new StringBuilder(methodName);
		for (Class<?> paramType : paramTypes) {
			sb.append(paramType.getCanonicalName());
		}
		
		String methodKey = sb.toString();
		Method method = methodCache.get(methodKey);
		if (method == null) {
			try {
				method = target.getClass().getMethod(methodName, paramTypes);
				methodCache.put(methodKey, method);
			}
			catch (Exception ex) {
				// unable to fetch method !
				method = null;
			}
		}
		
		return method;
	}

}
