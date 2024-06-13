package be.nabu.eai.module.auditing;

import java.util.List;

import be.nabu.eai.module.auditing.api.FlatServiceTracker.TrackTimeType;

public class TraceProfile {
	
	public static TraceProfile getDefault(boolean recursive) {
		TraceProfile traceProfile = new TraceProfile();
		traceProfile.setRecursive(recursive);
		traceProfile.setServices(true);
		traceProfile.setTrackTimeType(TrackTimeType.ALL);
		return traceProfile;
	}
	
	private boolean recursive;
	private boolean services, descriptions, reports, steps, rootServiceOnly;
	private List<String> whitelistedServices;
	private TrackTimeType trackTimeType;
	
	public boolean isServices() {
		return services;
	}
	public void setServices(boolean services) {
		this.services = services;
	}
	public boolean isDescriptions() {
		return descriptions;
	}
	public void setDescriptions(boolean descriptions) {
		this.descriptions = descriptions;
	}
	public boolean isReports() {
		return reports;
	}
	public void setReports(boolean reports) {
		this.reports = reports;
	}
	public boolean isSteps() {
		return steps;
	}
	public void setSteps(boolean steps) {
		this.steps = steps;
	}
	public List<String> getWhitelistedServices() {
		return whitelistedServices;
	}
	public void setWhitelistedServices(List<String> whitelistedServices) {
		this.whitelistedServices = whitelistedServices;
	}
	
	public boolean isRecursive() {
		return recursive;
	}
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}
	@Override
	public String toString() {
		return "TraceProfile [recursive= " + recursive + ", trackTimeType=" + trackTimeType + ", services=" + services + ", rootServiceOnly=" + rootServiceOnly + ", descriptions=" + descriptions + ", reports=" + reports
				+ ", steps=" + steps + ", whitelistedServices=" + whitelistedServices + "]";
	}
	public TrackTimeType getTrackTimeType() {
		return trackTimeType;
	}
	public void setTrackTimeType(TrackTimeType trackTimeType) {
		this.trackTimeType = trackTimeType;
	}
	public boolean isRootServiceOnly() {
		return rootServiceOnly;
	}
	public void setRootServiceOnly(boolean rootServiceOnly) {
		this.rootServiceOnly = rootServiceOnly;
	}
}
