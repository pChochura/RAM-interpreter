package com.pointlessapps.raminterpreter.models;

import java.util.Comparator;

public class AutocompletionItem implements Comparable<AutocompletionItem> {

	public static final Comparator<? super AutocompletionItem> ItemComparator = AutocompletionItem::compareTo;

	private String text;
	private String matching;
	private String description;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setMatching(String matching) {
		this.matching = matching;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public String getFormatted() {
		int start = Math.max(text.toLowerCase().indexOf(matching.toLowerCase()), 0);
		int end = Math.min(start + matching.length(), text.length());
		return text.substring(0, start) + "<b>" + text.substring(start, end) + "</b>" + text.substring(end);
	}

	@Override public int compareTo(AutocompletionItem item) {
		int index1 = text.toLowerCase().indexOf(matching.toLowerCase());
		int index2 = item.text.toLowerCase().indexOf(item.matching.toLowerCase());
		int length1 = matching.length();
		int length2 = item.matching.length();
		int exactMatchLength1 = 0;
		int exactMatchLength2 = 0;
		int exactMatchIndex1 = index1 + length1;
		int exactMatchIndex2 = index2 + length2;
		for(int i = index1; i < index1 + length1; i++)
			if(text.charAt(i) == matching.charAt(i - index1)) {
				exactMatchIndex1 = Math.min(exactMatchIndex1, i);
				exactMatchLength1++;
			}
		for(int i = index2; i < index2 + length2; i++)
			if(item.text.charAt(i) == item.matching.charAt(i - index2)) {
				exactMatchIndex2 = Math.min(exactMatchIndex2, i);
				exactMatchLength2++;
			}
		if(exactMatchLength1 > exactMatchLength2) return -1;
		else if(exactMatchLength1 == exactMatchLength2) {
			if(exactMatchIndex1 < exactMatchIndex2) return -1;
			else if(exactMatchIndex1 == exactMatchIndex2) {
				if(length1 > length2) return -1;
				else if(length1 == length2) {
					if(index1 < index2) return -1;
					else return 1;
				}
			}
		}
		return 0;
	}
}
