package net.ldvsoft.warofviruses;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.larvalabs.svgandroid.SVGParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.Map;

import static android.graphics.Color.HSVToColor;
import static android.graphics.Color.argb;

/**
 * Created by ldvsoft on 17.10.15.
 */
public class BoardCellButton extends ImageView {
    private static final String TAG = "BoardCellButton";

    private Context context;

    public BoardCellButton(Context context) {
        super(context);
        this.context = context;
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public BoardCellButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public BoardCellButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        if (drawable == getDrawable()) {
            return;
        }
        super.setImageDrawable(drawable);
        invalidate();
    }

    public void setFigure(FigureSet set, BoardCellState state) {
        setImageDrawable(set.getFigure(state, context));
    }
}
