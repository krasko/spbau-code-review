package net.ldvsoft.warofviruses;

/**
 * Created by ldvsoft on 21.10.15.
 */
public abstract class WoVPreferences {
    public static final String GCM_SENT_TOKEN_TO_SERVER = "GCM_SENT_TOKEN_TO_SERVER";
    public static final String GCM_REGISTRATION_COMPLETE = "GCM_REGISTRATION_COMPLETE";
    public static final String REPLAY_GAME_ID = "replayGameId";
    public static final String REPLAY_GAME_TURN = "replayGameTurn";
    public static final String TURN_BROADCAST = "turnBroadcast";
    public static final String TURN_BUNDLE = "turnBundle";
    public static final String GAME_LOADED_FROM_SERVER_BROADCAST = "gameLoadedFromServerBroadcast";
    public static final String GAME_BUNDLE = "gameBundle";
    public static final String GAME_JSON_DATA = "gameJsonData";
    public static final int OPPONENT_BOT = 0;
    public static final int OPPONENT_LOCAL_PLAYER = 1;
    public static final int OPPONENT_NETWORK_PLAYER = 2;
    public static final int OPPONENT_RESTORED_GAME = 3;
    public static final String OPPONENT_TYPE = "OPPONENT_TYPE";
    public static final String CURRENT_USER_ID = "currentUserID";
    public static final String PREFERENCES = "Preferences";
}
