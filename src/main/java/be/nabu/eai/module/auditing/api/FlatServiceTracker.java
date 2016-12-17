package be.nabu.eai.module.auditing.api;

import java.util.Date;
import java.util.UUID;

import javax.jws.WebParam;
import javax.validation.constraints.NotNull;

import be.nabu.libs.authentication.api.Token;

public interface FlatServiceTracker {
	public void track(
		// the id of the entire run (root id)
		@NotNull @WebParam(name = "runId") UUID id,
		// the type of the track entry
		@NotNull @WebParam(name = "type") TrackType trackType,
		// the id of this service instance
		@NotNull @WebParam(name = "instanceId") UUID instanceId,
		// the id of the parent service instance
		@WebParam(name = "parentId") UUID parentId,
		@WebParam(name = "token") Token token,
		@NotNull @WebParam(name="name") String service,
		// when the service/step started
		@NotNull @WebParam(name = "started") Date started,
		// if the service/step stopped (unless it is still running)
		@WebParam(name = "stopped") Date stopped,
		@WebParam(name="exception") Exception exception);
	
	public enum TrackType {
		SERVICE,
		STEP,
		BOTH
	}
}
