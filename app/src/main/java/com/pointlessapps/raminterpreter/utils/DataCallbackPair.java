package com.pointlessapps.raminterpreter.utils;

public interface DataCallbackPair<K, L> {
	void run(K first, L second);
}
