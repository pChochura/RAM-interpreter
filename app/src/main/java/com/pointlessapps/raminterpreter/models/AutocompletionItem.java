package com.pointlessapps.raminterpreter.models;

public class AutocompletionItem {

	private String text;
	private String matching;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setMatching(String matching) {
		this.matching = matching;
	}

	public String getFormatted() {
		int start = Math.max(text.toLowerCase().indexOf(matching.toLowerCase()), 0);
		int end = Math.min(start + matching.length(), text.length());
		return text.substring(0, start) + "<b>" + matching.toUpperCase() + "</b>" + text.substring(end);
	}
}
