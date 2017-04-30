package be.nabu.module.web.analyzer;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.repository.api.ListableSinkProviderArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;

@XmlRootElement(name = "webAnalyzer")
public class WebAnalyzerConfiguration {

	private ListableSinkProviderArtifact metricsDatabase;
	private List<WebAnalysis> analyses;
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public ListableSinkProviderArtifact getMetricsDatabase() {
		return metricsDatabase;
	}
	public void setMetricsDatabase(ListableSinkProviderArtifact metricsDatabase) {
		this.metricsDatabase = metricsDatabase;
	}
	
	@EnvironmentSpecific
	public List<WebAnalysis> getAnalyses() {
		if (analyses == null) {
			analyses = new ArrayList<WebAnalysis>();
		}
		return analyses;
	}
	public void setAnalyses(List<WebAnalysis> analyses) {
		this.analyses = analyses;
	}
	
}
