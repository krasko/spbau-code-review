package net.ldvsoft.warofviruses;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static net.ldvsoft.warofviruses.WoVProtocol.ACTION;
import static net.ldvsoft.warofviruses.WoVProtocol.ACTION_PING;
import static net.ldvsoft.warofviruses.WoVProtocol.ACTION_TURN;
import static net.ldvsoft.warofviruses.WoVProtocol.ACTION_UPDATE_LOCAL_GAME;
import static net.ldvsoft.warofviruses.WoVProtocol.ACTION_USER_READY;
import static net.ldvsoft.warofviruses.WoVProtocol.PING_ID;
import static net.ldvsoft.warofviruses.WoVProtocol.RESULT;
import static net.ldvsoft.warofviruses.WoVProtocol.RESULT_SUCCESS;

public final class WarOfVirusesServer {
//    protected static final JsonObject JSON_RESULT_FAILURE = new JsonObject();
//
//    static {
//        JSON_RESULT_FAILURE.addProperty(RESULT, RESULT_FAILURE);
//    }

    private static final String DEFAULT_CONFIG_FILE = "/etc/war-of-viruses-server.conf";

    private Logger logger;
    private Properties config = new Properties();

    private GCMHandler gcmHandler;
    private DatabaseHandler databaseHandler;

    private User waitingForGame = null;
    private Game runningGame = null;

    public String getSetting(String name) {
        return config.getProperty(name, "");
    }

    public GCMHandler getGcmHandler() {
        return gcmHandler;
    }

    /**
     * Process incoming via GCM message from client.
     * If simple answer is required, returns an answer.
     * May return null, but then client will get generic answer.
     * @param message Message from the client.
     * @return (Optional) Answer to client.
     */
    public JsonObject processGCM(JsonObject message) {
        String action = message.get("data").getAsJsonObject().get(ACTION).getAsString();
        switch (action) {
            case ACTION_PING:
                return processPing(message);
            case ACTION_USER_READY:
                return processUserReady(message);
            case ACTION_TURN:
                return processTurn(message);
            case ACTION_UPDATE_LOCAL_GAME:
                return processUpdateLocalGame(message);
            default:
                return null;
        }
    }

    private JsonObject processUpdateLocalGame(JsonObject message) {
        User sender = databaseHandler.getUserByToken(message.get("from").getAsString());
        if (sender.getId() == runningGame.getCrossPlayer().getUser().getId()) {
            ((ServerNetworkPlayer) runningGame.getCrossPlayer()).sendGameInfo(); //how about more elegant solution?
        } else {
            ((ServerNetworkPlayer) runningGame.getZeroPlayer()).sendGameInfo();
        }
        return null;
    }

    private JsonObject processPing(JsonObject message) {
        String sender = message.get("from").getAsString();
        String messageId = message.get("message_id").getAsString();

        JsonObject answer = new JsonObject();
        answer.addProperty(RESULT, RESULT_SUCCESS);
        answer.addProperty(PING_ID, messageId);

        gcmHandler.sendDownstreamMessage(gcmHandler.createJsonMessage(
                        sender,
                        gcmHandler.nextMessageId(),
                        RESULT_SUCCESS,
                        answer,
                        null,
                        null,
                        false,
                        "high")
        );
        return null;
    }

    private JsonObject processUserReady(JsonObject message) {
        User sender = databaseHandler.getUserByToken(message.get("from").getAsString());
        if (waitingForGame == null) {
            logger.log(Level.INFO, "User " + sender.getId() + " started waiting.");
            waitingForGame = sender;
        } else {
            logger.log(Level.INFO, "User " + sender.getId() + " came to start the game.");
            runningGame = new Game();
            runningGame.startNewGame(
                    new ServerNetworkPlayer(sender, waitingForGame, this, GameLogic.PlayerFigure.CROSS),
                    new ServerNetworkPlayer(waitingForGame, sender, this, GameLogic.PlayerFigure.ZERO));
            waitingForGame = null;
        }
        return null;
    }

    private JsonObject processTurn(JsonObject message) {
        User sender = databaseHandler.getUserByToken(message.get("from").getAsString());
        JsonObject data = (JsonObject) new JsonParser().parse(
                message.get("data").getAsJsonObject().get("data").getAsString()
        );
        if (runningGame.getCrossPlayer().getUser().getId() == sender.getId()) {
            ((ServerNetworkPlayer) runningGame.getCrossPlayer()).performMove(data);
        } else {
            ((ServerNetworkPlayer) runningGame.getZeroPlayer()).performMove(data);
        }
        return null;
    }

    /**
     * Sends message to user
     * @param user target user id
     * @param action action specifier
     * @param data additional JSON data, specifying action params
     */
    public boolean sendToUser(User user, String action, JsonObject data) {
        List<String> tokens = databaseHandler.getTokens(user.getId());
        boolean success = true;
        for (String token : tokens) {
            success = success && gcmHandler.sendDownstreamMessage(gcmHandler.createJsonMessage(
                    token, gcmHandler.nextMessageId(), action, data, null, null, false, "high"
            ));
        }
        return success;
    }

    public void run() {
        String configFile = System.getenv("CONFIG");
        if (configFile == null) {
            configFile = DEFAULT_CONFIG_FILE;
        }

        logger = Logger.getLogger(WarOfVirusesServer.class.getName());
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(configFile));
            config.load(new FileReader(configFile));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot open config file or it's contents are wrong.", e);
            System.exit(1);
        }
        logger = Logger.getLogger(WarOfVirusesServer.class.getName());

        try {
            databaseHandler = new DatabaseHandler(this);
            gcmHandler = new GCMHandler(this);

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    gcmHandler.stop();
                    databaseHandler.stop();
                }
            }));

            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Server failed", e);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        WarOfVirusesServer instance = new WarOfVirusesServer();
        instance.run();
    }
}
