package be.nabu.eai.module.auditing;

import be.nabu.eai.module.auditing.api.FlatServiceTracker;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackType;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.api.ServiceRuntimeTrackerProvider;
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
			while (runtimeToCheck != null) {
				// if we meet a tracking service, the service is a child from the tracker itself, do not track
				if (POJOUtils.isImplementation(runtimeToCheck.getService(), trackInterface)) {
					track = false;
					break;
				}
				else if (getConfig().getServicesToAudit().contains(ServiceUtils.unwrap(runtimeToCheck.getService()))) {
					track = true;
				}
				runtimeToCheck = runtimeToCheck.getParent();
				if (!getConfig().isRecursive()) {
					break;
				}
			}
			if (track) {
				FlatServiceTrackerWrapper tracker = (FlatServiceTrackerWrapper) runtime.getContext().get("audit:" + getId());
				if (tracker == null) {
					tracker = new FlatServiceTrackerWrapper(getConfig().getAuditingService(), runtime.getExecutionContext());
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
