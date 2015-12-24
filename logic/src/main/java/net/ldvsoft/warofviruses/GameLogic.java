package net.ldvsoft.warofviruses;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Сева on 21.10.2015.
 */
public class GameLogic {
    private ArrayList<GameEvent> events;
    private ArrayList<GameEvent> crossesEvents;
    private ArrayList<GameEvent> zeroesEvents;

    public static final int BOARD_SIZE = 10;

    public List<GameEvent> getEventHistory() {
        return events;
    }

    public enum CellType {CROSS, ZERO, DEAD_CROSS, DEAD_ZERO, EMPTY}
    public enum PlayerFigure {CROSS, ZERO, NONE}
    public enum GameState {NOT_RUNNING, RUNNING, DRAW, CROSS_WON, ZERO_WON}

    public static class Cell {
        private CellType cellType = CellType.EMPTY;
        private boolean canMakeTurn = false;
        private boolean isActive = false;

        public Cell() {
        }

        public Cell(CellType cellType, boolean canMakeTurn) {
            this.cellType = cellType;
            this.canMakeTurn = canMakeTurn;
        }

        public Cell(Cell cell) {
            if (cell == null) {
                return;
            }

            cellType = cell.cellType;
            canMakeTurn = cell.canMakeTurn;
            isActive = cell.isActive;
        }

        public PlayerFigure getOwner() {
            if (cellType == CellType.ZERO || cellType == CellType.DEAD_CROSS) {
                return PlayerFigure.ZERO;
            } else if (cellType == CellType.CROSS || cellType == CellType.DEAD_ZERO) {
                return PlayerFigure.CROSS;
            } else {
                return PlayerFigure.NONE;
            }
        }

        public boolean isActive() {
            return isActive;
        }

        public void setOwner(PlayerFigure newOwner) {
            if (newOwner == PlayerFigure.CROSS) {
                cellType = CellType.CROSS;
            } else if (newOwner == PlayerFigure.ZERO) {
                cellType = CellType.ZERO;
            } else {
                cellType = CellType.EMPTY;
            }
        }

        public boolean isDead() {
            return cellType == CellType.DEAD_CROSS || cellType == CellType.DEAD_ZERO;
        }

        public CellType getCellType() {
            return cellType;
        }

        public boolean canMakeTurn() {
            return canMakeTurn;
        }

        public void setOwnerOnDead(PlayerFigure newOwner) {
            if (newOwner == PlayerFigure.CROSS) {
                cellType = CellType.DEAD_ZERO;
            } else {
                cellType = CellType.DEAD_CROSS;
            }
        }
    }

    static final int[][] ADJACENT_DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    private Cell board[][] = new Cell[BOARD_SIZE][BOARD_SIZE];

    private GameState currentGameState = GameState.NOT_RUNNING;
    private PlayerFigure currentPlayerFigure = PlayerFigure.CROSS;

    private boolean previousTurnSkipped = false;
    private int currentTurn = 0;
    private int currentMiniturn = 0;

    public PlayerFigure getCurrentPlayerFigure() {
        return currentPlayerFigure;
    }

    public static GameLogic deserialize(List<GameEvent> events) {
        GameLogic logic = new GameLogic();
        logic.newGame();
        for (GameEvent event : events) {
            event.applyEvent(logic);
        }
        return logic;
    }

