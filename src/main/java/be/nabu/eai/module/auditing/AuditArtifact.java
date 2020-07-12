package be.nabu.eai.module.auditing;

import java.util.Map;

import be.nabu.eai.module.auditing.api.FlatServiceTracker;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackType;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.api.ServiceRuntimeTrackerProvider;
import be.nabu.libs.services.fixed.FixedInputService;
import be.nabu.libs.services.pojo.MethodServiceInterface;
import be.nabu.libs.services.pojo.POJOUtils;

public class AuditArtifact extends JAXBArtifact<AuditConfiguration> implements ServiceRuntimeTrackerProvider {

	private MethodServiceInterface trackInterface;
	
	public AuditArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "audit.xml", AuditConfiguration.class);
		trackInterface = MethodServiceInterface.wrap(FlatServiceTracker.class, "track");
	}

	@Override
	public ServiceRuntimeTracker getTracker(ServiceRuntime runtime) {
		// if we have disabled auditing for this context, don't set a tracker
		Object object = runtime.getContext().get("audit.disabled");
		if (object instanceof Boolean && (Boolean) object) {
			return null;
		}
		boolean track = false;
		if (getConfig().isAuditAll()) {
			// if we want to track everything, just set that
			if (getConfig().isRecursive()) {
				track = true;
			}
			// otherwise, we only mean root services
			else {
				track = runtime.getParent() == null;
			}
		}
		// make sure we are not tracking the tracker
		ServiceRuntime runtimeToCheck = runtime;
		boolean isTracker = false;
		findTracker: while (runtimeToCheck != null) {
			// if we meet a tracking service, the service is a child from the tracker itself, do not track
			if (POJOUtils.isImplementation(runtimeToCheck.getService(), trackInterface)) {
				track = false;
				isTracker = true;
				break;
			}
			
			Service unwrapped = ServiceUtils.unwrap(runtimeToCheck.getService());
			
			// check unwrapped as well (see fixed value wrapping)
			if (POJOUtils.isImplementation(runtimeToCheck.getService(), trackInterface)) {
				track = false;
				isTracker = true;
				break;
			}
			
			if (unwrapped instanceof DefinedService && track == false && getConfig().getServicesToAudit() != null && !getConfig().getServicesToAudit().isEmpty()) {
				String id = ((DefinedService) unwrapped).getId();
				for (String entry : getConfig().getServicesToAudit()) {
					// if we match the id specifically
					if (entry.equals(id) 
							// or we match the folder and there is no parent service
							|| (id.startsWith(entry + ".") && runtimeToCheck.getParent() == null)
							// or we match the folder and there is a parent service _and_ we have set recursive
							|| (id.startsWith(entry + ".") && getConfig().isRecursive())) {
						track = true;
						break findTracker;
					}
				}
			}
			// if we don't have recursive toggled, we don't check recursively
			if (!getConfig().isRecursive()) {
				break;
			}
			runtimeToCheck = runtimeToCheck.getParent();
		}
		// if we aren't in a tracker and we are still not tracking, check the service context
		// only valid if we have recursive turned on
		if (getConfig().isCheckServiceContext() && getConfig().isRecursive() && !isTracker && !track && getConfig().getServicesToAudit() != null && !getConfig().getServicesToAudit().isEmpty()) {
			String serviceContext = ServiceUtils.getServiceContext(runtime);
			for (String entry : getConfig().getServicesToAudit()) {
				if (serviceContext.equals(entry) || serviceContext.startsWith(entry + ".")) {
					track = true;
					break;
				}
			}
		}
		if (track) {
			FlatServiceTrackerWrapper tracker = (FlatServiceTrackerWrapper) runtime.getContext().get("audit:" + getId());
			if (tracker == null) {
				Map<String, String> properties = getConfig().getProperties();
				if (properties != null && !properties.isEmpty()) {
					FixedInputService service = new FixedInputService(getConfig().getAuditingService());
					for (String key : properties.keySet()) {
						service.setInput(key, properties.get(key));
					}
					tracker = new FlatServiceTrackerWrapper(service, runtime.getExecutionContext());
					
				}
				else {
					tracker = new FlatServiceTrackerWrapper(getConfig().getAuditingService(), runtime.getExecutionContext());
				}
				tracker.setType(getConfig().getTrackType() == null ? TrackType.SERVICE : getConfig().getTrackType());
				if (getConfig().getIncludeDescriptions() != null) {
					tracker.setIncludeDescriptions(getConfig().getIncludeDescriptions());
				}
				if (getConfig().getIncludeReports() != null) {
					tracker.setIncludeReports(getConfig().getIncludeReports());
				}
				if (getConfig().getIncludeServices() != null) {
					tracker.setIncludeServices(getConfig().getIncludeServices());
				}
				if (getConfig().getIncludeSteps() != null) {
					tracker.setIncludeSteps(getConfig().getIncludeSteps());
				}
				tracker.setTimeType(getConfig().getTrackTimeType());
				runtime.getContext().put("audit:" + getId(), tracker);
			}
			return tracker;
		}
		return null;
	}

}
