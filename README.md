hazelcast-fringe
================

- Java method level annotations enabling partition-affinity execution on top of hazelcast grid and spring framework.
- Simple event bus on top of Hazelcast, capable of partition-aware event routing and broadcasting to all members 
  [wiki] https://github.com/gibffe/hazelcast-fringe/wiki/message-bus

Examples:
---------

Invokes test method on a hazelcast partition. Hashcode of parameter annotated with @PartitionKey will be used as partition key.

    @PartitionInvoke
    public String testMethod(..., @PartitionKey Integer argument, ...)

Same as above, but uses a member variable (getter must exist) of selected argument as partition key.

    @PartitionInvoke
    public String testMethod(..., @PartitionKey(property="param1") SomeType argument, ...)

Invokes test method on a hazelcast partition. Using custom key generator to come up with partition key from argument annotated with @PartitionKey.

    @PartitionInvoke(keygen=CustomPartitionKeyGen.class)
    public String testMethod(..., @PartitionKey SomeType argument, ...)

Map-reduce invocation example

    @PartitionMapReduce
    public Collection<Integer> processCollection(@PartitionKey Collection<Integer> input)

Event-handler registration example

    @PartitionEventSubscribe
    public void handleEvent(AccountRegistered event) {
        ...
    }
    
Tagging an object as event

    @PartitionEvent
    public class AccountRegistered {
        ...
    }
	

Available annotations:
-------------------------

For partition invocation (method level)

	public @interface PartitionInvoke {

		Class<? extends PartitionKeyGenerator> keygen() default HashcodeKeyGenerator.class;	
		
		boolean blocking() default true;

	}
	
For map-reduce (method level)

	public @interface PartitionMapReduce {

		Class<? extends PartitionKeyGenerator> keygen()  default HashcodeKeyGenerator.class;	
		Class<? extends PartitionMapper>       mapper()  default CollectionMapper.class;
		Class<? extends PartitionReducer>      reducer() default CollectionReducer.class;

	}
	
For selecting partition key (method parameter level)

	public @interface PartitionKey {

		String property() default "";
	}
	
Installation:
-------------

Not published to maven repo yet - build from source. Once build you will need some spring wiring:

Manual:

	<aop:aspectj-autoproxy />
	
	<bean class="com.sulaco.fringe.ngine.FringeContext" />
	
	<bean class="com.sulaco.fringe.ngine.aop.PartitionInvokeAspect">
		<property name="hazelcast" ref="hazelcast" />
	</bean>
	
	<bean class="com.sulaco.fringe.ngine.aop.MapReduceInvokeAspect">
		<property name="hazelcast" ref="hazelcast" />
	</bean>

Automatic: (note, in this case hazelcast instance id has to be 'hazelcast')

    <import resource="classpath:/com/sulaco/fringe/context.xml" />
	
That should be it !
