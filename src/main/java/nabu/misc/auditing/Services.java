package nabu.misc.auditing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import be.nabu.eai.module.auditing.DynamicRuntimeTracker;
import be.nabu.eai.module.auditing.FlatServiceTrackerWrapper;
import be.nabu.eai.module.auditing.TraceProfile;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.ModifiableServiceRuntimeTrackerProvider;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.pojo.MethodServiceInterface;
import be.nabu.libs.services.pojo.POJOUtils;

@WebService
public class Services {
	
	public enum TrackMode {
		SERVICE,
		ROOT_SERVICE,
		DESCRIPTION
	}
	
	private ExecutionContext executionContext;
	
	private static Map<String, DynamicRuntimeTracker> trackers = new HashMap<String, DynamicRuntimeTracker>();
	
	/**
	 * Allows you to register a service that performs service tracking, it must implement the interface nabu.interfaces.Services.track
	 */
	public void registerServiceTracker(@WebParam(name = "serviceId") String serviceId, @WebParam(name = "recursive") Boolean recursive, @WebParam(name = "includeServices") Boolean includeServices, @WebParam(name = "includeSteps") Boolean includeSteps, @WebParam(name = "includeReports") Boolean includeReports, @WebParam(name = "includeDescriptions") Boolean includeDescriptions) {
		DefinedService resolved = executionContext.getServiceContext().getResolver(DefinedService.class).resolve(serviceId);
		if (executionContext.getServiceContext().getServiceTrackerProvider() instanceof ModifiableServiceRuntimeTrackerProvider) {
			FlatServiceTrackerWrapper runtimeTracker = new FlatServiceTrackerWrapper(resolved, executionContext);
			runtimeTracker.setIncludeDescriptions(includeDescriptions);
			runtimeTracker.setIncludeReports(includeReports);
			runtimeTracker.setIncludeServices(includeServices);
			runtimeTracker.setIncludeSteps(includeSteps);
			((ModifiableServiceRuntimeTrackerProvider) executionContext.getServiceContext().getServiceTrackerProvider()).addTracker(
				ServiceRuntime.getRuntime().getService(), 
				runtimeTracker, 
				recursive == null ? false : recursive
			);
		}
		else {
			throw new IllegalStateException("The current execution context does not allow addition of custom service trackers");
		}
	}
	
	private DynamicRuntimeTracker getTracker(TraceProfile profile, boolean create) {
		String key = profile.toString();
		DynamicRuntimeTracker dynamicRuntimeTracker = trackers.get(key);
		if (dynamicRuntimeTracker == null && create) {
			synchronized(Services.class) {
				dynamicRuntimeTracker = trackers.get(key);
				if (dynamicRuntimeTracker == null) {
					dynamicRuntimeTracker = new DynamicRuntimeTracker(profile);
					trackers.put(key, dynamicRuntimeTracker);
					((EAIResourceRepository) EAIResourceRepository.getInstance()).getDynamicRuntimeTrackers().add(dynamicRuntimeTracker);
				}
			}
		}
		return dynamicRuntimeTracker;
	}
	
	public void auditService(@NotNull @WebParam(name = "serviceId") String serviceId, @NotNull @WebParam(name = "auditServiceId") String auditServiceId, @WebParam(name = "recursive") Boolean recursive, @WebParam(name = "traceProfile") TraceProfile profile) {
		if (recursive != null && profile != null) {
			throw new IllegalArgumentException("Use either recursive or the profile, not a combination of both. Recursive is deprecated");
		}
		
		Artifact serviceToTrack = EAIResourceRepository.getInstance().resolve(serviceId);
		Artifact auditService = EAIResourceRepository.getInstance().resolve(auditServiceId);
		if (!(serviceToTrack instanceof DefinedService)
				|| !(auditService instanceof DefinedService)
				|| !POJOUtils.isImplementation((DefinedService) auditService, MethodServiceInterface.wrap(be.nabu.eai.module.auditing.api.FlatServiceTracker.class, "track"))) {
			throw new IllegalArgumentException("Invalid input, can not track " + serviceId + " with " + auditServiceId);
		}
		DynamicRuntimeTracker tracker = getTracker(profile == null ? TraceProfile.getDefault(recursive == null || recursive) : profile, true);
		tracker.auditService(serviceId, (DefinedService) auditService);
	}
	
	public void unauditService(@NotNull @WebParam(name = "serviceId") String serviceId, @WebParam(name = "auditServiceId") String auditServiceId, @WebParam(name = "recursive") Boolean recursive, @WebParam(name = "traceProfile") TraceProfile profile) {
		DynamicRuntimeTracker tracker = getTracker(profile == null ? TraceProfile.getDefault(recursive == null || recursive) : profile, false);
		
		if (tracker != null) {
			if (auditServiceId == null) {
				tracker.unauditService(serviceId);
			}
			else {
				tracker.unauditService(serviceId, (DefinedService) EAIResourceRepository.getInstance().resolve(auditServiceId));
			}
		}
	}
	
	@WebResult(name = "auditedServices")
	// list the services that are currently being audited
	public List<String> listAudited(@WebParam(name = "auditServiceId") String auditServiceId, @WebParam(name = "recursive") Boolean recursive, @WebParam(name = "traceProfile") TraceProfile profile) {
		DynamicRuntimeTracker tracker = getTracker(profile == null ? TraceProfile.getDefault(recursive == null || recursive) : profile, false);
		if (tracker != null) {
			return tracker.getAudited(auditServiceId);
		}
		return null;
	}
}
