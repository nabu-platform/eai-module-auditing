/*
* Copyright (C) 2015 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.auditing;

import java.util.ArrayList;
import java.util.HashMap;
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
	private TraceProfile profile;
	
	public DynamicRuntimeTracker() {
		trackInterface = MethodServiceInterface.wrap(FlatServiceTracker.class, "track");
	}
	
	public DynamicRuntimeTracker(TraceProfile profile) {
		this();
		this.profile = profile;
	}
	
	@Override
	public ServiceRuntimeTracker getTracker(ServiceRuntime runtime) {
		ServiceRuntime runtimeToCheck = runtime;
		MultipleServiceRuntimeTracker serviceTracker = null;
		
		String key = "dynamic-audit-" + profile.toString();
		// if we already have a tracker, we don't want to register a new one because this would mess up run ids etc
		// this is meant for long term auditing, so if you add an auditer _while_ the target is already running, too bad, you are missing that run
		// each tracker instance (e.g. logger, process, cdm,...) has one instance per service run. it can be enabled on multiple service if needed
		Map<String, FlatServiceTrackerWrapper> trackerMap = (Map<String, FlatServiceTrackerWrapper>) runtime.getContext().get(key);
		if (trackerMap == null) {
			trackerMap = new HashMap<String, FlatServiceTrackerWrapper>();
			runtime.getContext().put(key, trackerMap);
		}

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
			String serviceId = unwrap instanceof DefinedService ? ((DefinedService) unwrap).getId() : null;
			if (serviceId != null && trackers.containsKey(serviceId)) {
				List<DefinedService> list = trackers.get(serviceId);
				if (list != null && !list.isEmpty()) {
					shouldTrack = true;
					List<ServiceRuntimeTracker> tracker = new ArrayList<ServiceRuntimeTracker>();
					for (DefinedService single : list) {
						FlatServiceTrackerWrapper wrapper = trackerMap.get(single.getId());
						if (wrapper == null) {
							wrapper = new FlatServiceTrackerWrapper(single, runtime.getExecutionContext());
							wrapper.setIncludeServices(profile.isServices());
							wrapper.setIncludeSteps(profile.isSteps());
							wrapper.setIncludeReports(profile.isReports());
							wrapper.setRootServiceOnly(profile.isRootServiceOnly());
							wrapper.setTimeType(profile.getTrackTimeType() == null ? TrackTimeType.ALL : profile.getTrackTimeType());
							wrapper.setIncludeDescriptions(profile.isDescriptions());
							if (profile.getWhitelistedServices() != null) {
								wrapper.getWhitelistedServices().addAll(profile.getWhitelistedServices());
							}
							trackerMap.put(single.getId(), wrapper);
						}
						tracker.add(wrapper);
					}
					serviceTracker = new MultipleServiceRuntimeTracker(tracker.toArray(new ServiceRuntimeTracker[tracker.size()]));
				}
				break;
			}
			else if (!profile.isRecursive()) {
				break;
			}
			else {
				runtimeToCheck = runtimeToCheck.getParent();
			}
		}
		if (shouldTrack) {
			return serviceTracker;
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
	
}
