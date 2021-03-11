package be.nabu.eai.module.auditing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackTimeType;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackType;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.eai.repository.util.KeyValueMapAdapter;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.annotation.Field;

@XmlRootElement(name = "audit")
public class AuditConfiguration {
	
	private List<String> servicesToAudit;
	private boolean recursive, auditAll, checkServiceContext;
	private DefinedService auditingService;
	private TrackType trackType;
	private TrackTimeType trackTimeType;
	private Map<String, String> properties;
	private Boolean includeReports, includeDescriptions, includeServices, includeSteps;
	
	@Field(hide = "auditAll")
	public List<String> getServicesToAudit() {
		return servicesToAudit;
	}
	public void setServicesToAudit(List<String> servicesToAudit) {
		this.servicesToAudit = servicesToAudit;
	}
	
	@InterfaceFilter(implement = "be.nabu.eai.module.auditing.api.FlatServiceTracker.track")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getAuditingService() {
		return auditingService;
	}
	public void setAuditingService(DefinedService auditingService) {
		this.auditingService = auditingService;
	}
	
	public boolean isRecursive() {
		return recursive;
	}
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	@Field(hide = "true")
	@Deprecated
	public TrackType getTrackType() {
		return trackType;
	}
	@Deprecated
	public void setTrackType(TrackType trackType) {
		this.trackType = trackType;
	}
	
	public TrackTimeType getTrackTimeType() {
		if (trackTimeType == null) {
			trackTimeType = TrackTimeType.ALL;
		}
		return trackTimeType;
	}
	public void setTrackTimeType(TrackTimeType trackTimeType) {
		this.trackTimeType = trackTimeType;
	}

	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = KeyValueMapAdapter.class)
	public Map<String, String> getProperties() {
		if (properties == null) {
			properties = new LinkedHashMap<String, String>();
		}
		return properties;
	}
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	@Field(hide = "servicesToAudit != null")
	public boolean isAuditAll() {
		return auditAll;
	}
	public void setAuditAll(boolean auditAll) {
		this.auditAll = auditAll;
	}
	public boolean isCheckServiceContext() {
		return checkServiceContext;
	}
	public void setCheckServiceContext(boolean checkServiceContext) {
		this.checkServiceContext = checkServiceContext;
	}
	public Boolean getIncludeReports() {
		return includeReports;
	}
	public void setIncludeReports(Boolean includeReports) {
		this.includeReports = includeReports;
	}
	public Boolean getIncludeDescriptions() {
		return includeDescriptions;
	}
	public void setIncludeDescriptions(Boolean includeDescriptions) {
		this.includeDescriptions = includeDescriptions;
	}
	public Boolean getIncludeServices() {
		return includeServices;
	}
	public void setIncludeServices(Boolean includeServices) {
		this.includeServices = includeServices;
	}
	public Boolean getIncludeSteps() {
		return includeSteps;
	}
	public void setIncludeSteps(Boolean includeSteps) {
		this.includeSteps = includeSteps;
	}
}
