package com.pointlessapps.raminterpreter.utils;

import android.text.Editable;
import android.text.TextWatcher;

public class OnTextChanged implements TextWatcher {

	private final OnTextChangedListener onTextChangedListener;

	public OnTextChanged(OnTextChangedListener onTextChangedListener) {
		this.onTextChangedListener = onTextChangedListener;
	}

	@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {

	}

	@Override public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	@Override public void afterTextChanged(Editable s) {
		onTextChangedListener.onTextChanged(s);
	}

	public interface OnTextChangedListener {
		void onTextChanged(Editable text);
	}
}
