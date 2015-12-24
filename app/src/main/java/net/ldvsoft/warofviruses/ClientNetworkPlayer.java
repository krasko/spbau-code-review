package net.ldvsoft.warofviruses;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Created by Сева on 13.12.2015.
 */
public class ClientNetworkPlayer extends Player {
    private static final Gson gson = new Gson();

    private Context context;
    private GoogleCloudMessaging gcm;
    private BroadcastReceiver turnMessageReceiver;

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

    public static ClientNetworkPlayer deserialize(User user, GameLogic.PlayerFigure ownFigure, Context context) {
        return new ClientNetworkPlayer(user, ownFigure, context);
    }

    public ClientNetworkPlayer(User user, final GameLogic.PlayerFigure ownFigure, Context context) {
        this.user = user;
        this.ownFigure = ownFigure;
        this.context = context;
        this.type = 0;
        gcm = GoogleCloudMessaging.getInstance(context);
        turnMessageReceiver = new BroadcastReceiver() {
            @Override
            public synchronized void onReceive(Context context, Intent intent) {
                String data = intent.getBundleExtra(WoVPreferences.TURN_BUNDLE).getString(WoVProtocol.DATA);
                if (data == null) {
                    throw new IllegalArgumentException("Missing data field in TURN_BUNDLE!");
                }
                JsonObject jsonData = (JsonObject) new JsonParser().parse(data);
                GameEvent event = gson.fromJson(jsonData.get(WoVProtocol.EVENT), GameEvent.class);
                pendingEvents.add(event);
                while (!pendingEvents.isEmpty() && pendingEvents.first().getNumber() == game.getAwaitingEventNumber()) {
                    event = pendingEvents.pollFirst();
                    switch (event.type) {
                        case TURN_EVENT:
                            game.doTurn(ClientNetworkPlayer.this, event.getTurnX(), event.getTurnY());
                            break;

                        case CROSS_GIVE_UP_EVENT:
                            if (ownFigure != GameLogic.PlayerFigure.CROSS) {
                                throw new IllegalArgumentException("Expected ZERO_GIVE_UP_EVENT, found CROSS_GIVE_UP_EVENT!");
                            }
                            game.giveUp(ClientNetworkPlayer.this);
                            break;

                        case ZERO_GIVE_UP_EVENT:
                            if (ownFigure != GameLogic.PlayerFigure.CROSS) {
                                throw new IllegalArgumentException("Expected CROSS_GIVE_UP_EVENT, found ZERO_GIVE_UP_EVENT!");
                            }
                            game.giveUp(ClientNetworkPlayer.this);
                            break;

                        case SKIP_TURN_EVENT:
                            game.skipTurn(ClientNetworkPlayer.this);
                            break;
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        };
        context.registerReceiver(turnMessageReceiver, new IntentFilter(WoVPreferences.TURN_BROADCAST));
    }

    @Override
    public void makeTurn() {
        //nothing to do
    }

    @Override
    public void onGameStateChanged(GameEvent event, Player whoChanged) {
        if (equals(whoChanged)) {
            return;
        }

        JsonObject data = new JsonObject();
        data.add(WoVProtocol.EVENT, gson.toJsonTree(event));
        Bundle message = new Bundle();
        message.putString(WoVProtocol.ACTION, WoVProtocol.ACTION_TURN);
        message.putString(WoVProtocol.DATA, data.toString());
        String id = UUID.randomUUID().toString();
        try {
            gcm.send(context.getString(R.string.gcm_defaultSenderId) + "@gcm.googleapis.com", id, message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        context.unregisterReceiver(turnMessageReceiver);
    }

    @Override
    public void updateGameInfo(Game game) {
        String id = UUID.randomUUID().toString();
        Bundle message = new Bundle();
        message.putString(WoVProtocol.ACTION, WoVProtocol.ACTION_UPDATE_LOCAL_GAME);
        try {
            gcm.send(context.getString(R.string.gcm_defaultSenderId) + "@gcm.googleapis.com", id, message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
