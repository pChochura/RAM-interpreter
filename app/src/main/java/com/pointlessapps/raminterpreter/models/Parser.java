package com.pointlessapps.raminterpreter.models;

import android.content.Context;
import android.util.SparseArray;

import com.pointlessapps.raminterpreter.R;
import com.pointlessapps.raminterpreter.utils.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

	private static final Pattern commentRegex = Pattern.compile("#.*");
	private static final Pattern jumpAddressRegex = Pattern.compile("(\\w+)");
	private static final Pattern registerAddressRegex = Pattern.compile("([*=]?[-]?\\d+)");
	private static final Pattern literalAddress = Pattern.compile("([=][-]?\\d+)");

	public static List<Command> formatAsList(Context context, String code) throws ParseException {
		return formatAsList(context, code, true);
	}

	public static List<Command> formatAsList(Context context, String code, boolean throwable) throws ParseException {
		boolean haltCommand = false;
		List<Command> commands = new ArrayList<>();
		List<String> labels = new ArrayList<>();
		SparseArray<Command> jumpCommands = new SparseArray<>();
		String[] lines = code.split("\n");

		for(int i = 0; i < lines.length; i++) {
			String line = lines[i].replaceAll("^\\s+|\\s+$|\\s+(?=\\s)", "");
			if(!line.isEmpty()) {
				Command command = new Command();

				line = parseAndRemoveComment(line, command);

				String[] words = line.split(" ");

				for(int j = 0; j < words.length; j++) {
					String word = words[j];
					if(word.length() > 0) {
						if(j == 0 && command.getLabel().isEmpty() && word.indexOf(":") == word.length() - 1)
							parseLabel(context, command, word, i, throwable);
						else if(j == 0 || j == 1 && !command.getLabel().isEmpty())
							parseCommand(context, command, word, i, throwable);
						else if(j == 1 || j == 2 && !command.getLabel().isEmpty())
							parseAddress(context, command, word, i, throwable);
					}
				}

				if(command.getCommand().isEmpty()) {
					if(throwable) throw new ParseException(context.getResources().getString(R.string.exception_command_empty), i + 1);
					else continue;
				}

				if(!command.getCommand().equals(Command.COMMAND.HALT.toString()) && command.getAddress().isEmpty()) {
					if(throwable) throw new ParseException(context.getResources().getString(R.string.exception_address_empty), i + 1);
					else continue;
				}

				if(command.getCommand().equals(Command.COMMAND.READ.toString()) || command.getCommand().equals(Command.COMMAND.STORE.toString()))
					if(literalAddress.matcher(command.getAddress()).matches()) {
						if(throwable) throw new ParseException(context.getResources().getString(R.string.exception_address_spelling), command.getAddress(), i + 1);
						else continue;
					}

				if(command.getCommand().equals(Command.COMMAND.JZERO.toString()) || command.getCommand().equals(Command.COMMAND.JGTZ.toString()) || command.getCommand().equals(Command.COMMAND.JUMP.toString()))
					jumpCommands.put(i, command);

				if(!command.getLabel().isEmpty())
					labels.add(command.getLabel());

				if(command.getCommand().equals(Command.COMMAND.HALT.toString()))
					haltCommand = true;

				commands.add(command);
			}
		}
		for(int i = 0; i < jumpCommands.size(); i++) {
			Command command = jumpCommands.valueAt(i);
			if(!labels.contains(command.getAddress()) && throwable)
				throw new ParseException(context.getResources().getString(R.string.exception_label_spelling), command.getAddress(), jumpCommands.keyAt(i) + 1);
		}
		if(!haltCommand && throwable)
			throw new ParseException(context.getResources().getString(R.string.exception_halt_missing), Command.COMMAND.HALT.toString());
		return commands;
	}

	private static String parseAndRemoveComment(String line, Command command) {
		Matcher matcher = commentRegex.matcher(line);
		if(matcher.find()) {
			int commentStart = matcher.start();
			int commentEnd = matcher.end() < commentStart ? line.length() : matcher.end();
			command.setComment(line.substring(commentStart + 1, commentEnd));
			line = line.substring(0, Math.max(commentStart - 1, 0));
		}
		return line;
	}

	private static void parseLabel(Context context, Command command, String word, int lineNumber, boolean throwable) throws ParseException {
		String label = word.substring(0, Math.max(word.length() - 1, 0));
		if(label.isEmpty() && throwable)
			throw new ParseException(context.getResources().getString(R.string.exception_label_empty), lineNumber + 1);
		if(!command.getLabel().isEmpty() && throwable)
			throw new ParseException(context.getResources().getString(R.string.exception_label_multiple), lineNumber + 1);
		command.setLabel(label);
	}

	private static void parseCommand(Context context, Command command, String word, int lineNumber, boolean throwable) throws ParseException {
		if(!Command.keyWords.contains(word.toUpperCase()) && throwable)
			throw new ParseException(context.getResources().getString(R.string.exception_command_spelling), word, lineNumber + 1);
		if(!command.getCommand().isEmpty() && throwable)
			throw new ParseException(context.getResources().getString(R.string.exception_command_multiple), lineNumber + 1);
		command.setCommand(word.toUpperCase());
	}

	private static void parseAddress(Context context, Command command, String word, int lineNumber, boolean throwable) throws ParseException {
		String c = command.getCommand();
		if(!c.isEmpty())
			if(c.equals(Command.COMMAND.JZERO.toString()) || c.equals(Command.COMMAND.JGTZ.toString()) || c.equals(Command.COMMAND.JUMP.toString())) {
				if(!jumpAddressRegex.matcher(word).matches() && throwable)
					throw new ParseException(context.getResources().getString(R.string.exception_address_spelling), word, lineNumber + 1);
			} else if(!registerAddressRegex.matcher(word).matches() && throwable)
				throw new ParseException(context.getResources().getString(R.string.exception_address_spelling), word, lineNumber + 1);
		if(!command.getAddress().isEmpty() && throwable)
			throw new ParseException(context.getResources().getString(R.string.exception_address_multiple), lineNumber + 1);
		command.setAddress(word);
	}

	public static String formatCode(Context context, String code) throws ParseException {
		return Command.getStringList(formatAsList(context, code));
	}
}
