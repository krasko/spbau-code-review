package net.ldvsoft.warofviruses;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * Created by ldvsoft on 14.12.15.
 */
public class ServerNetworkPlayer extends Player {
    private static final Gson gson = new Gson();

    private final User opponent;
    private WarOfVirusesServer server;

    private final TreeSet<GameEvent> pendingEvents = new TreeSet<>(new Comparator<GameEvent>() {
        @Override
        public int compare(GameEvent x, GameEvent y) {
            int xNum = x.getNumber(), yNum = y.getNumber();
            if (xNum < yNum) {
                return -1;
            }
            return xNum == yNum ? 0 : 1;
        }
    });

    public ServerNetworkPlayer(User user, User opponent, WarOfVirusesServer server, GameLogic.PlayerFigure figure) {
        this.user = user;
        this.opponent = opponent;
        this.server = server;
        this.ownFigure = figure;
    }

    @Override
    public void makeTurn() {
        //Nothing to do
    }

    @Override
    public void onGameStateChanged(GameEvent event, Player whoChanged) {
        if (equals(whoChanged)) {
            return;
        }
        JsonObject message = new JsonObject();
        message.add(WoVProtocol.EVENT, gson.toJsonTree(event));
        server.sendToUser(user, WoVProtocol.ACTION_TURN, message);
    }

    @Override
    public void setGame(Game game) {
        super.setGame(game);
        //Send client that game has started
        sendGameInfo();
    }

    public void sendGameInfo() {
        JsonObject message = new JsonObject();
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
        message.add(WoVProtocol.TURN_ARRAY, gson.toJsonTree(WoVProtocol.getIntsFromEventArray(game.getGameLogic().getEventHistory())));
        server.sendToUser(user, WoVProtocol.GAME_LOADED, message);
    }

    public synchronized void performMove(JsonObject message) {
        GameEvent event = gson.fromJson(message.get(WoVProtocol.EVENT), GameEvent.class);
        pendingEvents.add(event);
        while (!pendingEvents.isEmpty() && pendingEvents.first().getNumber() == game.getAwaitingEventNumber()) {
            event = pendingEvents.pollFirst();
            switch (event.type) {
                case TURN_EVENT:
                    game.doTurn(this, event.getTurnX(), event.getTurnY());
                    break;
                case CROSS_GIVE_UP_EVENT:
                    game.giveUp(ownFigure == GameLogic.PlayerFigure.CROSS ? this : game.getZeroPlayer());
                    break;
                case ZERO_GIVE_UP_EVENT:
                    game.giveUp(ownFigure == GameLogic.PlayerFigure.ZERO ? this : game.getCrossPlayer());
                    break;
                case SKIP_TURN_EVENT:
                    game.skipTurn(this);
                    break;
            }
        }
    }
}
