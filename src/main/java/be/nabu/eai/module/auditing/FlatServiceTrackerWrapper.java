package be.nabu.eai.module.auditing;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Stack;
import java.util.UUID;

import be.nabu.eai.module.auditing.api.FlatServiceTracker;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackTimeType;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackType;
import be.nabu.libs.artifacts.api.ExternalDependencyArtifact;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.principals.DevicePrincipal;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.libs.types.java.BeanInstance;

// TODO: if we have other interesting steps that can not provide a correct toString(), we can add a concept of serializers
public class FlatServiceTrackerWrapper implements ServiceRuntimeTracker {

	private FlatServiceTracker tracker;
	private Stack<String> services = new Stack<String>();
	private Stack<String> steps = new Stack<String>();
	private UUID runId = UUID.randomUUID();
	private Stack<UUID> serviceInstanceIds = new Stack<UUID>();
	private Stack<UUID> stepInstanceIds = new Stack<UUID>();
	private Stack<Date> started = new Stack<Date>();
	private Stack<Long> sequences = new Stack<Long>();
	private TrackType type;
	private TrackTimeType timeType;
	private long sequence;
	private boolean includeReports, includeDescriptions, includeServices, includeSteps;
	
	public FlatServiceTrackerWrapper(Service service, ExecutionContext context) {
		// force an empty tracker to prevent recursive tracker calls
		this.tracker = POJOUtils.newProxy(FlatServiceTracker.class, service, context);
	}
	
	public FlatServiceTrackerWrapper(FlatServiceTracker tracker) {
		this.tracker = tracker;
	}
	
	private Device getDevice() {
		Token token = ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken();
		Device device = null;
		if (token != null && token instanceof DevicePrincipal) {
			device = ((DevicePrincipal) token).getDevice();
		}
		if (device == null && token != null && token.getCredentials() != null && !token.getCredentials().isEmpty()) {
			for (Principal credential : token.getCredentials()) {
				if (credential instanceof DevicePrincipal) {
					device = ((DevicePrincipal) credential).getDevice();
					if (device != null) {
						break;
					}
				}
			}
		}
		return device;
	}
	
	@Override
	public void start(Service service) {
		if (includeServices) {
			service = ServiceUtils.unwrap(service);
			if (service instanceof DefinedService) {
				UUID instanceId = UUID.randomUUID();
				services.push(((DefinedService) service).getId());
				started.push(new Date());
				serviceInstanceIds.push(instanceId);
				sequences.push(sequence++);
				if (timeType == TrackTimeType.BEFORE || timeType == TrackTimeType.ALL) {
					tracker.track(
						runId,
						TrackType.SERVICE, 
						instanceId,
						new ArrayList<UUID>(serviceInstanceIds),
						ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(),
						getDevice(),
						services.peek(),
						started.peek(),
						null,
						null,
						ServiceRuntime.getRuntime().getInput(),
						ServiceRuntime.getRuntime().getOutput(),
						ServiceRuntime.getRuntime().getCachedResult(),
						service instanceof ExternalDependencyArtifact ? ((ExternalDependencyArtifact) service).getExternalDependencies() : null,
						sequences.peek(),
						null,
						services.size() - 1,
						(String) ServiceRuntime.getRuntime().getContext().get("service.source")
					);
				}
			}
		}
	}
	
	@Override
	public void stop(Service service) {
		if (includeServices) {
			service = ServiceUtils.unwrap(service);
			if (!services.isEmpty() && service instanceof DefinedService) {
				if (!((DefinedService) service).getId().equals(services.peek())) {
					throw new RuntimeException("Service '" + ((DefinedService) service).getId() + "' does not match the stack: " + services);
				}
				if (timeType == TrackTimeType.AFTER || timeType == TrackTimeType.ALL) {
					Date serviceStarted = started.pop();
					Date serviceStopped = new Date();
					tracker.track(
						runId,
						TrackType.SERVICE,
						serviceInstanceIds.pop(), 
						new ArrayList<UUID>(serviceInstanceIds), 
						ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(), 
						getDevice(),
						services.pop(), 
						serviceStarted,
						serviceStopped, 
						null,
						ServiceRuntime.getRuntime().getInput(),
						ServiceRuntime.getRuntime().getOutput(),
						ServiceRuntime.getRuntime().getCachedResult(),
						service instanceof ExternalDependencyArtifact ? ((ExternalDependencyArtifact) service).getExternalDependencies() : null,
						sequences.pop(),
						serviceStopped.getTime() - serviceStarted.getTime(),
						services.size(),
						(String) ServiceRuntime.getRuntime().getContext().get("service.source")
					);
				}
				// do pop all the stacks
				else {
					services.pop();
					started.pop();
					serviceInstanceIds.pop();
				}
			}
		}
	}
	
