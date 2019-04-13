package com.pointlessapps.raminterpreter.utils;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.Objects;

public class BottomPaddingItem extends RecyclerView.ItemDecoration {

	private int bottomPadding;

	public BottomPaddingItem(int bottomPadding) {
		this.bottomPadding = bottomPadding;
	}

	@Override
	public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
		try {
			if(parent.getChildAdapterPosition(view) == Objects.requireNonNull(parent.getAdapter()).getItemCount() - 1)
				outRect.set(0, 0, 0, bottomPadding);
		} catch(NullPointerException ignored) {}
	}
}
