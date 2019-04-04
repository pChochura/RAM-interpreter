package com.pointlessapps.raminterpreter.utils;

import java.util.Locale;

public class ParseException extends Exception {

	private int lineIndex;

	public ParseException(String string, String word) {
		super(String.format(Locale.getDefault(), string, word));
		this.lineIndex = -1;
	}

	public ParseException(String string, int lineIndex) {
		super(String.format(Locale.getDefault(), string, lineIndex));
		this.lineIndex = lineIndex;
	}

	public ParseException(String string, String word, int lineIndex) {
		super(String.format(Locale.getDefault(), string, word, lineIndex));
		this.lineIndex = lineIndex;
	}

	public int getLineIndex() {
		return lineIndex;
	}
}
