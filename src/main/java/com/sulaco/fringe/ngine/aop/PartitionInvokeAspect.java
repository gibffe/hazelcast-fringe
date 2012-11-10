package com.sulaco.fringe.ngine.aop;
import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.partition.Partition;
import com.sulaco.fringe.annotation.PartitionInvoke;
import com.sulaco.fringe.annotation.PartitionKey;
import com.sulaco.fringe.exception.PartitionExecutionException;
import com.sulaco.fringe.exception.PartitionInvokeException;
import com.sulaco.fringe.ngine.FringeEvent;
import com.sulaco.fringe.ngine.partition.PartitionKeyArgument;
import com.sulaco.fringe.ngine.partition.PartitionKeyGenerator;


@Aspect
@SuppressWarnings("all")
public class PartitionInvokeAspect {

	protected HazelcastInstance hazelcast;
	
	protected ConcurrentMap<String, PartitionKeyTrace> traces = new ConcurrentHashMap<String, PartitionKeyTrace>();
	
	@Around("PartitionInvokeAspect.partitionInvocationPointcut(pi)")
	public Object partitionInvocation(ProceedingJoinPoint pjp, PartitionInvoke pi) throws Throwable {
		
		if (hazelcast != null) {
			// look for PartitionKey annotation in the method signature
			MethodSignature signature = (MethodSignature) pjp.getStaticPart().getSignature();
			PartitionKeyTrace trace   = getPartitionKeyTrace(signature);
					
			// generate partition key
			Integer partitionKey = generatePartitionKey(pjp, trace, pi);
			
			// determine if that is going to be a local or distributed invocation; for incoming distributed 
			// invocation from fringe event it has to resolve to local, otherwise infinite ping-pong 
			// between universes will unfold ;)
			//
			Partition partition = hazelcast.getPartitionService().getPartition(partitionKey);
			Member local = hazelcast.getCluster().getLocalMember();
			if (partition.getOwner().equals(local)) {
				// local
				//
				return pjp.proceed();
			}
			else {
				// distributed
				//
				FringeEvent fevent = new FringeEvent(	partitionKey, 
														signature.getDeclaringTypeName(), 
														signature.getName(), 
														signature.getParameterTypes(), 
														pjp.getArgs()
				);
				try {
					Future<Object> fut = hazelcast.getExecutorService().submit(fevent);
					return pi.blocking() ? fut.get() : fut;
				}
				catch (Exception ex) {
					throw new PartitionExecutionException("Unable to complete distributed execution :(", ex);
				}
			}
		}
		else {
			log.log(Level.WARNING, "Hazelcast instance has not been autowired, defaulting to non-distributed execution :?");
			return pjp.proceed();
		}
	}
	
	private Integer generatePartitionKey(ProceedingJoinPoint pjp, PartitionKeyTrace trace, PartitionInvoke pi) throws Exception {
		PartitionKeyArgument pkarg = new PartitionKeyArgument(
						pjp.getArgs()[trace.getPidx()], // annotated param 
						trace.getPk().property()		// optional property within param bean
		);
		PartitionKeyGenerator keygen = pi.keygen().newInstance();
		
		return keygen.generate(pkarg);
	}
	
	protected PartitionKeyTrace getPartitionKeyTrace(MethodSignature signature) {
		// inspect cached traces first
		PartitionKeyTrace trace = traces.get(signature.getMethod().toString());
		if (trace == null) {
			
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
		//
		return trace;
	}
	
	@Pointcut("@annotation(pi)")
	public void partitionInvocationPointcut(PartitionInvoke pi) {
		
	}
	
	public void setHazelcast(HazelcastInstance hazelcast) {
		this.hazelcast = hazelcast;
	}

	private static final ILogger log = Logger.getLogger("jdk");
	
	public static class PartitionKeyTrace {
		private PartitionKey pk;
		private int pidx;
		
		public PartitionKeyTrace(PartitionKey pk, int pidx) {
			this.pk  = pk;
			this.pidx = pidx;
		}

		public PartitionKey getPk() {
			return pk;
		}

		public int getPidx() {
			return pidx;
		}
	}
}