    public GameLogic() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = new Cell();
            }
        }
    }

    public GameLogic(GameLogic logic) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = new Cell(logic.board[i][j]);
            }
        }
        currentMiniturn = logic.currentMiniturn;
        currentTurn = logic.currentTurn;
        previousTurnSkipped = logic.previousTurnSkipped;
        currentGameState = logic.currentGameState;
        currentPlayerFigure = logic.currentPlayerFigure;
        events = new ArrayList<>(logic.events);
        crossesEvents = new ArrayList<>(logic.crossesEvents);
        zeroesEvents = new ArrayList<>(logic.zeroesEvents);
    }

    public void newGame() {
        events = new ArrayList<>();
        crossesEvents = new ArrayList<>();
        zeroesEvents = new ArrayList<>();
        currentTurn = 0;
        currentMiniturn = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = new Cell(CellType.EMPTY, false);
            }
        }
        previousTurnSkipped = false;
        currentPlayerFigure = PlayerFigure.CROSS;
        currentGameState = GameState.RUNNING;
        updateGameState();
    }

    public GameState getCurrentGameState() {
        return currentGameState;
    }

    static boolean isInside(int pos) {
        return pos >= 0 && pos < BOARD_SIZE;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public int getCurrentMiniturn() {
        return currentMiniturn;
    }

    public int getGameStateAsInt() {
        return currentGameState.ordinal();
    }

    private void updateAdjacentCells(int x, int y) {
        board[x][y].isActive = true;
        for (int[] adjacentDirection : ADJACENT_DIRECTIONS) {
            int dx = x + adjacentDirection[0], dy = y + adjacentDirection[1];
            if (isInside(dx) && isInside(dy)) {
                if ((board[dx][dy].getOwner() == getOpponent(currentPlayerFigure) && !board[dx][dy].isDead()) ||
                        board[dx][dy].cellType == CellType.EMPTY) {
                    board[dx][dy].canMakeTurn = true;
                }

                if (!board[dx][dy].isActive) {
                    if (board[dx][dy].isDead() && board[dx][dy].getOwner() == currentPlayerFigure) {
                        updateAdjacentCells(dx, dy);
                    }
                }
            }
        }
    }

    public Cell getCellAt(int x, int y) {
        return board[x][y];
    }

    public PlayerFigure getOpponent(PlayerFigure curPlayerFigure) {
        switch (curPlayerFigure) {
            case CROSS:
                return PlayerFigure.ZERO;
            case ZERO:
                return PlayerFigure.CROSS;
            default:
                return PlayerFigure.NONE;
        }
    }

    private void updateGameState() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j].canMakeTurn = false;
                board[i][j].isActive = false;
            }
        }

        boolean isCrossAlive = false, isZeroAlive = false;

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (!board[i][j].isDead() && board[i][j].cellType != CellType.EMPTY) {
                    if (board[i][j].getOwner() == PlayerFigure.CROSS) {
                        isCrossAlive = true;
                    } else {
                        isZeroAlive = true;
                    }
                }
            }
        }

        if (currentGameState != GameState.RUNNING) {
            return;
        }

        if (currentTurn > 1) {
            if (!isCrossAlive) {
                zeroWon();
            }
            if (!isZeroAlive) {
                crossWon();
            }
        }

        if (currentPlayerFigure != PlayerFigure.NONE) {
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (!board[i][j].isActive && board[i][j].getOwner() == currentPlayerFigure && !board[i][j].isDead()) {
                        updateAdjacentCells(i, j);
                    }
                }
            }
        }

        if (currentTurn == 0 && currentMiniturn == 0) {
            board[0][0].canMakeTurn = true;
        }
        if (currentTurn == 1 && currentMiniturn == 0) {
            board[BOARD_SIZE - 1][BOARD_SIZE - 1].canMakeTurn = true;
        }

    }

    //returns true is can pass turn (it's beginning of player's turn), false otherwise
    //probably it's better just to not allow to press 'pass' button

    public boolean isFinished() {
        return currentGameState == GameState.CROSS_WON ||
                currentGameState == GameState.ZERO_WON ||
                currentGameState == GameState.DRAW;
    }

    public boolean skipTurn() {
        if (currentMiniturn != 0 || currentTurn < 2) {
            return false;
        }

        storeEvent(GameEvent.newSkipTurnEvent(events.size()));

        if (previousTurnSkipped) {
            draw();
            return true;
        }

        previousTurnSkipped = true;
        currentMiniturn = 2;
        passTurn();
        return true;
    }

    private void storeEvent(GameEvent event) {
        events.add(event);
        switch (currentPlayerFigure) {
            case CROSS:
                crossesEvents.add(event);
                break;
            case ZERO:
                zeroesEvents.add(event);
                break;
        }
    }

    //returns true if turn was correct, false otherwise
    public boolean doTurn(int x, int y) {
        if (!board[x][y].canMakeTurn || currentGameState != GameState.RUNNING) {
            return false;
        }
        storeEvent(GameEvent.newTurnEvent(x, y, events.size()));
        previousTurnSkipped = false;

        if (board[x][y].cellType != CellType.EMPTY) {
            board[x][y].setOwnerOnDead(currentPlayerFigure);
        } else {
            board[x][y].setOwner(currentPlayerFigure);
        }

        passTurn();
        return true;
    }

    public boolean giveUp(PlayerFigure whoGivesUp) {
        switch (whoGivesUp) {
            case CROSS:
                storeEvent(GameEvent.newGiveUpEvent(PlayerFigure.CROSS, events.size()));
                zeroWon();
                return true;
            case ZERO:
                storeEvent(GameEvent.newGiveUpEvent(PlayerFigure.ZERO, events.size()));
                crossWon();
                return true;
            default:
                return false;
        }
    }

    private void draw() {
        currentGameState = GameState.DRAW;
        currentPlayerFigure = PlayerFigure.NONE;
        updateGameState();
    }

    private void crossWon() {
        currentGameState = GameState.CROSS_WON;
        currentPlayerFigure = PlayerFigure.NONE;
        updateGameState();
    }

    private void zeroWon() {
        currentGameState = GameState.ZERO_WON;
        currentPlayerFigure = PlayerFigure.NONE;
        updateGameState();
    }

    private void passTurn() {
        currentMiniturn++;
        if (currentMiniturn == 3 ) {
            currentTurn++;
            currentMiniturn = 0;
            currentPlayerFigure = getOpponent(currentPlayerFigure);
        }

        updateGameState();

        //to avoid recursion when game is over and both of the players pass turn because they can't make any move
        if (currentGameState != GameState.RUNNING) {
            return;
        }

        //if player can't move at the beginning of his turn, he should manually press "skip turn" button
        if (!canMove() && currentMiniturn != 0) {
            currentMiniturn = 2;
            passTurn();
        }
    }

    //should be used very carefully, as it might broke some game logic.
    //It's public for easier AI implementations
    public void setCurrentPlayerToOpponent() {
        currentPlayerFigure = getOpponent(currentPlayerFigure);
        updateGameState();
    }

    public static class CoordinatePair {
        int x, y;
        CoordinatePair(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    ArrayList<CoordinatePair> getMoves(){
        ArrayList<CoordinatePair> moves = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j].canMakeTurn) {
                    moves.add(new CoordinatePair(i, j));
                }
            }
        }
        return moves;
    }

    public boolean canMove() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j].canMakeTurn) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<GameEvent> getLastEventsBy(PlayerFigure figure) {
        List<GameEvent> result = new ArrayList<>();
        if (currentPlayerFigure == figure) {
            return result;
        }
        List<GameEvent> source;
        switch (figure) {
            case CROSS:
                source = crossesEvents;
                break;
            case ZERO:
                source = zeroesEvents;
                break;
            default:
                return result;
        }
        ListIterator<GameEvent> it = source.listIterator(source.size());
        int lastEventNo = -1;
        while (it.hasPrevious()) {
            GameEvent event = it.previous();
            if (lastEventNo == -1 || event.getNumber() == lastEventNo - 1) {
                result.add(event);
                lastEventNo = event.getNumber();
            } else {
                break;
            }
            if (event.type != GameEvent.GameEventType.TURN_EVENT) {
                break;
            }
        }
        return result;
    }
}
