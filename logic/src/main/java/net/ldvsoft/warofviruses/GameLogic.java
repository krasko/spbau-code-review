package net.ldvsoft.warofviruses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Сева on 21.10.2015.
 */
public class GameLogic {
    private List<GameEvent> events = new ArrayList<>();
    private List<GameEvent> crossesEvents = new ArrayList<>();
    private List<GameEvent> zeroesEvents = new ArrayList<>();

    public static final int BOARD_SIZE = 10;
    private boolean isPlayerBlocked = false;

    public List<GameEvent> getEventHistory() {
        return events;
    }

    public enum CellType {
        CROSS, ZERO, DEAD_CROSS, DEAD_ZERO, EMPTY;

        public PlayerFigure getOwner() {
            switch (this) {
                case CROSS:
                case DEAD_ZERO:
                    return PlayerFigure.CROSS;
                case ZERO:
                case DEAD_CROSS:
                    return PlayerFigure.ZERO;
                case EMPTY:
                default:
                    return PlayerFigure.NONE;
            }
        }
    }

    public enum PlayerFigure {CROSS, ZERO, NONE}
    public enum GameState {NOT_RUNNING, RUNNING, DRAW, CROSS_WON, ZERO_WON}

    public static class Cell {
        private CellType cellType = CellType.EMPTY;
        private boolean canMakeTurn = false;
        private boolean isActive = false;

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

        public boolean isActive() {
            return isActive;
        }

        private void setOwner(PlayerFigure newOwner) {
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

        private void setOwnerOnDead(PlayerFigure newOwner) {
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
        for (GameEvent event : events) {
            logic = event.applyEvent(logic);
        }
        return logic;
    }

    public GameLogic() {
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

    public GameState getCurrentGameState() {
        return currentGameState;
    }

    static private boolean isInside(int pos) {
        return pos >= 0 && pos < BOARD_SIZE;
    }

    public boolean isBlocked() {
        return isPlayerBlocked;
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
            int newX = x + adjacentDirection[0], newY = y + adjacentDirection[1];
            if (isInside(newX) && isInside(newY)) {
                if ((board[newX][newY].cellType.getOwner() == getOpponentPlayerFigure(currentPlayerFigure) && !board[newX][newY].isDead()) ||
                        board[newX][newY].cellType == CellType.EMPTY) {
                    board[newX][newY].canMakeTurn = true;
                }

                if (!board[newX][newY].isActive) {
                    if (board[newX][newY].isDead() && board[newX][newY].cellType.getOwner() == currentPlayerFigure) {
                        updateAdjacentCells(newX, newY);
                    }
                }
            }
        }
    }

    public Cell getCellAt(int x, int y) {
        return board[x][y];
    }

    public void blockCurrentPlayer() {
        isPlayerBlocked = true;
        updateGameState();
    }

    public static PlayerFigure getOpponentPlayerFigure(PlayerFigure curPlayerFigure) {
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
                    if (board[i][j].cellType.getOwner() == PlayerFigure.CROSS) {
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
        if (isPlayerBlocked) {
            return;
        }

        if (currentPlayerFigure != PlayerFigure.NONE) {
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (!board[i][j].isActive && board[i][j].cellType.getOwner() == currentPlayerFigure && !board[i][j].isDead()) {
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

    public GameLogic skipTurn() {
        if (isPlayerBlocked || currentMiniturn != 0 || currentTurn < 2) {
            return null;
        }
        GameLogic result = new GameLogic(this);
        result.storeEvent(GameEvent.newSkipTurnEvent(events.size()));

        if (previousTurnSkipped) {
            result.draw();
            return result;
        }

        result.previousTurnSkipped = true;
        result.currentMiniturn = 2;
        result.passTurn();
        return result;
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

    //returns new instance of GameLogic with applied turn if it was correct, null otherwise. Does not modifies current instance
    public GameLogic doTurn(int x, int y) {
        if (isPlayerBlocked || !board[x][y].canMakeTurn || currentGameState != GameState.RUNNING) {
            return null;
        }
        GameLogic result = new GameLogic(this);
        result.storeEvent(GameEvent.newTurnEvent(x, y, events.size()));
        result.previousTurnSkipped = false;

        if (result.board[x][y].cellType != CellType.EMPTY) {
            result.board[x][y].setOwnerOnDead(currentPlayerFigure);
        } else {
            result.board[x][y].setOwner(currentPlayerFigure);
        }

        result.passTurn();
        return result;
    }

    public GameLogic giveUp(PlayerFigure whoGivesUp) {
        if (isPlayerBlocked) {
            return null;
        }
        GameLogic result = new GameLogic(this);
        switch (whoGivesUp) {
            case CROSS:
                result.storeEvent(GameEvent.newGiveUpEvent(PlayerFigure.CROSS, events.size()));
                result.zeroWon();
                return result;
            case ZERO:
                result.storeEvent(GameEvent.newGiveUpEvent(PlayerFigure.ZERO, events.size()));
                result.crossWon();
                return result;
            default:
                return null;
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
            currentPlayerFigure = getOpponentPlayerFigure(currentPlayerFigure);
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
        currentPlayerFigure = getOpponentPlayerFigure(currentPlayerFigure);
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

    public GameEvent getLastEvent() {
        if (events.isEmpty()) {
            return null;
        }
        return events.get(events.size() - 1);
    }

    public List<GameEvent> getLastEventsBy(PlayerFigure figure) {
        List<GameEvent> result = new ArrayList<>();
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
        Collections.reverse(result);
        return result;
    }
}
