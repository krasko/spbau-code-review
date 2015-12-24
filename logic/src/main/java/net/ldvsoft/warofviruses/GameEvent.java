package net.ldvsoft.warofviruses;

import java.io.Serializable;

/**
 * Created by Сева on 05.12.2015.
 */
public class GameEvent implements Serializable {

    public enum GameEventType {TURN_EVENT, SKIP_TURN_EVENT, CROSS_GIVE_UP_EVENT, ZERO_GIVE_UP_EVENT};

    private int turnX, turnY;
    private int number;
    GameEventType type;
    public GameEvent(int turnX, int turnY, int number, GameEventType type) {
        this.turnX = turnX;
        this.turnY = turnY;
        this.number = number;
        this.type = type;
    }

    public static GameEvent deserialize(int type, int turnX, int turnY, int number) {
        return new GameEvent(turnX, turnY, number, GameEventType.values()[type]);
    }

    public int getNumber() {
        return number;
    }

    int getTurnX() {
        return turnX;
    }

    int getTurnY() {
        return turnY;
    }

    int getEventTypeAsInt() {
        return type.ordinal();
    }

    static GameEvent newGiveUpEvent(GameLogic.PlayerFigure whoGivesUp, int number) {
        switch (whoGivesUp) {
            case CROSS:
                return new GameEvent(-1, -1, number, GameEventType.CROSS_GIVE_UP_EVENT);
            case ZERO:
                return new GameEvent(-1, -1, number, GameEventType.ZERO_GIVE_UP_EVENT);
            default:
                throw new IllegalArgumentException("Illegal figure type!");
        }
    }

    static GameEvent newSkipTurnEvent(int number) {
        return new GameEvent(-1, -1, number, GameEventType.SKIP_TURN_EVENT);
    }

    static GameEvent newTurnEvent(int turnX, int turnY, int number) {
        return new GameEvent(turnX, turnY, number, GameEventType.TURN_EVENT);
    }

    public void applyEvent(GameLogic logic) {
        switch(type) {
            case TURN_EVENT:
                logic.doTurn(turnX, turnY);
                break;

            case SKIP_TURN_EVENT:
                logic.skipTurn();
                break;

            case CROSS_GIVE_UP_EVENT:
                logic.giveUp(GameLogic.PlayerFigure.CROSS);
                break;

            case ZERO_GIVE_UP_EVENT:
                logic.giveUp(GameLogic.PlayerFigure.ZERO);
                break;
        }
    }
}
