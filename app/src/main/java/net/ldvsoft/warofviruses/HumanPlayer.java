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
            DBProvider.USER_ANONYMOUS,
            "uniqueGoogleTokenForAnonymousPlayer",
            //0, //DBOpenHelper.playerClasses[0]
            "Anonymous", 1,
            30, 210,
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
//        id = new Random().nextInt();
    }

    @Override
    public void onGameStateChanged(GameEvent event, Player whoChanged) {
        if (onGameStateChangedListener != null) {
            onGameStateChangedListener.onGameStateChanged(game.getGameLogic());
        }
    }
}
