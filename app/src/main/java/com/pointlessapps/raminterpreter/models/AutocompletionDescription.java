package com.pointlessapps.raminterpreter.models;

public class AutocompletionDescription extends AutocompletionItem {

	private String example;

	public AutocompletionDescription() {}

	public void set(String description, String example) {
		this.description = description;
		this.example = example;
	}

	@Override public String getDescription() {
		return example == null || example.isEmpty() ? description : description.concat("<b>").concat(example).concat("</b>");
	}
}
