package com.pointlessapps.raminterpreter.models;

import android.content.Context;
import android.util.Log;

import com.pointlessapps.raminterpreter.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Command {

	public enum COMMAND {
		ADD, DIV, HALT, JGTZ, JUMP, JZERO,
		LOAD, MULT, READ, STORE, SUB, WRITE;

		public static String[] getAll() {
			String[] all = new String[values().length];
			COMMAND[] values = values();
			for(int i = 0; i < values.length; i++) all[i] = values[i].toString();
			return all;
		}
	}

	public static final List<String> keyWords = Arrays.asList(COMMAND.getAll());

	private static Pattern commentRegex = Pattern.compile("([\"'])(?:(?=(\\\\?))\\2.)*?\\1");
	private static Pattern addressRegex = Pattern.compile("([*=]?\\d)|(\\w)");

	private String label;
	private String command;
	private String address;
	private String comment;
	private boolean selected;

	public Command() {
		this("", "", "", "");
	}

	public Command(String label, String command, String address, String comment) {
		this.label = label;
		this.command = command;
		this.address = address;
		this.comment = comment;
		this.selected = false;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getComment() {
		return comment;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return selected;
	}

	@Override public String toString() {
		StringBuilder builder = new StringBuilder();
		if(getLabel() != null && !getLabel().isEmpty())
			builder.append(getLabel()).append(": ");
		if(getCommand() != null && !getCommand().isEmpty())
			builder.append(getCommand()).append(" ");
		if(getAddress() != null && !getAddress().isEmpty())
			builder.append(getAddress()).append(" ");
		if(getComment() != null && !getComment().isEmpty())
			builder.append("\"").append(getComment()).append("\"");
		return builder.toString();
	}

	public static String getStringList(List<Command> commands) {
		StringBuilder builder = new StringBuilder();
		for(Command c : commands) {
			builder.append(c.toString());
			builder.append("\n");
		}
		return builder.delete(Math.max(builder.length() - 1, 0), builder.length()).toString();
	}

	public static List<Command> getCommandsList(String stringList, Context context) throws Exception {
		List<Command> commands = new ArrayList<>();
		String[] lines = stringList.split("\n");
		for(int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if(!line.isEmpty()) {
				Command command = new Command();
				Matcher matcher = commentRegex.matcher(line);
				if(matcher.find()) {
					int commentStart = matcher.start();
					int commentEnd = matcher.end();
					command.setComment(line.substring(commentStart + 1, commentEnd - 1));
					line = line.substring(0, Math.max(commentStart - 1, 0));
				} else if(line.contains("\"") || line.contains("'"))
					throw new Exception(String.format(Locale.getDefault(), context.getString(R.string.exception_comments), i + 1));

				String[] words = line.split(" ");

				for(int j = 0; j < words.length; j++) {
					String word = words[j];
					if(word.length() > 0) {
						if(word.indexOf(":") == word.length() - 1) {
							String label = word.substring(0, Math.max(word.length() - 1, 0));
							if(label.isEmpty())
								throw new Exception(String.format(Locale.getDefault(), context.getString(R.string.exception_label_empty), i + 1));
							else if(!command.getLabel().isEmpty())
								throw new Exception(String.format(Locale.getDefault(), context.getString(R.string.exception_label_multiple), i + 1));
							if(j != 0)
								throw new Exception(String.format(Locale.getDefault(), context.getString(R.string.exception_label_position), i + 1));
							command.setLabel(label);
						} else if(keyWords.contains(word)) {
							if(!command.getCommand().isEmpty())
								throw new Exception(String.format(Locale.getDefault(), context.getString(R.string.exception_command_multiple), i + 1));
							if(!(!command.getLabel().isEmpty() && j == 1 || command.getLabel().isEmpty() && j == 0))
								throw new Exception(String.format(Locale.getDefault(), context.getString(R.string.exception_command_position), i + 1));
							command.setCommand(word);
						} else if(addressRegex.matcher(word).find()) {
							if(!command.getAddress().isEmpty())
								throw new Exception(String.format(Locale.getDefault(), context.getString(R.string.exception_address_multiple), i + 1));
							if(!(!command.getLabel().isEmpty() && j == 2 || command.getLabel().isEmpty() && j == 1))
								throw new Exception(String.format(Locale.getDefault(), context.getString(R.string.exception_address_position), i + 1));
							command.setAddress(word);
						}
					}
				}

				if(command.getCommand().isEmpty())
					throw new Exception(String.format(Locale.getDefault(), context.getString(R.string.exception_command_empty), i + 1));

				if(!command.getCommand().equals(COMMAND.HALT.toString()) && command.getAddress().isEmpty())
					throw new Exception(String.format(Locale.getDefault(), context.getString(R.string.exception_address_empty), i + 1));

				commands.add(command);
			}
		}
		return commands;
	}
}
