package be.nabu.module.web.analyzer;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseArtifactGUIInstance;
import be.nabu.eai.developer.managers.base.BasePortableGUIManager;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.ace.AceEditor;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.module.web.analyzer.WebAnalysis.AnalysisTiming;

public class WebAnalyzerGUIManager extends BasePortableGUIManager<WebAnalyzer, BaseArtifactGUIInstance<WebAnalyzer>> {

	public WebAnalyzerGUIManager() {
		super("Web Analyzer", WebAnalyzer.class, new WebAnalyzerManager());
	}

	@Override
	public void display(MainController controller, AnchorPane rootPane, WebAnalyzer artifact) throws IOException, ParseException {
		Accordion accordion = new Accordion();

		for (WebAnalysis analysis : artifact.getConfig().getAnalyses()) {
			addAnalysis(artifact, accordion, analysis);
		}
		
		VBox vbox = new VBox();
		HBox box = new HBox();
		Button button = new Button("Add Analysis");
		button.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				WebAnalysis analysis = new WebAnalysis();
				analysis.setTitle("Untitled");
				artifact.getConfig().getAnalyses().add(analysis);
				TitledPane addAnalysis = addAnalysis(artifact, accordion, analysis);
				accordion.setExpandedPane(addAnalysis);
				MainController.getInstance().setChanged();
			}
		});
		box.getChildren().add(button);
		vbox.getChildren().addAll(box, accordion);
		
		ScrollPane scroll = new ScrollPane();
		scroll.setContent(vbox);
		
		AnchorPane.setBottomAnchor(scroll, 0d);
		AnchorPane.setRightAnchor(scroll, 0d);
		AnchorPane.setLeftAnchor(scroll, 0d);
		AnchorPane.setTopAnchor(scroll, 0d);
		rootPane.getChildren().add(scroll);
		
		accordion.prefWidthProperty().bind(scroll.widthProperty());
	}

	private TitledPane addAnalysis(WebAnalyzer artifact, Accordion accordion, WebAnalysis analysis) {
		TitledPane pane = new TitledPane();
		TextField title = new TextField();
		pane.textProperty().bind(title.textProperty());
		title.setText(analysis.getTitle());
		title.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> arg0, String oldValue, String newValue) {
				analysis.setTitle(newValue == null || newValue.trim().isEmpty() ? "Untitled" : newValue);
				MainController.getInstance().setChanged();
			}
		});
		
		ComboBox<AnalysisTiming> timing = new ComboBox<AnalysisTiming>();
		timing.getItems().add(null);
		timing.getItems().addAll(AnalysisTiming.values());
		timing.setValue(analysis.getTiming());
		
		timing.valueProperty().addListener(new ChangeListener<AnalysisTiming>() {
			@Override
			public void changed(ObservableValue<? extends AnalysisTiming> arg0, AnalysisTiming arg1, AnalysisTiming arg2) {
				analysis.setTiming(arg2);
				MainController.getInstance().setChanged();
			}
		});
		
		HBox titleBox = new HBox();
		titleBox.getChildren().addAll(new Label("Title: "), title, timing);
		
		VBox vbox = new VBox();
		HBox buttonBox = new HBox();
		Button up = new Button("Up");
		Button down = new Button("Down");
		Button delete = new Button("Delete");
		buttonBox.getChildren().addAll(up, down, delete);
		
		up.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				int index = artifact.getConfig().getAnalyses().indexOf(analysis);
				if (index > 0) {
					WebAnalysis other = artifact.getConfig().getAnalyses().get(index - 1);
					artifact.getConfig().getAnalyses().set(index - 1, analysis);
					artifact.getConfig().getAnalyses().set(index, other);
					
					accordion.getPanes().remove(pane);
					accordion.getPanes().add(index - 1, pane);
					
					MainController.getInstance().setChanged();
				}
			}
		});
		
		down.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				int index = artifact.getConfig().getAnalyses().indexOf(analysis);
				if (index < artifact.getConfig().getAnalyses().size() - 1) {
					WebAnalysis other = artifact.getConfig().getAnalyses().get(index + 1);
					artifact.getConfig().getAnalyses().set(index + 1, analysis);
					artifact.getConfig().getAnalyses().set(index, other);

					accordion.getPanes().remove(pane);
					accordion.getPanes().add(index + 1, pane);

					MainController.getInstance().setChanged();
				}
			}
		});
		
		delete.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				Confirm.confirm(ConfirmType.QUESTION, "Delete rule", "Are you sure you want to delete the rule: " + analysis.getTitle(), new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						int index = artifact.getConfig().getAnalyses().indexOf(analysis);
						if (index >= 0) {
							artifact.getConfig().getAnalyses().remove(index);
							accordion.getPanes().remove(index);
							MainController.getInstance().setChanged();
						}
					}
				});
			}
		});
		
		AceEditor editor = new AceEditor();
		editor.setContent("text/x-glue", analysis.getScript() == null ? "" : analysis.getScript());
		
		editor.subscribe(AceEditor.CHANGE, new EventHandler<Event>() {
			@Override
			public void handle(Event arg0) {
				String content = editor.getContent();
				if (content == null) {
					content = "";
				}
				if (!content.equals(analysis.getScript())) {
					analysis.setScript(content);
					MainController.getInstance().setChanged();
				}
			}
		});
		editor.subscribe(AceEditor.CLOSE, new EventHandler<Event>() {
			@Override
			public void handle(Event arg0) {
				MainController.getInstance().close();
			}
		});
		editor.subscribe(AceEditor.SAVE, new EventHandler<Event>() {
			@Override
			public void handle(Event arg0) {
				try {
					analysis.setScript(editor.getContent());
					MainController.getInstance().save();
				}
				catch (IOException e) {
					MainController.getInstance().notify(e);
				}
			}
		});
		
		vbox.getChildren().addAll(buttonBox, titleBox, editor.getWebView());
		
		pane.setContent(vbox);
		accordion.getPanes().add(pane);
		return pane;
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected BaseArtifactGUIInstance<WebAnalyzer> newGUIInstance(Entry entry) {
		return new BaseArtifactGUIInstance<WebAnalyzer>(this, entry);
	}

	@Override
	protected void setEntry(BaseArtifactGUIInstance<WebAnalyzer> guiInstance, ResourceEntry entry) {
		guiInstance.setEntry(entry);
	}

	@Override
	protected WebAnalyzer newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		return new WebAnalyzer(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@Override
	protected void setInstance(BaseArtifactGUIInstance<WebAnalyzer> guiInstance, WebAnalyzer instance) {
		guiInstance.setArtifact(instance);
	}

}
