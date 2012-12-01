package com.sulaco.fringe.ngine.bus;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.partition.Partition;
import com.hazelcast.util.ConcurrentHashSet;
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

	private ExecutorService exe = Executors.newCachedThreadPool();
	
	private HazelcastInstance hazelcast;
	
	// global topic listener, forwarding incoming events towards subscribers
	private EventForwardingListener eventForwardingListener = new EventForwardingListener();
	
	// multimap of event subscribers, keyed by event type name
	private ConcurrentMap<String, Set<EventBusSubscriber>> subscribers = new ConcurrentHashMap<String, Set<EventBusSubscriber>>();
	
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
			log.log(Level.WARNING, "Unable to emit event ! "+event != null ? event.toString() : "null", ex);
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
			log.log(Level.WARNING, "Unable to broadcast event !" + event != null ? event.toString() : "null", ex);
			
		}
	}
	
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		// look for methods annotated with @PartitionEventSubscribe
		Method[] methods = bean.getClass().getDeclaredMethods();
		if (methods != null) {
			
			Class<?>[] types;
			String typeName;
			MessageListener<Object> listener;
			PartitionEventSubscribe pes;
			
			for (Method m : methods) {
				pes = m.getAnnotation(PartitionEventSubscribe.class);
				if (pes != null) {
					// register new EventSubscriber
					types = m.getParameterTypes();
					if (types.length == 1) {
						typeName = m.getParameterTypes()[0].getName();
						subscribe(bean, m, typeName);
					}
				}
			}
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	void subscribe(Object target, Method method, String eventClass) {
		
		if (!this.subscribers.containsKey(eventClass)) {
			synchronized(this) {
				if (!this.subscribers.containsKey(eventClass)) {
					this.subscribers.put(eventClass, new ConcurrentHashSet<MessageBusImpl.EventBusSubscriber>());
				}
			}
		}
		
		// save subscriber in multimap
		this.subscribers.get(eventClass).add(
				new EventBusSubscriber(target, method)
		);
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
	
	public ConcurrentMap<String, Set<EventBusSubscriber>> getSubscribers() {
		return subscribers;
	}

	private static class EventBusSubscriber {
		final Object target;
		final Method method;
		
		public EventBusSubscriber(Object target, Method method) {
			this.target = target;
			this.method = method;
		}
		
		public void call(Object message) {
			try {
				method.invoke(target, message);
			}
			catch (Exception ex) {
				log.log(Level.WARNING, "Unable to execute "+method.toString()+" for message "+message != null ? message.toString() : "null", ex);
			}
		}
		
		private static final ILogger log = Logger.getLogger("jdk");
	}
	
	/**
	 * Forwards events from the topic onto localy registered subscribers
	 *
	 */
	private class EventForwardingListener implements MessageListener<Object> {

		@Override
		public void onMessage(Message<Object> message) {
			exe.submit(
					new EventForwardingRunnable(
										message.getMessageObject()	
					)
			);
		}
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
