package net.ldvsoft.warofviruses;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static net.ldvsoft.warofviruses.GameLogic.*;

/**
 * FigureSet contains loaded Drawables for BoardCellButtons, which it gets from FigureSources.
 * @see FigureSource
 * FigureSet uses three sources for every owner of figures displayed.
 *
 * FigureSet is designed to be created once, then via `setFigureSource' changing Drawables source
 * and unloading already loaded Drawables that are outdated now. Buttons itself are still required
 * to be updated manually.
 */
public class FigureSet {
    /**
     * These are all available sources. All new ones should be added here!
     */
    private static final Map<String, FigureSource> SOURCES = new Hashtable<>();

    /**
     * For every figure, STATES holds list of BoardCellStates that are owned by that figure.
     * These are used to unload Drawables when changing source.
     */
    private static final Map<PlayerFigure, List<BoardCellState>> STATES = new EnumMap<>(PlayerFigure.class);

    static {
        for (PlayerFigure figure : PlayerFigure.values()) {
            STATES.put(figure, new ArrayList<BoardCellState>());
        }
        for (CellType cellType : CellType.values()) {
            for (boolean isHighlighted : new boolean[]{false, true}) {
                for (PlayerFigure focus : PlayerFigure.values()) {
                    STATES.get(cellType.getOwner()).add(BoardCellState.get(cellType, isHighlighted, focus));
                }
            }
        }

        addSource(new DefaultFigureSource());
    }

    private static void addSource(FigureSource source) {
        SOURCES.put(source.getName(), source);
    }

    private Map<PlayerFigure, String> figureSource = new EnumMap<>(PlayerFigure.class);
    private Map<BoardCellState, Drawable> loadedFigures = new Hashtable<>();
    private int hueCross, hueZero;

    /**
     * Returns Drawable for given BoardCellState. Loads it if not yet (or has been outdated).
     * @param state cell state
     * @return Drawable to display that state
     */
    public Drawable getFigure(BoardCellState state, Context context) {
        if (! loadedFigures.containsKey(state) && figureSource.containsKey(state.getCellType().getOwner())) {
            String sourceName = figureSource.get(state.getCellType().getOwner());
            FigureSource source = SOURCES.get(sourceName);
            loadedFigures.put(state, source.loadFigure(state, hueCross, hueZero, context));
        }
        return loadedFigures.get(state);
    }

    /**
     * Changes FigureSource for given figure. All already loaded Drawables for that figure will be
     * unloaded, so that new requests will load new Drawables from new sourceName
     * @param figure figure to change sourceName for
     * @param sourceName new FigureSource
     */
    public void setFigureSource(PlayerFigure figure, String sourceName) {
        if (figureSource.get(figure) == sourceName) {
            return;
        }
        for (BoardCellState state : STATES.get(figure)) {
            loadedFigures.remove(state);
        }
        figureSource.put(figure, sourceName);
    }

    public void setHueCross(int newHueCross) {
        if (hueCross == newHueCross) {
            return;
        }
        hueCross = newHueCross;
        loadedFigures.clear();
    }

    public void setHueZero(int newHueZero) {
        if (hueZero == newHueZero) {
            return;
        }
        hueZero = newHueZero;
        loadedFigures.clear();
    }
}
