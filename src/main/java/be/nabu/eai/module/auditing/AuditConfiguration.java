package be.nabu.eai.module.auditing;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "audit")
public class AuditConfiguration {
	
	private List<DefinedService> servicesToAudit;
	private DefinedService auditingService;
	private Boolean recursive, servicesOnly;
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public List<DefinedService> getServicesToAudit() {
		return servicesToAudit;
	}
	public void setServicesToAudit(List<DefinedService> servicesToAudit) {
		this.servicesToAudit = servicesToAudit;
	}
	
	@InterfaceFilter(implement = "be.nabu.eai.repository.api.FlatServiceTracker.track")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getAuditingService() {
		return auditingService;
	}
	public void setAuditingService(DefinedService auditingService) {
		this.auditingService = auditingService;
	}
	
	public Boolean getRecursive() {
		return recursive;
	}
	public void setRecursive(Boolean recursive) {
		this.recursive = recursive;
	}
	
	public Boolean getServicesOnly() {
		return servicesOnly;
	}
	public void setServicesOnly(Boolean servicesOnly) {
		this.servicesOnly = servicesOnly;
	}
}
