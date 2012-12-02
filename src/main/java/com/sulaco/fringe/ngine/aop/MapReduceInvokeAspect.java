package com.sulaco.fringe.ngine.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.partition.Partition;
import com.hazelcast.partition.PartitionService;
import com.sulaco.fringe.annotation.PartitionKey;
import com.sulaco.fringe.annotation.PartitionMapReduce;
import com.sulaco.fringe.exception.PartitionExecutionException;
import com.sulaco.fringe.exception.PartitionInvokeException;
import com.sulaco.fringe.ngine.FringeEvent;
import com.sulaco.fringe.ngine.aop.PartitionInvokeAspect.PartitionKeyTrace;
import com.sulaco.fringe.ngine.mapreduce.PartitionMapper;
import com.sulaco.fringe.ngine.mapreduce.PartitionReducer;
import com.sulaco.fringe.ngine.partition.PartitionFuture;
import com.sulaco.fringe.ngine.partition.PartitionKeyGenerator;

@Aspect
@SuppressWarnings("rawtypes")
public class MapReduceInvokeAspect {

	private HazelcastInstance hazelcast;
	
	protected ConcurrentMap<String, PartitionKeyTrace> traces = new ConcurrentHashMap<String, PartitionKeyTrace>();
	
	@Around("MapReduceInvokeAspect.partitionInvocationPointcut(pmr)")
	public Object mapreduceInvocation(ProceedingJoinPoint pjp, PartitionMapReduce pmr) throws Throwable {
		
		if (hazelcast != null) {
			
			Member local = hazelcast.getCluster().getLocalMember();
			
			// look for PartitionKey annotation in the method signature
			MethodSignature signature = (MethodSignature) pjp.getStaticPart().getSignature();
			PartitionKeyTrace trace   = getPartitionKeyTrace(pjp.getTarget(), signature);
			
			// split input collection into subsets, each subset resolving to
			// a grid partition it has affinity to
			PartitionReducer reducer = getPartitionReducer(pmr);
			PartitionMapper  mapper  = getPartitionMapper(pmr);
			
			if (mapper != null && reducer != null) {
				// map input to partitions
				Collection input = (Collection) pjp.getArgs()[trace.getPidx()];
				Map<Integer, Collection> splits = mapper.map(input);
				
				// process mapped input
				List<PartitionFuture> futures = new ArrayList<PartitionFuture>();
				for (Map.Entry<Integer, Collection> entry : splits.entrySet()) {
					if (!entry.getValue().isEmpty()) {
						
						// this is a dirty hack; seems I can't just use partition id :(
						Integer partitionKey = mapper.getPartitionKey(head(entry.getValue()));
						
						// replace original collection with current split
						Object[] args = pjp.getArgs();
						args[trace.getPidx()] = entry.getValue();
						
						// does this split belong to this member's partition ?
						Partition partition = hazelcast.getPartitionService().getPartition(partitionKey);
						if (partition.getOwner().equals(local)) {
							reducer.reduce(entry.getKey(), pjp.proceed(args));
						}
						else {
							// fire fringe event
							FringeEvent fevent = new FringeEvent(	partitionKey, 
																	signature.getDeclaringTypeName(), 
																	signature.getName(), 
																	signature.getParameterTypes(), 
																	args
							);
							try {
								Future<Object> fut = hazelcast.getExecutorService().submit(fevent);
								futures.add(
											new PartitionFuture(entry.getKey(), fut)
								);
							}
							catch (Exception ex) {
								throw new PartitionExecutionException("Unable to complete map-reduce execution :(", ex);
							}
						}
					}
				}
				// wait for collected futures to complete (best effort)
				//
				for (PartitionFuture fut : futures) {
					Object result = getFutureResult(fut);
					if (result != null) {
						reducer.reduce(fut.getPartitionId(), result);
					}
				}
				return reducer.collate();
			}
			else {
				log.log(Level.WARNING, "Either ParitionMapper or PartitionReducer is null, invocation will not be distributed !");
				return pjp.proceed();
			}
		}
		else {
			log.log(Level.WARNING, "Hazelcast instance has not been autowired, defaulting to non-distributed execution :?");
			return pjp.proceed();
		}
		
	}

	protected PartitionMapper getPartitionMapper(PartitionMapReduce pmr) {
		
		PartitionMapper mapper = null;
		try {
			Constructor c = pmr.mapper().getConstructor(PartitionKeyGenerator.class, PartitionService.class);
			mapper = (PartitionMapper) c.newInstance(pmr.keygen().newInstance(), hazelcast.getPartitionService());
		} 
		catch (Exception ex) {
			log.log(Level.SEVERE, "Unable to instantiate PartitionMapper !", ex);
		}
		//
		return mapper;	
	}
	
	protected PartitionReducer getPartitionReducer(PartitionMapReduce pmr) {
		
		PartitionReducer reducer = null;
		try {
			reducer = pmr.reducer().newInstance();
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Unable to instantiate PartitionReducer !", ex);
		}
		//
		return reducer;
	}
	
	protected PartitionKeyTrace getPartitionKeyTrace(Object target, MethodSignature signature) {
		// inspect cached traces first
		PartitionKeyTrace trace = traces.get(signature.getMethod().toString());
		if (trace == null) {
			
			Method method = getTargetMethod(target, signature);
			
			if (method != null) {
				Annotation[][] pann = signature.getMethod().getParameterAnnotations();
				
				PartitionKey pk = null;
				int pidx = -1; // @PartitionKey annotation index
				
				for (int i = 0; i < pann.length; i++) {
					if (pann[i].length > 0) {
						for (Annotation an : pann[i]) {
							if (an instanceof PartitionKey) {
								// got ya bitch
								pk = (PartitionKey) an;
								pidx = i;
								break;
							}
						}
					}
					if (pidx >= 0) break;
				}
				
				if (pk != null) {
					// cache this
					trace = new PartitionKeyTrace(pk, pidx);
					traces.put(signature.getMethod().toString(), trace);
				}
				else {
					throw new PartitionInvokeException("Unable to find @PartitionKey annotated parameter :?");	
				}
			}
		}
		//
		return trace;
	}
	
	private Object getFutureResult(PartitionFuture fut) {
		Object result = null;
		try {
			result = fut.get();
		} 
		catch (Exception ex) {
			log.log(Level.WARNING, "Error executing one of map-reduce task :?", ex);
		}
		//
		return result;
	}
	
	private Method getTargetMethod(Object target, MethodSignature signature) {
		Method method = null;
		try {
			method = target.getClass().getMethod(signature.getName(), signature.getParameterTypes());
		} 
		catch (Exception ex) {
			log.log(Level.WARNING, "Unable to find requested method !", ex);
		} 
		return method;
	}
	
	private Object head(Collection input) {
		return input.iterator().next();
	}
	
	@Pointcut("@annotation(pmr)")
	public void partitionInvocationPointcut(PartitionMapReduce pmr) {
	
	}
	
	public void setHazelcast(HazelcastInstance hazelcast) {
		this.hazelcast = hazelcast;
	}
	
	private static final ILogger log = Logger.getLogger("jdk");

}
