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
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
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

	@Autowired HazelcastInstance hazelcast;
	
	private ConcurrentMap<String, PartitionKeyTrace> traces = new ConcurrentHashMap<>();
	
	@Around("PartitionInvokeAspect.partitionInvocationPointcut(pi)")
	public Object partitionInvocation(ProceedingJoinPoint pjp, PartitionInvoke pi) throws Throwable {
		
		if (hazelcast != null) {
			// mkay, look for param annotated with @PartitionKey
			MethodSignature signature = (MethodSignature) pjp.getStaticPart().getSignature();
			
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
	
			// generate partition key
			PartitionKeyArgument pkarg = new PartitionKeyArgument(
													pjp.getArgs()[trace.getPidx()], // annotated param 
													trace.getPk().property()		// optional property within param bean
			);
			PartitionKeyGenerator keygen = pi.keygen().newInstance();
			
			int partitionKey = keygen.generate(pkarg);
	
			// and create a fringe event
			FringeEvent fevent = new FringeEvent(	partitionKey, 
													signature.getDeclaringTypeName(), 
													signature.getName(), 
													signature.getParameterTypes(), 
													pjp.getArgs()
			);
			try {
				Future<Object> fut = hazelcast.getExecutorService().submit(fevent);
				return fut.get();
			}
			catch (Exception ex) {
				throw new PartitionExecutionException("Unable to complete distributed execution :(", ex);
			}
		}
		else {
			log.log(
					Level.WARNING, 
					"Hazelcast instance has not been autowired, defaulting to non-distributed execution :?"
			);
			return pjp.proceed();
		}
	}
	
	@Pointcut("@annotation(pi)")
	public void partitionInvocationPointcut(PartitionInvoke pi) {
		
	}
	
	private static final ILogger log = Logger.getLogger("jdk");
	
	private class PartitionKeyTrace {
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
