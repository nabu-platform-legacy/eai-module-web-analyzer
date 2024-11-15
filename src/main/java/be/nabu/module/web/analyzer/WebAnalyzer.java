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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebApplicationMethods;
import be.nabu.eai.module.web.application.WebFragment;
import be.nabu.eai.module.web.application.WebFragmentConfiguration;
import be.nabu.eai.module.web.application.WebFragmentPriority;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.glue.api.ParserProvider;
import be.nabu.glue.api.Script;
import be.nabu.glue.api.ScriptRepository;
import be.nabu.glue.core.impl.parsers.GlueParserProvider;
import be.nabu.glue.core.impl.providers.StaticJavaMethodProvider;
import be.nabu.glue.core.repositories.DynamicScriptRepository;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.services.ServiceMethodProvider;
import be.nabu.glue.utils.DynamicScript;
import be.nabu.libs.authentication.api.Permission;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.glue.GluePostProcessListener;
import be.nabu.libs.http.glue.GluePreprocessListener;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.types.api.ComplexType;

public class WebAnalyzer extends JAXBArtifact<WebAnalyzerConfiguration> implements WebFragment {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public WebAnalyzer(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "web-analyzer.xml", WebAnalyzerConfiguration.class);
	}

	private Map<String, List<EventSubscription<?, ?>>> subscriptions = new HashMap<String, List<EventSubscription<?, ?>>>();
	
	private String getKey(WebApplication artifact, String path) {
		return artifact.getId() + ":" + path;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void start(WebApplication artifact, String path) throws IOException {
		String key = getKey(artifact, path);
		if (subscriptions.containsKey(key)) {
			stop(artifact, path);
		}

		if (path == null) {
			path = "/";
		}
		List<EventSubscription<?, ?>> list = subscriptions.get(key);
		if (list == null) {
			list = new ArrayList<EventSubscription<?, ?>>();
			synchronized(subscriptions) {
				subscriptions.put(key, list);
			}
		}

		if (getConfig().getAnalyses() != null && !getConfig().getAnalyses().isEmpty()) {
			ServiceMethodProvider serviceMethodProvider = new ServiceMethodProvider(getRepository(), getRepository());
			StaticJavaMethodProvider webApplicationProvider = new StaticJavaMethodProvider(new WebApplicationMethods(artifact));
			GlueParserProvider parserProvider;
			if (getConfig().getMetricsDatabase() == null) {
				parserProvider = new GlueParserProvider(serviceMethodProvider, webApplicationProvider);
			}
			else {
				StaticJavaMethodProvider metricsMethodProvider = new StaticJavaMethodProvider(new MetricsMethods(getConfig().getMetricsDatabase(), getId()));
				parserProvider = new GlueParserProvider(serviceMethodProvider, metricsMethodProvider, webApplicationProvider);
			}
			String environmentName = path;
			if (environmentName.startsWith("/")) {
				environmentName.substring(1);
			}
			if (environmentName.isEmpty()) {
				environmentName = "root";
			}
			
			SimpleExecutionEnvironment environment = new SimpleExecutionEnvironment(environmentName, artifact.getEnvironmentProperties());
			EventDispatcher dispatcher = artifact.getDispatcher();

			// get a reversed list because we do a promote()
			// this means the last will actually be the highest ordered
			// we no longer do a promote but state that we are highest priority web artifacts
			// this means we can still enjoy such niceties as basic/jwt/... tokens, initial preprocessing etc
			List<WebAnalysis> analyses = new ArrayList<WebAnalysis>(getConfig().getAnalyses());
//			Collections.reverse(analyses);
			for (WebAnalysis analysis : analyses) {
				if (analysis.getScript() != null && !analysis.getScript().isEmpty()) {
					try {
						DynamicScriptRepository repository = new DynamicScriptRepository(parserProvider);
						repository.setGroup(GlueListener.PUBLIC);
						DynamicScript script = new DynamicScript(repository, parserProvider, analysis.getScript());
						repository.add(script);
						
						ScriptRepository singleRepository = new ScriptRepository() {
							@Override
							public Iterator<Script> iterator() {
								return repository.iterator();
							}
							@Override
							public Script getScript(String name) throws IOException, ParseException {
								return script;
							}
							@Override
							public ParserProvider getParserProvider() {
								return repository.getParserProvider();
							}
							@Override
							public ScriptRepository getParent() {
								return null;
							}
							@Override
							public void refresh() throws IOException {
								repository.refresh();
							}
						};
						
						EventSubscription<?, ?> subscription;
						
						if (analysis.getTiming() == AnalysisTiming.BEFORE) {
							subscription = dispatcher.subscribe(HTTPRequest.class, new GluePreprocessListener(artifact.getAuthenticator(), artifact.getSessionProvider(), singleRepository, environment, path));
							if (!path.equals("/")) {
								((EventSubscription<HTTPRequest, ?>) subscription).filter(HTTPServerUtils.limitToPath(path));
							}
						}
						else if (analysis.getTiming() == AnalysisTiming.AFTER) {
							subscription = dispatcher.subscribe(HTTPResponse.class, new GluePostProcessListener(singleRepository, environment, path));
							if (!path.equals("/")) {
								((EventSubscription<HTTPResponse, ?>) subscription).filter(HTTPServerUtils.limitToRequestPath(path));
							}
						}
						else {
							GlueListener listener = new GlueListener(artifact.getSessionProvider(), singleRepository, environment, path);
							listener.setRequireResponse(false);
							listener.setRequireFullName(false);
							listener.setAllowEncoding(!EAIResourceRepository.isDevelopment());
							listener.setAuthenticator(artifact.getAuthenticator());
							listener.setTokenValidator(artifact.getTokenValidator());
							listener.setPermissionHandler(artifact.getPermissionHandler());
							listener.setRoleHandler(artifact.getRoleHandler());
							listener.setDeviceValidator(artifact.getDeviceValidator());
							listener.setRealm(artifact.getRealm());
							subscription = dispatcher.subscribe(HTTPRequest.class, listener);
							if (!path.equals("/")) {
								((EventSubscription<HTTPRequest, ?>) subscription).filter(HTTPServerUtils.limitToPath(path));
							}
						}
						
						list.add(subscription);
					}
					catch (ParseException e) {
						logger.error("Could not parse analysis '" + analysis.getTitle() + "' in: " + getId(), e);
					}
				}
			}
		}
	}
	
	@Override
	public List<WebFragmentConfiguration> getFragmentConfiguration() {
		List<WebFragmentConfiguration> configuration = new ArrayList<WebFragmentConfiguration>();
		if (getConfig().getConfigurationType() != null) {
			configuration.add(new WebFragmentConfiguration() {
				@Override
				public ComplexType getType() {
					return (ComplexType) getConfig().getConfigurationType();
				}
				@Override
				public String getPath() {
					return "/";
				}
			});
		}
		return configuration;
	}

	@Override
	public void stop(WebApplication artifact, String path) {
		String key = getKey(artifact, path);
		if (subscriptions.containsKey(key)) {
			synchronized(subscriptions) {
				if (subscriptions.containsKey(key)) {
					for (EventSubscription<?, ?> subscription : subscriptions.get(key)) {
						subscription.unsubscribe();
					}
					subscriptions.remove(key);
				}
			}
		}
	}

	@Override
	public List<Permission> getPermissions(WebApplication artifact, String path) {
		return new ArrayList<Permission>();
	}

	@Override
	public boolean isStarted(WebApplication artifact, String path) {
		return subscriptions.containsKey(getKey(artifact, path));
	}

	@Override
	public WebFragmentPriority getPriority() {
		return WebFragmentPriority.HIGHEST;
	}
	
}
