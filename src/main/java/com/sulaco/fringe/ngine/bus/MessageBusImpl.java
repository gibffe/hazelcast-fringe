package com.sulaco.fringe.ngine.bus;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.partition.Partition;
import com.sulaco.fringe.annotation.PartitionEvent;
import com.sulaco.fringe.annotation.PartitionEventSubscribe;
import com.sulaco.fringe.exception.PartitionExecutionException;
import com.sulaco.fringe.ngine.FringeContext;
import com.sulaco.fringe.ngine.FringeEvent;
import com.sulaco.fringe.ngine.FringeTask;
import com.sulaco.fringe.ngine.SpringWired;
import com.sulaco.fringe.ngine.partition.PartitionKeyArgument;
import com.sulaco.fringe.ngine.partition.PartitionKeyGenerator;

public class MessageBusImpl implements MessageBus, BeanPostProcessor {

	protected ExecutorService exe = Executors.newCachedThreadPool();
	
	protected HazelcastInstance hazelcast;
	
	// multimap of event subscriber instances, keyed by bean name - single bean can subscribe to multiple
	// events, resulting in many EventBusSubscriber object per single bean;
	// this map is used in bean preprocessing to replace raw instances with proxies
	private Map<String, Set<EventBusSubscriber>> beans_index = new HashMap<String, Set<EventBusSubscriber>>();
	
	// multimap of event subscribers, keyed by event type name
	protected Map<String, Set<EventBusSubscriber>> subscribers = new HashMap<String, Set<EventBusSubscriber>>();
	
	@Override
	public void emit(final Object event) {
		try {
			PartitionEvent pe = event.getClass().getAnnotation(PartitionEvent.class);
			if (pe != null) {
				Integer partitionKey = getPartitionKey(pe, event);
				
				// determine if that is going to be a local or distributed emit invocation;
				Partition partition = hazelcast.getPartitionService().getPartition(partitionKey);
				Member local = hazelcast.getCluster().getLocalMember();
				if (partition.getOwner().equals(local)) {
					// local
					this.emitLocal(event);
				}
				else {
					// distributed, fringe event
					FringeEvent fevent = new FringeEvent(	partitionKey, 
															this.getClass().getName(), 
															"emit", 
															new Class<?>[]  {Object.class}, 
															new Object  []  {event       }
					);
					try {
						hazelcast.getExecutorService().submit(fevent);
					}
					catch (Exception ex) {
						throw new PartitionExecutionException("Unable to complete distributed execution :(", ex);
					}
				}
			}
		}
		catch (Throwable ex) {
			// emit does not escalate anything, log stuff here for later inspection
			log.log(Level.SEVERE, "Unable to emit event ! "+event != null ? event.toString() : "null", ex);
		}
	}
	
	@Override
	public void emitLocal(Object event) {
		exe.submit(
				new EventForwardingRunnable(event)
		);
	}
	
	@Override
	public void broadcast(final Object event) {
		try {
			FringeTask ftask = new FringeTask(
											new FringeEvent(
														0, 
														this.getClass().getName(), 
														"emitLocal", 
														new Class<?>[]  {Object.class}, 
														new Object  []  {event       }
											),
											hazelcast.getCluster().getMembers()
			);
			hazelcast.getExecutorService().submit(ftask);
		}
		catch (Throwable ex) {
			// broadcast does not escalate anything, log stuff here for later inspection
			log.log(Level.SEVERE, "Unable to broadcast event !" + event != null ? event.toString() : "null", ex);
			
		}
	}
	
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		// look for methods annotated with @PartitionEventSubscribe in raw beans
		Method[] methods = bean.getClass().getDeclaredMethods();
		if (methods != null) {
			
			Class<?>[] types;
			PartitionEventSubscribe pes;
			
			for (Method m : methods) {
				pes = m.getAnnotation(PartitionEventSubscribe.class);
				if (pes != null) {
					// register new EventSubscriber
					subscribe(	m.getName(), 
								m.getParameterTypes(), 
								beanName
					);
				}
			}
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		// some of the beans migh get proxied by now - need to replace raw instances with 
		// appropriate proxies so other aop based stuff is working as expected
		//
		if (beans_index.containsKey(beanName)) {
			for (EventBusSubscriber sub : beans_index.get(beanName)) {
				sub.setTarget(bean);
			}
		}
		return bean;
	}

	void subscribe(String handlerMethod, Class<?>[] handlerParams, String beanName) {
		
		String eventClass= handlerParams[0].getName();
		
		if (!this.subscribers.containsKey(eventClass)) {
			this.subscribers.put(eventClass, new HashSet<EventBusSubscriber>());
		}
		if (!this.beans_index.containsKey(beanName)) {
			this.beans_index.put(beanName, new HashSet<EventBusSubscriber>());
		}
		
		// save subscriber in multimaps
		EventBusSubscriber sub = new EventBusSubscriber(handlerMethod, handlerParams);
		this.subscribers.get(eventClass).add(sub);
		this.beans_index.get(beanName).add(sub);
	}
	
	protected Integer getPartitionKey(PartitionEvent pe, Object message) throws Throwable {
		
		PartitionKeyGenerator keygen;
		if(SpringWired.class.isAssignableFrom(pe.keygen())) {
			keygen = FringeContext.getBean(pe.keygen().getName());
		}
		else {
			keygen = pe.keygen().newInstance(); // TODO: cache instances, they should have no state anyway
		}
		
		return keygen.generate(
							new PartitionKeyArgument(message, pe.property())
		);
	}

	
	public void setExe(ExecutorService exe) {
		this.exe = exe;
	}

	public void setHazelcast(HazelcastInstance hazelcast) {
		this.hazelcast = hazelcast;
	}
	
	public Map<String, Set<EventBusSubscriber>> getSubscribers() {
		return subscribers;
	}

	private static class EventBusSubscriber {
		
		private Object target;
		private Method method;
		
		private String     handlerMethod;
		private Class<?>[] handlerParams;
		
		public EventBusSubscriber(String handlerMethod, Class<?>[] handlerParams) {
			this.handlerMethod = handlerMethod;
			this.handlerParams = handlerParams;
		}
		
		public void call(Object message) {
			try {
				switch(handlerParams.length) {
					case 1 :{
					// basic invocation
							method.invoke(target, message);
							break;
					}
					case 2 : {
					// invocation with target proxy
							method.invoke(target, message, target);
							break;
					}
				}
			}
			catch (Exception ex) {
				log.log(Level.SEVERE, "Unable to execute "+handlerMethod+" for message "+message != null ? message.toString() : "null", ex);
			}
		}
		
		public void setTarget(Object target) {
			try {
				this.target = target;
				this.method = target.getClass().getMethod(handlerMethod, handlerParams);
			}
			catch (Exception ex) {
				log.log(Level.SEVERE, "Unable to get event handler method !", ex);
			}
			
		}
		
		private static final ILogger log = Logger.getLogger("jdk");
	}
	
	/**
	 * Forwards a single event to all locally registered subscribers
	 * 
	 */
	private class EventForwardingRunnable implements Runnable {
		
		private final Object event;
		
		public EventForwardingRunnable(Object event) {
			this.event = event;
		}
		
		public void run() {
			// forward to registered subscribers
			Set<EventBusSubscriber> subs = subscribers.get(event.getClass().getName());
			if (subs != null) {
				for (EventBusSubscriber sub : subs) {
					sub.call(event);
				}
			}
		}
	}
	
	private static final ILogger log = Logger.getLogger("jdk");
}
