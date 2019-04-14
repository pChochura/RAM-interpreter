package com.pointlessapps.raminterpreter.models;

import android.util.SparseIntArray;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Output {

	private static final Pattern outputFormatRegex = Pattern.compile("\\w+=(\\*?\\d+:?\\*?\\d*)");
	private static final Pattern outputFormatTableRegex = Pattern.compile("\\w+=(\\*?\\d+:\\*?\\d+)");

	private SparseIntArray values;
	private String output;

	public Output() {
		values = new SparseIntArray();
	}

	public void addValue(int registerNumber, int value) {
		values.put(registerNumber, value);
	}

	public void setOutput(String output) {
		this.output = output.replaceAll(" ", "");
	}

	public String getOutput() {
		return output;
	}

	public String formatOutput(SparseIntArray registers) {
		StringBuilder finalOutput = new StringBuilder();

		for(Matcher m = outputFormatRegex.matcher(output); m.find(); ) {
			String expression = output.substring(m.start(), m.end());
			String name;
			int start, end;
			name = expression.substring(0, expression.indexOf("="));
			if(outputFormatTableRegex.matcher(expression).matches()) {
				start = Parser.decodeAddress(registers, expression.substring(expression.indexOf("=") + 1, expression.indexOf(":")));
				end = Parser.decodeAddress(registers, expression.substring(expression.indexOf(":") + 1));
			} else start = end = Parser.decodeAddress(registers, expression.substring(expression.indexOf("=") + 1));

			StringBuilder values = new StringBuilder();

			for(int i = start; i <= end; i++) {
				values.append(registers.get(i));
				if(i < end) values.append(", ");
			}

			finalOutput.append(String.format(Locale.getDefault(), (finalOutput.toString().isEmpty() ? "" : ", ") + "%s=[%s]", name, values.toString()));
		}

//		Adding values from WRITE command
		for(int i = 0; i < values.size(); i++) finalOutput.append(finalOutput.toString().isEmpty() ? "" : ", ").append(values.valueAt(i));
		return finalOutput.toString();
	}

	public void clear() {
		values.clear();
		output = "";
	}
}