	@Override
	public void error(Service service, Exception exception) {
		if (includeServices) {
			service = ServiceUtils.unwrap(service);
			if (!services.isEmpty() && service instanceof DefinedService) {
				if (!((DefinedService) service).getId().equals(services.peek())) {
					throw new RuntimeException("Service '" + ((DefinedService) service).getId() + "' does not match the stack: " + services);
				}
				Date serviceStarted = started.pop();
				Date serviceStopped = new Date();
				tracker.track(
					runId,
					TrackType.SERVICE,
					serviceInstanceIds.pop(), 
					new ArrayList<UUID>(serviceInstanceIds), 
					ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(),
					getDevice(),
					services.pop(), 
					serviceStarted,
					serviceStopped, 
					exception,
					ServiceRuntime.getRuntime().getInput(),
					ServiceRuntime.getRuntime().getOutput(),
					ServiceRuntime.getRuntime().getCachedResult(),
					service instanceof ExternalDependencyArtifact ? ((ExternalDependencyArtifact) service).getExternalDependencies() : null,
					sequences.pop(),
					serviceStopped.getTime() - serviceStarted.getTime(),
					services.size(),
					(String) ServiceRuntime.getRuntime().getContext().get("service.source")
				);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void before(Object step) {
		if (!(step instanceof String)) {
			BeanInstance beanInstance = new BeanInstance(step);
			step = beanInstance.get("name");
		}
		if (step != null && includeSteps) {
			UUID instanceId = UUID.randomUUID();
			steps.push((String) step);
			started.push(new Date());
			stepInstanceIds.push(instanceId);
			steps.push((String) step);
			if (timeType == TrackTimeType.BEFORE || timeType == TrackTimeType.ALL) {
				tracker.track(
					runId,
					TrackType.STEP, 
					instanceId,
					new ArrayList<UUID>(stepInstanceIds),
					ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(),
					getDevice(),
					steps.peek(),
					started.peek(),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					steps.size() - 1,
					(String) ServiceRuntime.getRuntime().getContext().get("service.source")
				);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void error(Object step, Exception exception) {
		if (!(step instanceof String)) {
			BeanInstance beanInstance = new BeanInstance(step);
			step = beanInstance.get("name");
		}
		if (step != null && includeSteps) {
			Date stepStarted = started.pop();
			Date stepStopped = new Date();
			tracker.track(
				runId,
				TrackType.STEP,
				stepInstanceIds.pop(), 
				new ArrayList<UUID>(stepInstanceIds), 
				ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(),
				getDevice(),
				steps.pop(), 
				stepStarted,
				stepStopped, 
				exception,
				null,
				null,
				null,
				null,
				null,
				stepStopped.getTime() - stepStarted.getTime(),
				steps.size(),
				(String) ServiceRuntime.getRuntime().getContext().get("service.source")
			);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void after(Object step) {
		if (!(step instanceof String)) {
			BeanInstance beanInstance = new BeanInstance(step);
			step = beanInstance.get("name");
		}
		if (step != null && includeSteps) {
			if (timeType == TrackTimeType.AFTER || timeType == TrackTimeType.ALL) {
				Date stepStarted = started.pop();
				Date stepStopped = new Date();
				tracker.track(
					runId,
					TrackType.STEP,
					stepInstanceIds.pop(), 
					new ArrayList<UUID>(stepInstanceIds), 
					ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(),
					getDevice(),
					steps.pop(), 
					stepStarted,
					stepStopped, 
					null,
					null,
					null,
					null,
					null,
					null,
					stepStopped.getTime() - stepStarted.getTime(),
					steps.size(),
					(String) ServiceRuntime.getRuntime().getContext().get("service.source")
				);
			}
		}
	}

	@Override
	public void report(Object report) {
		if (report != null && includeReports) {
			tracker.track(
				runId,
				TrackType.REPORT,
				serviceInstanceIds.peek(), 
				new ArrayList<UUID>(serviceInstanceIds), 
				ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(),
				getDevice(),
				services.peek(), 
				started.peek(),
				null, 
				null,
				report,
				null,
				null,
				null,
				sequences.peek(),
				null,
				services.size(),
				(String) ServiceRuntime.getRuntime().getContext().get("service.source")
			);
		}
	}
	
	@Override
	public void describe(Object description) {
		if (description != null && includeDescriptions) {
			tracker.track(
				runId,
				TrackType.DESCRIPTION,
				serviceInstanceIds.peek(), 
				new ArrayList<UUID>(serviceInstanceIds), 
				ServiceRuntime.getRuntime().getExecutionContext().getSecurityContext().getToken(),
				getDevice(),
				services.peek(),
				started.peek(),
				null, 
				null,
				description,
				null,
				null,
				null,
				sequences.peek(),
				null,
				services.size(),
				(String) ServiceRuntime.getRuntime().getContext().get("service.source")
			);
		}
	}

	public Stack<String> getServices() {
		return services;
	}

	@Deprecated
	public TrackType getType() {
		return type;
	}
	@Deprecated
	public void setType(TrackType type) {
		this.type = type;
		if (TrackType.SERVICE.equals(type)) {
			includeServices = true;
		}
		else if (TrackType.STEP.equals(type)) {
			includeSteps = true;
		}
		else if (TrackType.BOTH.equals(type)) {
			includeServices = true;
			includeSteps = true;
		}
	}

	public TrackTimeType getTimeType() {
		return timeType;
	}

	public void setTimeType(TrackTimeType timeType) {
		this.timeType = timeType;
	}

	public boolean isIncludeReports() {
		return includeReports;
	}

	public void setIncludeReports(boolean includeReports) {
		this.includeReports = includeReports;
	}

	public boolean isIncludeDescriptions() {
		return includeDescriptions;
	}

	public void setIncludeDescriptions(boolean includeDescriptions) {
		this.includeDescriptions = includeDescriptions;
	}

	public boolean isIncludeServices() {
		return includeServices;
	}

	public void setIncludeServices(boolean includeServices) {
		this.includeServices = includeServices;
	}

	public boolean isIncludeSteps() {
		return includeSteps;
	}

	public void setIncludeSteps(boolean includeSteps) {
		this.includeSteps = includeSteps;
	}
	
}
