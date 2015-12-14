package be.nabu.eai.module.auditing;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class AuditArtifactManager extends JAXBArtifactManager<AuditConfiguration, AuditArtifact> {

	public AuditArtifactManager() {
		super(AuditArtifact.class);
	}

	@Override
	protected AuditArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new AuditArtifact(id, container, repository);
	}

}
