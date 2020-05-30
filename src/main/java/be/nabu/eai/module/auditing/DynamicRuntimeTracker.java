package be.nabu.eai.module.auditing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import be.nabu.eai.module.auditing.api.FlatServiceTracker;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackTimeType;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackType;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.api.ServiceRuntimeTrackerProvider;
import be.nabu.libs.services.pojo.MethodServiceInterface;
import be.nabu.libs.services.pojo.POJOUtils;

public class DynamicRuntimeTracker implements ServiceRuntimeTrackerProvider {

	private Map<String, DefinedService> trackers = new ConcurrentHashMap<String, DefinedService>();
	private MethodServiceInterface trackInterface;
	
	public DynamicRuntimeTracker() {
		trackInterface = MethodServiceInterface.wrap(FlatServiceTracker.class, "track");
	}
	
	@Override
	public ServiceRuntimeTracker getTracker(ServiceRuntime runtime) {
		boolean track = false;
		ServiceRuntime runtimeToCheck = runtime;
		DefinedService serviceTracker = null;
		while (runtimeToCheck != null) {
			// if we meet a tracking service, the service is a child from the tracker itself, do not track
			if (POJOUtils.isImplementation(runtimeToCheck.getService(), trackInterface)) {
				track = false;
				break;
			}
			Service unwrap = ServiceUtils.unwrap(runtimeToCheck.getService());
			// check unwrapped as well (see fixed value wrapping)
			if (POJOUtils.isImplementation(runtimeToCheck.getService(), trackInterface)) {
				track = false;
				break;
			}
			if (unwrap instanceof DefinedService && trackers.containsKey(((DefinedService) unwrap).getId())) {
				track = true;
				serviceTracker = trackers.get(((DefinedService) unwrap).getId());
				break;
			}
			else {
				runtimeToCheck = runtimeToCheck.getParent();
			}
		}
		if (track) {
			// check if we already have a tracker, share it across all? this makes it harder to have multiple different types of trackers though...
			FlatServiceTrackerWrapper tracker = (FlatServiceTrackerWrapper) runtime.getContext().get("dynamic-audit");
			if (tracker == null) {
				tracker = new FlatServiceTrackerWrapper(serviceTracker, runtime.getExecutionContext());
				tracker.setType(TrackType.SERVICE);
				tracker.setTimeType(TrackTimeType.ALL);
				runtime.getContext().put("dynamic-audit", tracker);
			}
			return tracker;
		}
		return null;
	}

	public void auditService(String serviceId, DefinedService trackerService) {
		trackers.put(serviceId, trackerService);
	}
	
	public void unauditService(String serviceId) {
		trackers.remove(serviceId);
	}
	
	public List<String> getAudited() {
		return new ArrayList<String>(trackers.keySet());
	}
}
