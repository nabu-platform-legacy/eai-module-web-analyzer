package be.nabu.module.web.analyzer;

public class WebAnalysis {
	
	private AnalysisTiming timing;
	private String script, title;
	
	public AnalysisTiming getTiming() {
		return timing;
	}

	public void setTiming(AnalysisTiming timing) {
		this.timing = timing;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public enum AnalysisTiming {
		BEFORE,
		AFTER,
		NORMAL
	}
}
