package be.nabu.eai.module.auditing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackType;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.api.ServiceRuntimeTrackerProvider;

public class AuditArtifact extends JAXBArtifact<AuditConfiguration> implements ServiceRuntimeTrackerProvider {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public AuditArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "audit.xml", AuditConfiguration.class);
	}

	@Override
	public ServiceRuntimeTracker getTracker(ServiceRuntime runtime) {
		if (getConfig().getServicesToAudit() != null && getConfig().getServicesToAudit().isEmpty()) {
			try {
				boolean track = getConfiguration().getServicesToAudit().contains(runtime.getService());
				// if we want to track recursively
				if (!track && getConfiguration().getRecursive() != null && getConfiguration().getRecursive()) {
					ServiceRuntime runtimeToCheck = runtime.getParent();
					while (runtimeToCheck != null) {
						if (getConfiguration().getServicesToAudit().contains(runtime.getParent().getService())) {
							track = true;
							break;
						}
						runtimeToCheck = runtimeToCheck.getParent();
					}
				}
				if (track) {
					FlatServiceTrackerWrapper tracker = (FlatServiceTrackerWrapper) runtime.getContext().get("audit:" + getId());
					if (tracker == null) {
						tracker = new FlatServiceTrackerWrapper(getConfiguration().getAuditingService(), runtime.getExecutionContext());
						tracker.setType(getConfig().getTrackType() == null ? TrackType.SERVICE : getConfig().getTrackType());
						tracker.setStopOnly(getConfig().getStopOnly() != null && getConfig().getStopOnly());
						runtime.getContext().put("audit:" + getId(), tracker);
					}
					return tracker;
				}
			}
			catch (Exception e) {
				logger.error("Could not load auditing", e);
			}
		}
		return null;
	}

}
