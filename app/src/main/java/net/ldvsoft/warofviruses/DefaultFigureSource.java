package net.ldvsoft.warofviruses;

import android.content.Context;

import java.util.EnumMap;
import java.util.Map;

import static net.ldvsoft.warofviruses.GameLogic.CellType;

/**
 * Created by ldvsoft on 05.02.16.
 */
public class DefaultFigureSource extends SVGFigureSource {
    public static final String NAME = "default";
    private static final Map<CellType, Integer> resorceIds = new EnumMap<>(CellType.class);

    static {
        resorceIds.put(CellType.EMPTY, R.raw.board_cell_empty);
        resorceIds.put(CellType.CROSS, R.raw.board_cell_cross);
        resorceIds.put(CellType.ZERO, R.raw.board_cell_zero);
        resorceIds.put(CellType.DEAD_CROSS, R.raw.board_cell_cross_dead);
        resorceIds.put(CellType.DEAD_ZERO, R.raw.board_cell_zero_dead);
    }

    @Override
    protected int getResourceId(BoardCellState state) {
        return resorceIds.get(state.getCellType());
    }

    @Override
    public String getName() {
        return NAME;
    }
}
