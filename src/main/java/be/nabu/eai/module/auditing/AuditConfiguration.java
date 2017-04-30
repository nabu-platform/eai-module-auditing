package be.nabu.eai.module.auditing;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackTimeType;
import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackType;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "audit")
public class AuditConfiguration {
	
	private List<DefinedService> servicesToAudit;
	private boolean recursive;
	private DefinedService auditingService;
	private TrackType trackType;
	private TrackTimeType trackTimeType;
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public List<DefinedService> getServicesToAudit() {
		return servicesToAudit;
	}
	public void setServicesToAudit(List<DefinedService> servicesToAudit) {
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

	public TrackType getTrackType() {
		return trackType;
	}
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

}
