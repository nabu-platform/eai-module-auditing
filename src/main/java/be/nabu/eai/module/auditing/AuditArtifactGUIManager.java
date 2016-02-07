package be.nabu.eai.module.auditing;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class AuditArtifactGUIManager extends BaseJAXBGUIManager<AuditConfiguration, AuditArtifact> {

	public AuditArtifactGUIManager() {
		super("Service Audit", AuditArtifact.class, new AuditArtifactManager(), AuditConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected AuditArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>...values) throws IOException {
		return new AuditArtifact(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@Override
	public String getCategory() {
		return "Miscellaneous";
	}
	
}
