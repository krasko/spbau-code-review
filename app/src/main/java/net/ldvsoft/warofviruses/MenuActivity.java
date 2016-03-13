package net.ldvsoft.warofviruses;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.UUID;

public class MenuActivity extends AppCompatActivity {
    private GameLoadedFromServerReceiver gameLoadedFromServerReceiver = null;
    private TextView userName;
    private BoardCellButton crossButton;
    private BoardCellButton zeroButton;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private Button playAgainstBotButton;
    private Button playAgainstLocalPlayerButton;
    private Button playOnlineButton;
    private Button restoreGameButton;
    /*FIXME*/
    private FigureSet figureSet = new FigureSet();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        SharedPreferences preferences = getSharedPreferences(WoVPreferences.PREFERENCES, MODE_PRIVATE);
        if (!preferences.contains(WoVPreferences.CURRENT_USER_ID)) {
            preferences.edit().putLong(WoVPreferences.CURRENT_USER_ID, HumanPlayer.USER_ANONYMOUS.getId()).apply();
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        playAgainstBotButton = (Button) findViewById(R.id.button_play_against_bot);
        playAgainstLocalPlayerButton = (Button) findViewById(R.id.button_play_against_player);
        playOnlineButton = (Button) findViewById(R.id.button_play_online);
        restoreGameButton = (Button) findViewById(R.id.button_restore_game);

        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        View drawerHeader = navigationView.inflateHeaderView(R.layout.drawer_header);
        crossButton = (BoardCellButton) drawerHeader.findViewById(R.id.avatar_cross);
        zeroButton = (BoardCellButton) drawerHeader.findViewById(R.id.avatar_zero);
        userName = (TextView) drawerHeader.findViewById(R.id.username);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.drawer_clear_db:
                        clearDB();
                        return true;
                    case R.id.drawer_settings:
                        Intent intent = new Intent(MenuActivity.this, SettingsActivity.class);
                        startActivity(intent);
                        return true;
                    default:
                        Toast.makeText(MenuActivity.this, menuItem.getTitle() + " pressed", Toast.LENGTH_LONG).show();
                        drawerLayout.closeDrawers();
                        return true;
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onStop() {
        if (gameLoadedFromServerReceiver != null) {
            unregisterReceiver(gameLoadedFromServerReceiver);
            gameLoadedFromServerReceiver = null;
        }
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class RestoreGameDialog {
        private final Runnable loadGame;

        RestoreGameDialog(Runnable loadGame) {
            this.loadGame = loadGame;
        }

        public void execute() {
            new AlertDialog.Builder(MenuActivity.this)
                    .setMessage("Found saved game. What should I do with it?") //todo: more understandable options
                    .setCancelable(false)
                    .setPositiveButton("Load it", new RestoreGame())
                    .setNeutralButton("Do nothing", null)
                    .setNegativeButton("Give up and start new game", new NewGame())
                    .show();
        }

        private class RestoreGame implements Dialog.OnClickListener {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                restoreSavedGame(null);
            }
        }

        private class NewGame implements Dialog.OnClickListener {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Game game = DBOpenHelper.getInstance(MenuActivity.this).getAndRemoveActiveGame();
                game.giveUp(game.getCurrentPlayer()); //todo: give up for me, not for current player!
                DBOpenHelper.getInstance(MenuActivity.this).addGame(game);
                loadGame.run();
            }
        }
    }

    private class PlayAgainstBot implements Runnable {
        @Override
        public void run() {
            Intent intent = new Intent(MenuActivity.this, GameActivity.class);
            intent.putExtra(WoVPreferences.OPPONENT_TYPE, WoVPreferences.OPPONENT_BOT);
            startActivity(intent);
        }
    }

    public void playAgainstBot(View view) {
        if (DBOpenHelper.getInstance(this).hasActiveGame()) {
            new RestoreGameDialog(new PlayAgainstBot()).execute();
            return;
        }
        new PlayAgainstBot().run();
    }

    private class PlayAgainstLocalPlayer implements Runnable {
        @Override
        public void run() {
            Intent intent = new Intent(MenuActivity.this, GameActivity.class);
            intent.putExtra(WoVPreferences.OPPONENT_TYPE, WoVPreferences.OPPONENT_LOCAL_PLAYER);
            startActivity(intent);
        }
    }

