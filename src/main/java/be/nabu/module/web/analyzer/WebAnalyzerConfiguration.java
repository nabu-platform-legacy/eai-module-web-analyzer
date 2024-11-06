/*
* Copyright (C) 2017 Alexander Verbruggen
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

package be.nabu.module.web.analyzer;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.api.ListableSinkProviderArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.types.api.DefinedType;

@XmlRootElement(name = "webAnalyzer")
public class WebAnalyzerConfiguration {

	private ListableSinkProviderArtifact metricsDatabase;
	private List<WebAnalysis> analyses;
	private DefinedType configurationType;
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public ListableSinkProviderArtifact getMetricsDatabase() {
		return metricsDatabase;
	}
	public void setMetricsDatabase(ListableSinkProviderArtifact metricsDatabase) {
		this.metricsDatabase = metricsDatabase;
	}
	
	public List<WebAnalysis> getAnalyses() {
		if (analyses == null) {
			analyses = new ArrayList<WebAnalysis>();
		}
		return analyses;
	}
	public void setAnalyses(List<WebAnalysis> analyses) {
		this.analyses = analyses;
	}
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedType getConfigurationType() {
		return configurationType;
	}
	public void setConfigurationType(DefinedType configurationType) {
		this.configurationType = configurationType;
	}
}
