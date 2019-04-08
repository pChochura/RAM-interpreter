package com.pointlessapps.raminterpreter.models;

import android.content.Context;
import android.util.SparseArray;

import com.pointlessapps.raminterpreter.R;
import com.pointlessapps.raminterpreter.utils.ParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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

	private static final Pattern commentRegex = Pattern.compile("([\"'])(?:(?=(\\\\?))\\2.)*?\\1");
	private static final Pattern jumpAddressRegex = Pattern.compile("(\\w+)");
	private static final Pattern registerAddressRegex = Pattern.compile("([*=]?\\d+)");

	private String label;
	private String command;
	private String address;
	private String comment;
	private boolean selected;
	private final int id;

	public Command() {
		this("", "", "", "");
	}

	public Command(String label, String command, String address, String comment) {
		this.label = label;
		this.command = command;
		this.address = address;
		this.comment = comment;
		this.selected = false;
		this.id = UUID.randomUUID().hashCode();
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

	@Override public int hashCode() {
		return id;
	}

	public static String getStringList(List<Command> commands) {
		StringBuilder builder = new StringBuilder();
		for(Command c : commands) {
			builder.append(c.toString());
			builder.append("\n");
		}
		return builder.delete(Math.max(builder.length() - 1, 0), builder.length()).toString();
	}

	public static List<Command> getCommandsList(String stringList, Context context) throws ParseException {
		boolean haltCommand = false;
		List<Command> commands = new ArrayList<>();
		List<String> labels = new ArrayList<>();
		SparseArray<Command> jumpCommands = new SparseArray<>();
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
					throw new ParseException(context.getResources().getString(R.string.exception_comments), i + 1);

				String[] words = line.split(" ");

				for(int j = 0; j < words.length; j++) {
					String word = words[j];
					if(word.length() > 0) {
						if(j == 0 && command.getLabel().isEmpty() && word.indexOf(":") == word.length() - 1) {
							String label = word.substring(0, Math.max(word.length() - 1, 0));
							if(label.isEmpty())
								throw new ParseException(context.getResources().getString(R.string.exception_label_empty), i + 1);
							if(!command.getLabel().isEmpty())
								throw new ParseException(context.getResources().getString(R.string.exception_label_multiple), i + 1);
							command.setLabel(label);
						} else if(j == 0 || j == 1 && !command.getLabel().isEmpty()) {
							if(!keyWords.contains(word.toUpperCase()))
								throw new ParseException(context.getResources().getString(R.string.exception_command_spelling), word, i + 1);
							if(!command.getCommand().isEmpty())
								throw new ParseException(context.getResources().getString(R.string.exception_command_multiple), i + 1);
							command.setCommand(word.toUpperCase());
						} else if(j == 1 || j == 2 && !command.getLabel().isEmpty()) {
							String c = command.getCommand();
							if(!c.isEmpty())
								if(c.equals(COMMAND.JZERO.toString()) || c.equals(COMMAND.JGTZ.toString()) || c.equals(COMMAND.JUMP.toString())) {
									if(!jumpAddressRegex.matcher(word).matches())
										throw new ParseException(context.getResources().getString(R.string.exception_address_spelling), word, i + 1);
								} else if(!registerAddressRegex.matcher(word).matches())
									throw new ParseException(context.getResources().getString(R.string.exception_address_spelling), word, i + 1);
							if(!command.getAddress().isEmpty())
								throw new ParseException(context.getResources().getString(R.string.exception_address_multiple), i + 1);
							command.setAddress(word);
						}
					}
				}

				if(command.getCommand().isEmpty())
					throw new ParseException(context.getResources().getString(R.string.exception_command_empty), i + 1);

				if(!command.getCommand().equals(COMMAND.HALT.toString()) && command.getAddress().isEmpty())
					throw new ParseException(context.getResources().getString(R.string.exception_address_empty), i + 1);

				if(command.getCommand().equals(COMMAND.JZERO.toString()) || command.getCommand().equals(COMMAND.JGTZ.toString()) || command.getCommand().equals(COMMAND.JUMP.toString()))
					jumpCommands.put(i, command);

				if(!command.getLabel().isEmpty())
					labels.add(command.getLabel());

				if(command.getCommand().equals(COMMAND.HALT.toString()))
					haltCommand = true;

				commands.add(command);
			}
		}
		for(int i = 0; i < jumpCommands.size(); i++) {
			Command command = jumpCommands.valueAt(i);
			if(!labels.contains(command.getAddress()))
				throw new ParseException(context.getResources().getString(R.string.exception_label_spelling), command.getAddress(), jumpCommands.keyAt(i) + 1);
		}
		if(!haltCommand)
			throw new ParseException(context.getResources().getString(R.string.exception_halt_missing), COMMAND.HALT.toString());
		return commands;
	}
}
