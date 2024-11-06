/*
* Copyright (C) 2015 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.auditing.api;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.jws.WebParam;
import javax.validation.constraints.NotNull;

import be.nabu.libs.artifacts.api.ExternalDependency;
import be.nabu.libs.authentication.api.Device;
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
		@WebParam(name = "hierarchy") List<UUID> hierarchy,
		@WebParam(name = "token") Token token,
		@WebParam(name = "device") Device device,
		@NotNull @WebParam(name="name") String service,
		// when the service/step started
		@NotNull @WebParam(name = "started") Date started,
		// if the service/step stopped (unless it is still running)
		@WebParam(name = "stopped") Date stopped,
		@WebParam(name = "exception") Exception exception,
		@WebParam(name = "input") Object input,
		@WebParam(name = "output") Object output,
		@WebParam(name = "cached") Boolean cached,
		@WebParam(name = "external") List<ExternalDependency> externalDependencies,
		@WebParam(name = "sequence") Long sequence,
		// how long it took in ms
		@WebParam(name = "duration") Long duration,
		@WebParam(name = "depth") Integer depth,
		@WebParam(name = "source") String source);
	
	public enum TrackType {
		SERVICE,
		STEP,
		REPORT,
		DESCRIPTION,
		BOTH
	}
	
	public enum TrackTimeType {
		BEFORE,
		AFTER,
		ERROR,
		ALL
	}
}
