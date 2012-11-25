package com.sulaco.fringe.ngine.bus;


public interface MessageBus {

	public void emit(Object message);
	
	public void broadcast(Object message);
	
}
