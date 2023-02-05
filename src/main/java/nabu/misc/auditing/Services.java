package nabu.misc.auditing;

import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import be.nabu.eai.module.auditing.DynamicRuntimeTracker;
import be.nabu.eai.module.auditing.FlatServiceTrackerWrapper;
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
	private static DynamicRuntimeTracker nonRecursiveInstance;
	
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
	
	public void auditService(@NotNull @WebParam(name = "serviceId") String serviceId, @NotNull @WebParam(name = "auditServiceId") String auditServiceId, @WebParam(name = "recursive") Boolean recursive) {
		Artifact serviceToTrack = EAIResourceRepository.getInstance().resolve(serviceId);
		Artifact auditService = EAIResourceRepository.getInstance().resolve(auditServiceId);
		if (!(serviceToTrack instanceof DefinedService)
				|| !(auditService instanceof DefinedService)
				|| !POJOUtils.isImplementation((DefinedService) auditService, MethodServiceInterface.wrap(be.nabu.eai.module.auditing.api.FlatServiceTracker.class, "track"))) {
			throw new IllegalArgumentException("Invalid input, can not track " + serviceId + " with " + auditServiceId);
		}
		if (recursive == null || recursive) {
			if (instance == null) {
				synchronized(Services.class) {
					if (instance == null) {
						DynamicRuntimeTracker dynamicRuntimeTracker = new DynamicRuntimeTracker();
						dynamicRuntimeTracker.setRecursive(true);
						// set up a new service track provider
						((EAIResourceRepository) EAIResourceRepository.getInstance()).getDynamicRuntimeTrackers().add(dynamicRuntimeTracker);
						instance = dynamicRuntimeTracker;
					}
				}
			}
			instance.auditService(serviceId, (DefinedService) auditService);
		}
		else {
			if (nonRecursiveInstance == null) {
				synchronized(Services.class) {
					if (nonRecursiveInstance == null) {
						DynamicRuntimeTracker dynamicRuntimeTracker = new DynamicRuntimeTracker();
						dynamicRuntimeTracker.setRecursive(false);
						// set up a new service track provider
						((EAIResourceRepository) EAIResourceRepository.getInstance()).getDynamicRuntimeTrackers().add(dynamicRuntimeTracker);
						nonRecursiveInstance = dynamicRuntimeTracker;
					}
				}
			}
			nonRecursiveInstance.auditService(serviceId, (DefinedService) auditService);
		}
	}
	
	public void unauditService(@NotNull @WebParam(name = "serviceId") String serviceId, @WebParam(name = "auditServiceId") String auditServiceId, @WebParam(name = "recursive") Boolean recursive) {
		if ((recursive == null || recursive) && instance != null) {
			if (auditServiceId == null) {
				instance.unauditService(serviceId);
			}
			else {
				instance.unauditService(serviceId, (DefinedService) EAIResourceRepository.getInstance().resolve(auditServiceId));
			}
		}
		else if (recursive != null && !recursive && nonRecursiveInstance != null) {
			if (auditServiceId == null) {
				nonRecursiveInstance.unauditService(serviceId);
			}
			else {
				nonRecursiveInstance.unauditService(serviceId, (DefinedService) EAIResourceRepository.getInstance().resolve(auditServiceId));
			}
		}
	}
	
	@WebResult(name = "auditedServices")
	// list the services that are currently being audited
	public List<String> listAudited(@WebParam(name = "auditServiceId") String auditServiceId, @WebParam(name = "recursive") Boolean recursive) {
		if (recursive == null || recursive) {
			if (instance == null) {
				return null;
			}
			return instance.getAudited(auditServiceId);
		}
		else {
			if (nonRecursiveInstance == null) {
				return null;
			}
			return nonRecursiveInstance.getAudited(auditServiceId);
		}
	}
}
