package net.ldvsoft.warofviruses;

import net.ldvsoft.warofviruses.GameLogic.PlayerFigure;

import static net.ldvsoft.warofviruses.GameLogic.CellType;

/**
 * Alike-enum state for BoardCellButton state to choose figure,
 * that is actually `CellType * Boolean * PlayerFigure'
 *
 * Use `get' method for accessing the enum, then use it anywhere with maps.
 * It is written not to allocate memory at every `get'.
 */
public final class BoardCellState {
    private static final int STATES_COUNT = CellType.values().length * 2 * PlayerFigure.values().length;
    private static final BoardCellState[] STATES = new BoardCellState[STATES_COUNT];

    public static BoardCellState get(CellType cellType, boolean isHighlighted, PlayerFigure focus) {
        int hash = getHash(cellType, isHighlighted, focus);
        if (STATES[hash] == null) {
            STATES[hash] = new BoardCellState(cellType, isHighlighted, focus);
        }
        return STATES[hash];
    }

    public static BoardCellState get(CellType cellType) {
        return get(cellType, false, PlayerFigure.NONE);
    }

    private static int getHash(CellType cellType, boolean isHighlighted, PlayerFigure focus) {
        return (cellType.ordinal() * 2 + (isHighlighted ? 1 : 0)) * PlayerFigure.values().length + focus.ordinal();
    }

    private CellType cellType;
    private boolean isHighlighted;
    private PlayerFigure focus;

    private BoardCellState(CellType cellType, boolean isHighlighted, PlayerFigure focus) {
        this.cellType = cellType;
        this.isHighlighted = isHighlighted;
        this.focus = focus;
    }

    public CellType getCellType() {
        return cellType;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public PlayerFigure getFocus() {
        return focus;
    }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof BoardCellState))
            return false;
        BoardCellState that = (BoardCellState) o;
        return this.cellType == that.cellType
                && this.isHighlighted == that.isHighlighted
                && this.focus == that.focus;
    }

    @Override
    public int hashCode() {
        return getHash(cellType, isHighlighted, focus);
    }
}
