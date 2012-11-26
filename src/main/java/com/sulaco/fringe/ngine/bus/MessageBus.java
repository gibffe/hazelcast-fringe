package com.sulaco.fringe.ngine.bus;


public interface MessageBus {

	public void emit(Object event);
	
	public void emitLocal(Object event);
	
	public void broadcast(Object event);
	
}
