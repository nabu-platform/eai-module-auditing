package be.nabu.eai.module.auditing;

import java.util.Date;
import java.util.Stack;
import java.util.UUID;

import be.nabu.eai.module.auditing.api.FlatServiceTracker;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackType;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.pojo.POJOUtils;

public class FlatServiceTrackerWrapper implements ServiceRuntimeTracker {

	private FlatServiceTracker tracker;
	private Stack<String> services = new Stack<String>();
	private Stack<String> steps = new Stack<String>();
	private UUID runId = UUID.randomUUID();
	private Stack<UUID> serviceInstanceIds = new Stack<UUID>();
	private Stack<UUID> stepInstanceIds = new Stack<UUID>();
	private Stack<Date> started = new Stack<Date>();
	private TrackType type;
	private boolean stopOnly;
	
	public FlatServiceTrackerWrapper(Service service, ExecutionContext context) {
		// force an empty tracker to prevent recursive tracker calls
		this.tracker = POJOUtils.newProxy(FlatServiceTracker.class, service, context);
	}
	
	public FlatServiceTrackerWrapper(FlatServiceTracker tracker) {
		this.tracker = tracker;
	}
	
	@Override
	public void start(Service service) {
		if (!stopOnly && (type == TrackType.SERVICE || type == TrackType.BOTH)) {
			service = ServiceUtils.unwrap(service);
			if (service instanceof DefinedService) {
				UUID instanceId = UUID.randomUUID();
				services.push(((DefinedService) service).getId());
				started.push(new Date());
				UUID parentId = serviceInstanceIds.isEmpty() ? null : serviceInstanceIds.peek();
				serviceInstanceIds.push(instanceId);
				tracker.track(
					runId,
					TrackType.SERVICE, 
					instanceId,
					parentId,
					ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(),
					((DefinedService) service).getId(),
					started.peek(),
					null,
					null
				);
			}
		}
	}
	
	@Override
	public void stop(Service service) {
		if (type == TrackType.SERVICE || type == TrackType.BOTH) {
			service = ServiceUtils.unwrap(service);
			if (!services.isEmpty() && service instanceof DefinedService) {
				if (!((DefinedService) service).getId().equals(services.peek())) {
					throw new RuntimeException("Service does not match the stack");
				}
				tracker.track(
					runId,
					TrackType.SERVICE,
					serviceInstanceIds.pop(), 
					serviceInstanceIds.isEmpty() ? null : serviceInstanceIds.peek(), 
					ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(), 
					services.pop(), 
					started.pop(),
					new Date(), 
					null
				);
				services.pop();
			}
		}
	}
	
	@Override
	public void error(Service service, Exception exception) {
		if (type == TrackType.SERVICE || type == TrackType.BOTH) {
			service = ServiceUtils.unwrap(service);
			if (!services.isEmpty() && service instanceof DefinedService) {
				if (!((DefinedService) service).getId().equals(services.peek())) {
					throw new RuntimeException("Service does not match the stack");
				}
				tracker.track(
					runId,
					TrackType.SERVICE,
					serviceInstanceIds.pop(), 
					serviceInstanceIds.isEmpty() ? null : serviceInstanceIds.peek(), 
					ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(), 
					services.pop(), 
					started.pop(),
					new Date(), 
					exception
				);
				services.pop();
			}
		}
	}
	
	@Override
	public void before(Object step) {
		if (!stopOnly && step instanceof String && (type == TrackType.STEP || type == TrackType.BOTH)) {
			UUID instanceId = UUID.randomUUID();
			steps.push((String) step);
			started.push(new Date());
			UUID parentId = stepInstanceIds.isEmpty() ? null : stepInstanceIds.peek();
			stepInstanceIds.push(instanceId);
			tracker.track(
				runId,
				TrackType.STEP, 
				instanceId,
				parentId,
				ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(),
				(String) step,
				started.peek(),
				null,
				null
			);
			steps.push((String) step);
		}
	}

	@Override
	public void error(Object step, Exception exception) {
		if (step instanceof String && (type == TrackType.STEP || type == TrackType.BOTH)) {
			tracker.track(
				runId,
				TrackType.STEP,
				stepInstanceIds.pop(), 
				stepInstanceIds.isEmpty() ? null : stepInstanceIds.peek(), 
				ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(), 
				steps.pop(), 
				started.pop(),
				new Date(), 
				exception
			);
			steps.pop();
		}
	}

	@Override
	public void after(Object step) {
		if (step instanceof String && (type == TrackType.STEP || type == TrackType.BOTH)) {
			tracker.track(
				runId,
				TrackType.STEP,
				stepInstanceIds.pop(), 
				stepInstanceIds.isEmpty() ? null : stepInstanceIds.peek(), 
				ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(), 
				steps.pop(), 
				started.pop(),
				new Date(), 
				null
			);
			steps.pop();
		}
	}

	@Override
	public void report(Object arg0) {
		// do nothing
	}

	public Stack<String> getServices() {
		return services;
	}

	public TrackType getType() {
		return type;
	}

	public void setType(TrackType type) {
		this.type = type;
	}

	public boolean isStopOnly() {
		return stopOnly;
	}

	public void setStopOnly(boolean stopOnly) {
		this.stopOnly = stopOnly;
	}
}
