package net.ldvsoft.warofviruses;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * Created by ldvsoft on 04.02.16.
 */
public interface FigureSource {
    String getName();
    Drawable loadFigure(BoardCellState state, int hueCross, int hueZero, Context context);
}
