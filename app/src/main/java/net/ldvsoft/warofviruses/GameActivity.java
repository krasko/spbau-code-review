package net.ldvsoft.warofviruses;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static net.ldvsoft.warofviruses.GameLogic.BOARD_SIZE;
import static net.ldvsoft.warofviruses.GameLogic.PlayerFigure.CROSS;
import static net.ldvsoft.warofviruses.GameLogic.PlayerFigure.NONE;
import static net.ldvsoft.warofviruses.GameLogic.PlayerFigure.ZERO;

public class GameActivity extends GameActivityBase {
    private static Gson gson = new Gson();

    private TextView crossNick;
    private TextView zeroNick;
    private TextView gameStatus1;
    private TextView gameStatus2;

    private BroadcastReceiver tokenSentReceiver;
    private BroadcastReceiver gameLoadedFromServerReceiver;
    private Game game = null;
    private static final long NO_GAME_SAVED = -1;
    private static final long DO_NOT_SAVE_GAME = -2;
    private static long lastSavedGameID = NO_GAME_SAVED; //it's somewhat a hack, but it's the simplest solution

    private final HumanPlayer.OnGameStateChangedListener ON_GAME_STATE_CHANGED_LISTENER =
            new HumanPlayer.OnGameStateChangedListener() {
                @Override
                public void onGameStateChanged(final GameLogic gameLogic) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            redrawGame(game);
                        }
                    });
                }
            };
    private HumanPlayer humanPlayer = new HumanPlayer(DBOpenHelper.getInstance(this).getUserById(HumanPlayer.USER_ANONYMOUS.getId()), CROSS,
            ON_GAME_STATE_CHANGED_LISTENER);

    private class OnExitActivityListener implements DialogInterface.OnClickListener {
        private boolean saveGame;
        private boolean giveUp;
        private boolean exitActivity;

        public OnExitActivityListener(boolean saveGame, boolean giveUp, boolean exitActivity) {
            this.saveGame = saveGame;
            this.giveUp = giveUp;
            this.exitActivity= exitActivity;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (giveUp) {
                game.giveUp(humanPlayer);
            }
            if (saveGame) {
                saveCurrentGame();
            }
            if (exitActivity) {
                game = null;
                GameActivity.super.onBackPressed();
            }
        }
    }

    protected void redrawGame(Game game) {
        if (game == null) {
            return;
        }
        super.redrawGame(game.getGameLogic(), humanPlayer.ownFigure);

        crossNick.setText(game.getCrossPlayer().getName());
        zeroNick .setText(game.getZeroPlayer().getName());

        GameLogic.PlayerFigure mineFigure = game.getMineFigure(humanPlayer.getUser().getId());
        GameLogic.GameState gameState = game.getGameState();
        switch (gameState) {
            case RUNNING:
                GameLogic.PlayerFigure currentFigure = game.getCurrentPlayer().ownFigure;
                if (mineFigure == NONE) {
                    switch (currentFigure) {
                        case CROSS:
                            gameStatus1.setText(getString(R.string.GAME_CROSS_TURN));
                            break;
                        case ZERO:
                            gameStatus1.setText(getString(R.string.GAME_ZERO_TURN));
                            break;
                    }
                } else if (currentFigure == mineFigure) {
                    gameStatus1.setText(getString(R.string.GAME_USER_TURN));
                } else {
                    gameStatus1.setText(getString(R.string.GAME_OPPONENT_TURN));
                }
                int miniturnsLeft = 3 - game.getGameLogic().getCurrentMiniturn();
                gameStatus2.setText(String.format(getString(R.string.GAME_MINITURNS_LEFT), miniturnsLeft));
                break;
            case DRAW:
                gameStatus1.setText(getString(R.string.GAME_DRAW));
                gameStatus2.setText(getString(R.string.GAME_OVER));
                break;
            case CROSS_WON:
                if (mineFigure == NONE) {
                    gameStatus1.setText(getString(R.string.GAME_CROSS_WON));
                } else if (mineFigure == CROSS) {
                    gameStatus1.setText(getString(R.string.GAME_WON));
                } else {
                    gameStatus1.setText(getString(R.string.GAME_LOST));
                }
                gameStatus2.setText(getString(R.string.GAME_OVER));
                break;
            case ZERO_WON:
                if (mineFigure == NONE) {
                    gameStatus1.setText(getString(R.string.GAME_ZERO_WON));
                } else if (mineFigure == ZERO) {
                    gameStatus1.setText(getString(R.string.GAME_WON));
                } else {
                    gameStatus1.setText(getString(R.string.GAME_LOST));
                }
                gameStatus2.setText(getString(R.string.GAME_OVER));
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (game == null || game.isFinished()) {
            saveCurrentGame();
            lastSavedGameID = DO_NOT_SAVE_GAME;
            super.onBackPressed();
            return;
        }

        new AlertDialog.Builder(this)
                .setMessage("Do you want to save current game?")
                .setCancelable(false)
                .setPositiveButton("Yes, save and quit!", new OnExitActivityListener(true, false, true))
                .setNeutralButton("Cancel: I don't want to quit", new OnExitActivityListener(false, false, false))
                .setNegativeButton("I want to give up and quit", new OnExitActivityListener(true, true, true))
                        //.setNegativeButton("No, don't save it", new OnExitActivityListener(false, false)) probably I don't need this option...
                .show();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        switch (getResources().getConfiguration().orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                findViewById(R.id.game_bar_replay).setVisibility(View.GONE);
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                findViewById(R.id.game_bar_replay_left ).setVisibility(View.GONE);
                findViewById(R.id.game_bar_replay_right).setVisibility(View.GONE);
                break;
        }

        crossNick   = (TextView) findViewById(R.id.game_cross_nick);
        zeroNick    = (TextView) findViewById(R.id.game_zero_nick);
        gameStatus1 = (TextView) findViewById(R.id.game_text_game_status_1);
        gameStatus2 = (TextView) findViewById(R.id.game_text_game_status_2);

        game = new Game();
        tokenSentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = prefs.getBoolean(WoVPreferences.GCM_SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    Toast.makeText(GameActivity.this, "YEEEEEEEY!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GameActivity.this, "Oh no, Oh no, Oh no-no-no-no(", Toast.LENGTH_SHORT).show();
                }
            }
        };

        Intent intent = getIntent();
        setCurrentGameListeners();
        switch (intent.getIntExtra(WoVPreferences.OPPONENT_TYPE, -1)) {
            case WoVPreferences.OPPONENT_RESTORED_GAME:
                game = null; //will be loaded during onStart()
                break;
            case WoVPreferences.OPPONENT_BOT:
                game.startNewGame(humanPlayer, new AIPlayer(ZERO));
                break;
            case WoVPreferences.OPPONENT_LOCAL_PLAYER:
                game.startNewGame(humanPlayer, new HumanPlayer(humanPlayer.getUser(), ZERO));
                break;
            case WoVPreferences.OPPONENT_NETWORK_PLAYER:
                loadGameFromJson(intent.getStringExtra(WoVPreferences.GAME_JSON_DATA));
                break;
            default:
                Log.wtf("GameActivityBase", "Could not start new game: incorrect opponent type");
        }
        initButtons();
        if (game != null) {
            redrawGame(game);
        }
    }

    @Override
    protected void onPause() {
        Log.d("GameActivityBase", "onPause");
        if (game != null) {
            saveCurrentGame();
            game.onStop();
        }
        unregisterReceiver(gameLoadedFromServerReceiver);

        LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(tokenSentReceiver);
        super.onPause();
    }

    private class OnSkipTurnListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    if (!game.skipTurn(humanPlayer)) {
                        return null;
                    }
                    return null;
                }
            }.execute();

        }
    }

    private class OnGiveUpListener implements  View.OnClickListener {

        @Override
        public void onClick(View v) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    game.giveUp(humanPlayer);
                    return null;
                }
            }.execute();
        }
    }

    private class OnBoardClickListener implements View.OnClickListener {
        private final int x, y;

        OnBoardClickListener(int x, int y) {
            this.x = x;
            this.y = y;
        }
        @Override
        public void onClick(View v) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    game.doTurn(humanPlayer, x, y);
                    return null;
                }
            }.execute();
        }
    }

    private void initButtons() {
        Button skipTurnButton = (Button) findViewById(R.id.game_button_passturn);
        skipTurnButton.setOnClickListener(new OnSkipTurnListener());
        Button giveUpButton = (Button) findViewById(R.id.game_button_giveup);
        giveUpButton.setOnClickListener(new OnGiveUpListener());
        if (game != null) {
            BoardCellButton.loadDrawables(this, game.getCrossPlayer().getUser().getColorCross(),
                    game.getZeroPlayer().getUser().getColorZero());
        }
        for (int i = 0; i != BOARD_SIZE; i++)
            for (int j = 0; j != BOARD_SIZE; j++) {
                boardButtons[i][j].setOnClickListener(new OnBoardClickListener(i, j));
            }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //lastSavedGameID = DO_NOT_SAVE_GAME;
    }

    protected void onResume() {
        super.onResume();
        new StoredGameLoader().execute();
        gameLoadedFromServerReceiver = new GameLoadedFromServerReceiver();
        registerReceiver(gameLoadedFromServerReceiver, new IntentFilter(WoVPreferences.GAME_LOADED_FROM_SERVER_BROADCAST));

        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(tokenSentReceiver, new IntentFilter(WoVPreferences.GCM_REGISTRATION_COMPLETE));
    }

    private void setCurrentGameListeners() {
        game.setOnGameFinishedListener(new Game.OnGameFinishedListener() {
            @Override
            public void onGameFinished() {
                saveCurrentGame();
            }
        });
    }

    private void saveCurrentGame() {
        new AsyncTask<Game, Void, Void> (){
            @Override
            protected Void doInBackground(Game... params) {
                //if (lastSavedGameID != DO_NOT_SAVE_GAME) {
                for (Game game : params) { //actually, there is only one game
                    long id = DBOpenHelper.getInstance(GameActivity.this).addGame(game);
                    if (lastSavedGameID != DO_NOT_SAVE_GAME) {
                        lastSavedGameID = id;
                    }
                }
                //}
                return null;
            }
        }.execute(game);
    }

    private final class StoredGameLoader extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Game loadedGame = DBOpenHelper.getInstance(GameActivity.this).getAndRemoveActiveGame();
            if (loadedGame == null) {
                loadedGame = DBOpenHelper.getInstance(GameActivity.this).getGameById(lastSavedGameID);
                lastSavedGameID = NO_GAME_SAVED;
            }
            if (loadedGame == null) {
                Log.d("GameActivity", "FAIL: Null game loaded");
            } else {
                Log.d("GameActivity", "OK: game loaded");
                if (game != null) {
                    game.onStop();
                }
                game = loadedGame;
                if (game.getCrossPlayer() instanceof HumanPlayer) {
                    ((HumanPlayer) game.getCrossPlayer()).setOnGameStateChangedListener(ON_GAME_STATE_CHANGED_LISTENER);
                } else if (game.getZeroPlayer() instanceof HumanPlayer) {
                    ((HumanPlayer) game.getZeroPlayer()).setOnGameStateChangedListener(ON_GAME_STATE_CHANGED_LISTENER);
                } //it's a dirty hack, don't know how to do better
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (game != null) {
                onGameLoaded(game);
            }
        }
    }

    private class GameLoadedFromServerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("GameActivity", "networkLoadGame broadcast recieved!");
            Bundle tmp = intent.getBundleExtra(WoVPreferences.GAME_BUNDLE);
            String data = tmp.getString(WoVProtocol.DATA);
            loadGameFromJson(data);
        }
    }

    private void loadGameFromJson(String data) {
        JsonObject jsonData = (JsonObject) new JsonParser().parse(data);
        User cross = gson.fromJson(jsonData.get(WoVProtocol.CROSS_USER), User.class);
        User zero = gson.fromJson(jsonData.get(WoVProtocol.ZERO_USER), User.class);

        DBOpenHelper.getInstance(GameActivity.this).addUser(cross);
        DBOpenHelper.getInstance(GameActivity.this).addUser(zero);

        GameLogic.PlayerFigure myFigure = gson.fromJson(jsonData.get(WoVProtocol.MY_FIGURE),
                GameLogic.PlayerFigure.class);
        Player playerCross, playerZero;

        switch (myFigure) {
            case CROSS:
                playerZero = new ClientNetworkPlayer(cross, ZERO, GameActivity.this);
                playerCross = humanPlayer = new HumanPlayer(zero, CROSS);
                break;
            case ZERO:
                playerCross = new ClientNetworkPlayer(cross, CROSS, GameActivity.this);
                playerZero = humanPlayer = new HumanPlayer(zero, ZERO);
                break;
            default:
                throw new IllegalArgumentException("Illegal myFigure value!");
        }

        List<GameEvent> events = (WoVProtocol.getEventsFromIntArray(gson.fromJson(jsonData.get(WoVProtocol.TURN_ARRAY), int[].class)));

        humanPlayer.setOnGameStateChangedListener(ON_GAME_STATE_CHANGED_LISTENER);
        int crossType = myFigure == CROSS ? 0 : 2;
        int zeroType = 2 - crossType; //fixme remove magic constants
        if (game != null) {
            game.onStop();
        }
        game = Game.deserializeGame(gson.fromJson(jsonData.get(WoVProtocol.GAME_ID), int.class),
                playerCross, crossType, playerZero, zeroType, GameLogic.deserialize(events));
        initButtons();
        redrawGame(game);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void onGameLoaded(Game game) {
        this.game = game;
        game.updateGameInfo();
        setCurrentGameListeners();
        initButtons();
        redrawGame(game);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.test2) {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    String msg;
                    try {
                        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(GameActivity.this);

                        Bundle data = new Bundle();
                        data.putString(WoVProtocol.ACTION, WoVProtocol.ACTION_PING);
                        String id = UUID.randomUUID().toString();
                        gcm.send(getString(R.string.gcm_defaultSenderId) + "@gcm.googleapis.com", id, data);
                        msg = "Sent message";
                    } catch (IOException ex) {
                        msg = "Error :" + ex.getMessage();
                    }
                    return msg;
                }

                @Override
                protected void onPostExecute(String msg) {
                    if (msg == null)
                        return;
                    Toast.makeText(GameActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }.execute(null, null, null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
