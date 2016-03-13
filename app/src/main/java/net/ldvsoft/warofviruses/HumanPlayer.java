package net.ldvsoft.warofviruses;

import android.content.Context;

/**
 * Created by Сева on 21.10.2015.
 */
public class HumanPlayer extends Player {
    private OnGameStateChangedListener onGameStateChangedListener = null;

    public void setOnGameStateChangedListener(OnGameStateChangedListener onGameStateChangedListener) {
        this.onGameStateChangedListener = onGameStateChangedListener;
    }

    public interface OnGameStateChangedListener {
        void onGameStateChanged(GameLogic gameLogic);
    }

    public static final User USER_ANONYMOUS = new User(
            DBProvider.USER_ANONYMOUS_ID,
            WoVPreferences.ANONYMOUS_GOOGLE_TOKEN,
            WoVPreferences.ANONYMOUS_NICKNAME_STR, WoVPreferences.ANONYMOUS_NICKNAME_ID,
            WoVPreferences.DEFAULT_CROSS_COLOR, WoVPreferences.DEFAULT_ZERO_COLOR,
            null);

    public static HumanPlayer deserialize(User user, GameLogic.PlayerFigure ownFigure, Context context) {
        return new HumanPlayer(user, ownFigure);
    }

    public HumanPlayer(User user, GameLogic.PlayerFigure ownFigure) {
        this.user = user;
        this.ownFigure = ownFigure;
        this.type = 0;
    }

    public HumanPlayer(User user, GameLogic.PlayerFigure ownFigure, OnGameStateChangedListener onGameStateChangedListener) {
        this.user = user;
        this.ownFigure = ownFigure;
        this.onGameStateChangedListener = onGameStateChangedListener;
    }

    @Override
    public void makeTurn() {
        if (onGameStateChangedListener != null) {
            onGameStateChangedListener.onGameStateChanged(game.getGameLogic());
        }
    }

    @Override
    public void onGameStateChanged(GameEvent event, Player whoChanged) {
        if (onGameStateChangedListener != null) {
            onGameStateChangedListener.onGameStateChanged(game.getGameLogic());
        }
    }
}
