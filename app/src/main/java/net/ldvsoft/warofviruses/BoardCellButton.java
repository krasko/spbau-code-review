package net.ldvsoft.warofviruses;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.larvalabs.svgandroid.SVGParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;

import static android.graphics.Color.HSVToColor;
import static android.graphics.Color.argb;

/**
 * Created by ldvsoft on 17.10.15.
 */
public class BoardCellButton extends ImageView {
    protected final static int CROSS_FG   = argb(0, 255, 0  , 0  );
    protected final static int CROSS_BG   = argb(0, 127, 0  , 0  );
    protected final static int ZERO_FG    = argb(0, 0  , 0  , 255);
    protected final static int ZERO_BG    = argb(0, 0  , 0  , 127);
    protected final static int NEUTRAL_FG = argb(0, 255, 255, 255);
    protected final static int NEUTRAL_BG = argb(0, 127, 127, 127);
    protected final static int BORDER     = argb(0, 0  , 255, 0  );

    protected final static int EMPTY_FG   = argb(0, 200, 200, 200);
    protected final static int EMPTY_BG   = argb(0, 240, 240, 240);

    protected static Drawable cellEmpty;
    protected static Drawable cellEmpty_forCross;
    protected static Drawable cellEmpty_forZero;
    protected static Drawable cellCross;
    protected static Drawable cellCross_forCross;
    protected static Drawable cellCross_forZero;
    protected static Drawable cellCross_forZero_highlighted;
    protected static Drawable cellCross_highlighted;
    protected static Drawable cellCrossDead;
    protected static Drawable cellCrossDead_forZero;
    protected static Drawable cellCrossDead_highlighted;
    protected static Drawable cellZero;
    protected static Drawable cellZero_forCross;
    protected static Drawable cellZero_forCross_highlighted;
    protected static Drawable cellZero_forZero;
    protected static Drawable cellZero_highlighted;
    protected static Drawable cellZeroDead;
    protected static Drawable cellZeroDead_forCross;
    protected static Drawable cellZeroDead_highlighted;


    public BoardCellButton(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public BoardCellButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public BoardCellButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        if (drawable == getDrawable())
            return;
        super.setImageDrawable(drawable);
        invalidate();
    }

    protected static int hueToColor(float hue, float saturation, float value) {
        return HSVToColor(new float[]{hue, saturation, value});
    }

    protected static int getHighColor(float hue) {
        return hueToColor(hue, 1.00f, 1.00f);
    }

    protected static int getMediumColor(float hue) {
        return hueToColor(hue, 0.70f, 1.00f);
    }

    protected static int getLowColor(float hue) {
        return hueToColor(hue, 0.43f, 1.00f);
    }

    protected static Drawable getImage(String svg, Map<Integer, Integer> map) {
        return SVGParser.getSVGFromString(svg, map).createPictureDrawable();
    }

    public static void loadDrawables(Context context, int hueCross, int hueZero) {
        int crossHigh   = getHighColor(hueCross);
        int crossMedium = getMediumColor(hueCross);
        int crossLow    = getLowColor(hueCross);
        int zeroHigh    = getHighColor(hueZero);
        int zeroMedium  = getMediumColor(hueZero);
        int zeroLow     = getLowColor(hueZero);
        String cross, crossDead, zero, zeroDead, empty;
        try {
            empty      = loadSVG(context, R.raw.board_cell_empty     );
            cross      = loadSVG(context, R.raw.board_cell_cross     );
            crossDead  = loadSVG(context, R.raw.board_cell_cross_dead);
            zero       = loadSVG(context, R.raw.board_cell_zero      );
            zeroDead   = loadSVG(context, R.raw.board_cell_zero_dead );
        } catch (IOException e) {
            e.printStackTrace();
            Log.wtf("BoardCellButton", "Cannot load SVGs!");
            return;
        }
        Hashtable<Integer, Integer> colors = new Hashtable<>(10);
        colors.put(CROSS_FG, crossHigh);
        colors.put(CROSS_BG, crossLow);
        colors.put(ZERO_FG, zeroHigh);
        colors.put(ZERO_BG, zeroLow);
        colors.put(NEUTRAL_FG, EMPTY_FG);
        colors.put(NEUTRAL_BG, EMPTY_BG);

        colors.put(BORDER, EMPTY_BG);
        cellEmpty                     = getImage(empty    , colors);

        colors.put(BORDER, crossHigh);
        cellEmpty_forCross            = getImage(empty    , colors);
        cellCross_forCross            = getImage(cross    , colors);
        cellZero_forCross             = getImage(zero     , colors);
        cellZeroDead_forCross         = getImage(zeroDead , colors);

        colors.put(BORDER, crossLow );
        cellCross                     = getImage(cross    , colors);
        cellZeroDead                  = getImage(zeroDead , colors);

        colors.put(BORDER, zeroHigh );
        cellEmpty_forZero             = getImage(empty    , colors);
        cellCross_forZero             = getImage(cross    , colors);
        cellCrossDead_forZero         = getImage(crossDead, colors);
        cellZero_forZero              = getImage(zero     , colors);

        colors.put(BORDER, zeroLow  );
        cellZero                      = getImage(zero     , colors);
        cellCrossDead                 = getImage(crossDead, colors);

        colors.put(CROSS_BG, crossMedium);
        colors.put(ZERO_BG, zeroMedium);

        colors.put(BORDER, crossHigh);
        cellZero_forCross_highlighted = getImage(zero     , colors);

        colors.put(BORDER, crossMedium);
        cellCross_highlighted         = getImage(cross    , colors);
        cellZeroDead_highlighted      = getImage(zeroDead , colors);

        colors.put(BORDER, zeroHigh);
        cellCross_forZero_highlighted = getImage(cross    , colors);

        colors.put(BORDER, zeroMedium);
        cellZero_highlighted          = getImage(zero     , colors);
        cellCrossDead_highlighted     = getImage(crossDead, colors);
    }

    protected static String loadSVG(Context context, int id) throws IOException {
        try {
            InputStream is = context.getResources().openRawResource(id);
            byte[] b = new byte[is.available()];
            is.read(b);
            return new String(b);
        } catch (Exception e) {
            Log.wtf("BoardCellButton", "What the hell is with svg?!");
            throw e;
        }
    }
}
