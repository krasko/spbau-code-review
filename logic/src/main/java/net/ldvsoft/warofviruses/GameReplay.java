package net.ldvsoft.warofviruses;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Class that provides interaction between saved finished game and human, allowing to view replay of
 * that games.
 */
public class GameReplay {
    ArrayList<GameLogic> gameStates;
    private int currentEventNumber;
    private Player crossPlayer;
    private Player zeroPlayer;

    /**
     * Constructor for gameReplay
     * @param gameEventHistory history of events from GameLogic class that should be reproduced
     */
    GameReplay(List<GameEvent> gameEventHistory, Player crossPlayer, Player zeroPlayer) {
        GameLogic gameLogic = new GameLogic();
        gameLogic.newGame();
        this.crossPlayer = crossPlayer;
        this.zeroPlayer = zeroPlayer;
        gameStates = new ArrayList<>();
        gameStates.add(new GameLogic(gameLogic));
        for (GameEvent event : gameEventHistory) {
            event.applyEvent(gameLogic);
            gameStates.add(new GameLogic(gameLogic));
        }
        currentEventNumber = 0;
    }

    /**
     * Sets internal state to beginning of game(empty field)
     */
    public void toBeginOfGame() {
        currentEventNumber = 0;
    }

    /**
     * Sets internal state to the end of game
     */
    public void toEndOfGame() {
        currentEventNumber = gameStates.size() - 1;
    }

    /**
     * Applies next event to the internal state
     */
    public void nextEvent() {
        currentEventNumber = min(currentEventNumber + 1, gameStates.size() - 1);
    }

    /**
     * Cancel last event
     */
    public void prevEvent() {
        currentEventNumber = max(0, currentEventNumber - 1);
    }

    public int getEventCount() {
        return gameStates.size();
    }

    public int getCurrentEventNumber() {
        return currentEventNumber + 1;
    }

    /**
     *
     * @return copy of the current internal gameLogic state that is changed during calls of toBeginOfGame(),
     * toEndOfGame(), nextEvent(), prevEvent()
     */
    public GameLogic getGameLogic() {
        return new GameLogic(gameStates.get(currentEventNumber));
    }

    public Player getCrossPlayer() {
        return crossPlayer;
    }

    public Player getZeroPlayer() {
        return zeroPlayer;
    }
}