    public void playAgainstLocalPlayer(View view) {
        if (DBOpenHelper.getInstance(this).hasActiveGame()) {
            new RestoreGameDialog(new PlayAgainstLocalPlayer()).execute();
            return;
        }
        new PlayAgainstLocalPlayer().run();
    }

    public void viewGameHistory(View view) {
        Intent intent = new Intent(this, GameHistoryActivity.class);
        startActivity(intent);
    }

    private class GameLoadedFromServerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("GameActivity", "networkLoadGame broadcast recieved!");
            Bundle tmp = intent.getBundleExtra(WoVPreferences.GAME_BUNDLE);
            String data = tmp.getString(WoVProtocol.DATA);
            intent = new Intent(MenuActivity.this, GameActivity.class);
            intent.putExtra(WoVPreferences.OPPONENT_TYPE, WoVPreferences.OPPONENT_NETWORK_PLAYER);
            intent.putExtra(WoVPreferences.GAME_JSON_DATA, data);
            unregisterReceiver(gameLoadedFromServerReceiver);
            gameLoadedFromServerReceiver = null;
            startActivity(intent);
        }
    }

    private class PlayOnline implements Runnable {
        @Override
        public void run() {
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(MenuActivity.this);
            Bundle data = new Bundle();
            data.putString(WoVProtocol.ACTION, WoVProtocol.ACTION_USER_READY);
            String id = UUID.randomUUID().toString();
            try {
                gcm.send(MenuActivity.this.getString(R.string.gcm_defaultSenderId) + "@gcm.googleapis.com", id, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            gameLoadedFromServerReceiver = new GameLoadedFromServerReceiver();
            registerReceiver(gameLoadedFromServerReceiver, new IntentFilter(WoVPreferences.GAME_LOADED_FROM_SERVER_BROADCAST));
        }
    }

    public void playOnline(View view) {
        if (DBOpenHelper.getInstance(this).hasActiveGame()) {
            new RestoreGameDialog(new PlayOnline()).execute();
            return;
        }
        new PlayOnline().run();
    }

    public void clearDB() {
        DBOpenHelper instance = DBOpenHelper.getInstance(this);
        instance.onUpgrade(instance.getReadableDatabase(), 0, 0);
        updateUI();
    }

    public void restoreSavedGame(View view) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(WoVPreferences.OPPONENT_TYPE, WoVPreferences.OPPONENT_RESTORED_GAME);
        startActivity(intent);
    }

    public void updateUI() {
        boolean hasGame = DBOpenHelper.getInstance(this).hasActiveGame();
        long userId = getSharedPreferences(WoVPreferences.PREFERENCES, MODE_PRIVATE).getLong(WoVPreferences.CURRENT_USER_ID, -1);
        User user = DBOpenHelper.getInstance(this).getUserById(userId);
        boolean isOnline = user.getId() != WoVPreferences.ANONYMOUS_NICKNAME_ID;

        playOnlineButton.setEnabled(!hasGame);
        playAgainstBotButton.setEnabled(!hasGame);
        playAgainstLocalPlayerButton.setEnabled(!hasGame);
        playOnlineButton.setEnabled(!hasGame /* FIXME && isOnline */);
        restoreGameButton.setEnabled(hasGame);

        navigationView.getMenu().findItem(R.id.drawer_login).setEnabled(!isOnline);
        navigationView.getMenu().findItem(R.id.drawer_logout).setEnabled(isOnline);

        /*FIXME*/
        for (GameLogic.PlayerFigure figure : GameLogic.PlayerFigure.values()) {
            figureSet.setFigureSource(figure, DefaultFigureSource.NAME);
        }
        userName.setText(user.getFullNickname());
        figureSet.setHueCross(user.getColorCross());
        figureSet.setHueZero(user.getColorZero());
        crossButton.setFigure(figureSet, BoardCellState.get(GameLogic.CellType.CROSS));
        zeroButton.setFigure(figureSet, BoardCellState.get(GameLogic.CellType.ZERO));


    }
}
