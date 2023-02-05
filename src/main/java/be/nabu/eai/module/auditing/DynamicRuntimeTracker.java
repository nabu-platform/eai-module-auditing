package be.nabu.eai.module.auditing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import be.nabu.eai.module.auditing.api.FlatServiceTracker;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackTimeType;
import be.nabu.libs.services.MultipleServiceRuntimeTracker;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.api.ServiceRuntimeTrackerProvider;
import be.nabu.libs.services.pojo.MethodServiceInterface;
import be.nabu.libs.services.pojo.POJOUtils;

public class DynamicRuntimeTracker implements ServiceRuntimeTrackerProvider {

	private Map<String, List<DefinedService>> trackers = new ConcurrentHashMap<String, List<DefinedService>>();
	private MethodServiceInterface trackInterface;
	private boolean recursive;
	
	public DynamicRuntimeTracker() {
		trackInterface = MethodServiceInterface.wrap(FlatServiceTracker.class, "track");
	}
	
	@Override
	public ServiceRuntimeTracker getTracker(ServiceRuntime runtime) {
		ServiceRuntime runtimeToCheck = runtime;
		MultipleServiceRuntimeTracker serviceTracker = null;
		
		String key = "dynamic-audit-" + (recursive ? "recursive" : "non-recursive");
		// if we already have a tracker, we don't want to register a new one because this would mess up run ids etc
		// this is meant for long term auditing, so if you add an auditer _while_ the target is already running, too bad, you are missing that run
		MultipleServiceRuntimeTracker currentTracker = (MultipleServiceRuntimeTracker) runtime.getContext().get(key);
		boolean shouldTrack = false;
		
		while (runtimeToCheck != null) {
			// if we meet a tracking service, the service is a child from the tracker itself, do not track
			if (POJOUtils.isImplementation(runtimeToCheck.getService(), trackInterface)) {
				break;
			}
			Service unwrap = ServiceUtils.unwrap(runtimeToCheck.getService());
			// check unwrapped as well (see fixed value wrapping)
			if (POJOUtils.isImplementation(runtimeToCheck.getService(), trackInterface)) {
				break;
			}
			if (unwrap instanceof DefinedService && trackers.containsKey(((DefinedService) unwrap).getId())) {
				List<DefinedService> list = trackers.get(((DefinedService) unwrap).getId());
				if (list != null && !list.isEmpty()) {
					shouldTrack = true;
					// only go through the trouble of constructing an actual tracker if we don't have one yet
					if (currentTracker == null) {
						List<ServiceRuntimeTracker> tracker = new ArrayList<ServiceRuntimeTracker>();
						for (DefinedService single : list) {
							FlatServiceTrackerWrapper wrapper = new FlatServiceTrackerWrapper(single, runtime.getExecutionContext());
							wrapper.setIncludeServices(true);
							wrapper.setIncludeSteps(false);
							wrapper.setIncludeReports(false);
							wrapper.setTimeType(TrackTimeType.ALL);
							wrapper.setIncludeDescriptions(true);
							tracker.add(wrapper);
						}
						serviceTracker = new MultipleServiceRuntimeTracker(tracker.toArray(new ServiceRuntimeTracker[tracker.size()]));
					}
				}
				break;
			}
			else if (!recursive) {
				break;
			}
			else {
				runtimeToCheck = runtimeToCheck.getParent();
			}
		}
		if (shouldTrack) {
			if (currentTracker == null) {
				runtime.getContext().put(key, serviceTracker);
				return serviceTracker;
			}
			else {
				return currentTracker;
			}
		}
		return null;
	}

	public void auditService(String serviceId, DefinedService trackerService) {
		if (!trackers.containsKey(serviceId)) {
			trackers.put(serviceId, new ArrayList<DefinedService>());
		}
		boolean found = false;
		// we only want one instance for each combo
		for (DefinedService single : trackers.get(serviceId)) {
			if (single.getId().equals(trackerService.getId())) {
				found = true;
				break;
			}
		}
		if (!found) {
			trackers.get(serviceId).add(trackerService);
		}
	}
	
	public void unauditService(String serviceId) {
		trackers.remove(serviceId);
	}
	
	public void unauditService(String serviceId, DefinedService trackerService) {
		List<DefinedService> list = trackers.get(serviceId);
		if (list != null) {
			for (DefinedService single : trackers.get(serviceId)) {
				if (single.getId().equals(trackerService.getId())) {
					list.remove(single);
					break;
				}
			}
		}
		if (list != null && list.isEmpty()) {
			trackers.remove(serviceId);
		}
	}
	
	public List<String> getAudited(String auditServiceId) {
		if (auditServiceId == null) {
			return new ArrayList<String>(trackers.keySet());
		}
		else {
			ArrayList<String> result = new ArrayList<String>();
			for (Map.Entry<String, List<DefinedService>> entry : trackers.entrySet()) {
				List<DefinedService> value = entry.getValue();
				if (value != null) {
					for (DefinedService service : value) {
						if (service.getId().equals(auditServiceId)) {
							result.add(entry.getKey());
							break;
						}
					}
				}
			}
			return result;
		}
	}

	public boolean isRecursive() {
		return recursive;
	}

	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}
	
}
