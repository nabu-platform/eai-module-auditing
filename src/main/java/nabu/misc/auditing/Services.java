package nabu.misc.auditing;

import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import be.nabu.eai.module.auditing.DynamicRuntimeTracker;
import be.nabu.eai.module.auditing.FlatServiceTrackerWrapper;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackType;
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
	
	private ExecutionContext executionContext;
	private static DynamicRuntimeTracker instance;
	
	/**
	 * Allows you to register a service that performs service tracking, it must implement the interface nabu.interfaces.Services.track
	 */
	public void registerServiceTracker(@WebParam(name = "serviceId") String serviceId, @WebParam(name = "trackType") TrackType trackType, @WebParam(name = "recursive") Boolean recursive) {
		DefinedService resolved = executionContext.getServiceContext().getResolver(DefinedService.class).resolve(serviceId);
		if (executionContext.getServiceContext().getServiceTrackerProvider() instanceof ModifiableServiceRuntimeTrackerProvider) {
			FlatServiceTrackerWrapper runtimeTracker = new FlatServiceTrackerWrapper(resolved, executionContext);
			runtimeTracker.setType(trackType == null ? TrackType.SERVICE : trackType);
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
	
	public void auditService(@WebParam(name = "serviceId") String serviceId, @WebParam(name = "auditServiceId") String auditServiceId) {
		Artifact serviceToTrack = EAIResourceRepository.getInstance().resolve(serviceId);
		Artifact auditService = EAIResourceRepository.getInstance().resolve(auditServiceId);
		if (!(serviceToTrack instanceof DefinedService)
				|| !(auditService instanceof DefinedService)
				|| !POJOUtils.isImplementation((DefinedService) auditService, MethodServiceInterface.wrap(be.nabu.eai.module.auditing.api.FlatServiceTracker.class, "track"))) {
			throw new IllegalArgumentException("Invalid input, can not track " + serviceId + " with " + auditServiceId);
		}
		if (instance == null) {
			synchronized(Services.class) {
				if (instance == null) {
					DynamicRuntimeTracker dynamicRuntimeTracker = new DynamicRuntimeTracker();
					// set up a new service track provider
					((EAIResourceRepository) EAIResourceRepository.getInstance()).getDynamicRuntimeTrackers().add(dynamicRuntimeTracker);
					instance = dynamicRuntimeTracker;
				}
			}
		}
		instance.auditService(serviceId, (DefinedService) auditService);
	}
	
	public void unauditService(@WebParam(name = "serviceId") String serviceId) {
		if (instance != null) {
			instance.unauditService(serviceId);
		}
	}
	
	@WebResult(name = "auditedServices")
	// list the services that are currently being audited
	public List<String> listAudited() {
		return instance == null ? null : instance.getAudited();
	}
}
