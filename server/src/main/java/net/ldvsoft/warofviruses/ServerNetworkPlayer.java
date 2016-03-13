package net.ldvsoft.warofviruses;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Created by ldvsoft on 14.12.15.
 */
public class ServerNetworkPlayer extends Player {
    private static final Gson gson = new Gson();

    private final User opponent;
    private WarOfVirusesServer server;

    public ServerNetworkPlayer(User user, User opponent, WarOfVirusesServer server, GameLogic.PlayerFigure figure) {
        this.user = user;
        this.opponent = opponent;
        this.server = server;
        this.ownFigure = figure;
    }

    @Override
    public void makeTurn() {
        server.sendToUser(user, WoVProtocol.ACTION_TURN, WoVProtocol.eventsToJson(game.getGameLogic()
                .getLastEventsBy(GameLogic.getOpponentPlayerFigure(ownFigure))));
    }

    @Override
    public void onGameStateChanged(GameEvent event, Player whoChanged) {
        //Nothing to do
    }

    @Override
    public void setGame(Game game) {
        super.setGame(game);
        //Send client that game has started
        sendGameInfo();
    }

    public void sendGameInfo() {
        JsonObject message = WoVProtocol.eventsToJson(game.getGameLogic().getEventHistory());;
        message.add(WoVProtocol.MY_FIGURE , gson.toJsonTree(ownFigure));
        message.add(WoVProtocol.GAME_ID, gson.toJsonTree(game.getGameId()));
        switch (ownFigure) {
            case CROSS:
                message.add(WoVProtocol.CROSS_USER, gson.toJsonTree(user));
                message.add(WoVProtocol.ZERO_USER , gson.toJsonTree(opponent));
                break;
            case ZERO:
                message.add(WoVProtocol.ZERO_USER , gson.toJsonTree(user));
                message.add(WoVProtocol.CROSS_USER, gson.toJsonTree(opponent));
                break;
        }
        server.sendToUser(user, WoVProtocol.GAME_LOADED, message);
    }

    public synchronized void performMove(JsonObject message) {
        List<GameEvent> events = WoVProtocol.eventsFromJson(message);
        game.applyPlayerEvents(events, ServerNetworkPlayer.this);
    }
}
