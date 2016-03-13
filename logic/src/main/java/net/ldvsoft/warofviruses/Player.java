package net.ldvsoft.warofviruses;

import java.io.Serializable;

/**
 * Created by Сева on 20.10.2015.
 */
public abstract class Player implements Serializable {
    protected GameLogic.PlayerFigure ownFigure;
    protected User user;
    protected Game game;
    protected int type;
    public abstract void makeTurn();

    public String getName() {
        return user.getFullNickname();
    }

    public abstract void onGameStateChanged(GameEvent event, Player whoChanged);

    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Player && user.getId() == ((Player) obj).user.getId();
    }

    public User getUser() {
        return user;
    }

    /*
    Is called when player should finish its work because of destroying of game
     */
    public void onStop() {

    }
    /*
    Method that is called when game needs to be updated (by server, for example)
     */
    public void updateGameInfo(Game game) {

    }
}
