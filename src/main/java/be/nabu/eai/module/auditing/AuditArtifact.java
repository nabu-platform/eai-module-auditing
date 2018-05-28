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
		if (getConfig().getServicesToAudit() != null && !getConfig().getServicesToAudit().isEmpty()) {
			boolean track = false;
			ServiceRuntime runtimeToCheck = runtime;
			findTracker: while (runtimeToCheck != null) {
				// if we meet a tracking service, the service is a child from the tracker itself, do not track
				if (POJOUtils.isImplementation(runtimeToCheck.getService(), trackInterface)) {
					track = false;
					break;
				}
				
				Service unwrapped = ServiceUtils.unwrap(runtimeToCheck.getService());
				
				// check unwrapped as well (see fixed value wrapping)
				if (POJOUtils.isImplementation(runtimeToCheck.getService(), trackInterface)) {
					track = false;
					break;
				}
				
				if (unwrapped instanceof DefinedService) {
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
					tracker.setTimeType(getConfig().getTrackTimeType());
					runtime.getContext().put("audit:" + getId(), tracker);
				}
				return tracker;
			}
		}
		return null;
	}

}
