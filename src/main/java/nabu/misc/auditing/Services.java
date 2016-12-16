package nabu.misc.auditing;

import javax.jws.WebParam;
import javax.jws.WebService;

import be.nabu.eai.module.auditing.FlatServiceTrackerWrapper;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackType;
import be.nabu.eai.repository.api.ModifiableServiceRuntimeTrackerProvider;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;

@WebService
public class Services {
	
	private ExecutionContext executionContext;
	
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
}
