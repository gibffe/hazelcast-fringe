package com.sulaco.fringe.ngine.partition;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.StringUtils;

@SuppressWarnings("unchecked")
public class PartitionKeyArgument {

	private Object target;

	public PartitionKeyArgument(Object target, String parameter) {
		
		if (StringUtils.hasText(parameter)) {
			this.target = new BeanWrapperImpl(target).getPropertyValue(parameter);
		} 
		else {
			this.target = target;
		}
	}
	
	public <T> T getTarget() {
		return (T) this.target;
	}
	
	@Override
	public int hashCode() {
		return 331 * (1 + (target != null ? target.hashCode() : 0));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		
		if (o == null) {
			return false;
		}
		
		if (getClass() != o.getClass()) {
			return false;
		}
		
		final PartitionKeyArgument object = (PartitionKeyArgument) o;
		if (target == null) {
			if (object.target != null) {
				return false;
			}
		} 
		else if (!target.equals(object.target)) {
			return false;
		}
		
		return true;
	}
}
