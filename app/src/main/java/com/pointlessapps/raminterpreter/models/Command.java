package com.pointlessapps.raminterpreter.models;

import android.content.Context;

import com.pointlessapps.raminterpreter.utils.ParseException;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

	private String label;
	private String command;
	private String address;
	private String comment;
	private boolean selected;
	private boolean breakpoint;
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
		this.breakpoint = false;
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

	public boolean isBreakpoint() {
		return breakpoint;
	}

	public void setBreakpoint(boolean breakpoint) {
		this.breakpoint = breakpoint;
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
			builder.append("#").append(getComment());
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
}
