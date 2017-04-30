package be.nabu.module.web.analyzer;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class WebAnalyzerManager extends JAXBArtifactManager<WebAnalyzerConfiguration, WebAnalyzer> {

	public WebAnalyzerManager() {
		super(WebAnalyzer.class);
	}

	@Override
	protected WebAnalyzer newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new WebAnalyzer(id, container, repository);
	}

}
