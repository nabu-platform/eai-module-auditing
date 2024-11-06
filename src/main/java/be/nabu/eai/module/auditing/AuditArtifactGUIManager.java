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

package be.nabu.eai.module.auditing;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.module.auditing.api.FlatServiceTracker;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.Element;

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

	@SuppressWarnings("unchecked")
	@Override
	public <V> void setValue(AuditArtifact instance, Property<V> property, V value) {
		if ("auditingService".equals(property.getName())) {
			Map<String, String> properties = getConfiguration(instance).getProperties();
			if (properties == null) {
				properties = new LinkedHashMap<String, String>();
			}
			else {
				properties.clear();
			}
			if (value != null) {
				DefinedService service = (DefinedService) value;
				Method method = EAIRepositoryUtils.getMethod(FlatServiceTracker.class, "track");
				List<Element<?>> inputExtensions = EAIRepositoryUtils.getInputExtensions(service, method);
				for (Element<?> element : inputExtensions) {
					properties.put(element.getName(), properties.get(element.getName()));
				}
			}
			getConfiguration(instance).setProperties(properties);
		}
		if (!"properties".equals(property.getName())) {
			super.setValue(instance, property, value);
		}
		else if (value instanceof Map) {
			getConfiguration(instance).getProperties().putAll(((Map<? extends String, ? extends String>) value));
		}
	}
	
}
