package net.ldvsoft.warofviruses;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.List;

import static net.ldvsoft.warofviruses.GameLogic.BOARD_SIZE;
import static net.ldvsoft.warofviruses.GameLogic.PlayerFigure;

public abstract class GameActivityBase extends AppCompatActivity {
    protected static final int PLAY_SERVICES_DIALOG = 9001;

    protected LinearLayout boardRoot;
    protected BoardCellButton[][] boardButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game_base);

        boardRoot = (LinearLayout) findViewById(R.id.game_board_root);
        buildBoard();

    }

    protected void redrawGame(GameLogic gameLogic, PlayerFigure currentPlayer) {
        if (gameLogic == null) {
            return;
        }

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                setButton(boardButtons[i][j], gameLogic.getCellAt(i, j), gameLogic.getCurrentPlayerFigure(), false);
            }
        }
        List<GameEvent> lastOpponentEvents = gameLogic.getLastEventsBy(gameLogic.getOpponent(currentPlayer));
        for (GameEvent event : lastOpponentEvents) {
            if (event.type != GameEvent.GameEventType.TURN_EVENT) {
                break;
            }
            int i = event.getTurnX();
            int j = event.getTurnY();
            setButton(boardButtons[i][j], gameLogic.getCellAt(i, j), gameLogic.getCurrentPlayerFigure(), true);
        }

        BoardCellButton avatar = (BoardCellButton) findViewById(R.id.game_cross_avatar);
        if (gameLogic.getCurrentPlayerFigure() == PlayerFigure.CROSS) {
            avatar.setImageDrawable(BoardCellButton.cellCross_forCross);
        } else {
            avatar.setImageDrawable(BoardCellButton.cellCross);
        }
        avatar = (BoardCellButton) findViewById(R.id.game_zero_avatar);
        if (gameLogic.getCurrentPlayerFigure() == PlayerFigure.ZERO) {
            avatar.setImageDrawable(BoardCellButton.cellZero_forZero);
        } else {
            avatar.setImageDrawable(BoardCellButton.cellZero);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_game, menu);
        return true;
    }

    public boolean checkGoogleServices() {
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        int result = availability.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (availability.isUserResolvableError(result)) {
                availability.getErrorDialog(this, result, PLAY_SERVICES_DIALOG).show();
            } else {
                Toast.makeText(this, "No Google Play Services.", Toast.LENGTH_SHORT).show();
            }
            return false;
        } else {
            return true;
        }
    }


    private void setButton(BoardCellButton button, GameLogic.Cell cell, GameLogic.PlayerFigure current, boolean highlight) {
        switch (cell.getCellType()) {
            case CROSS:
                if (cell.isActive()) {
                    button.setImageDrawable(BoardCellButton.cellCross_forCross);
                } else if (cell.canMakeTurn()) {
                    if (highlight) {
                        button.setImageDrawable(BoardCellButton.cellCross_forZero_highlighted);
                    } else {
                        button.setImageDrawable(BoardCellButton.cellCross_forZero);
                    }
                } else {
                    if (highlight) {
                        button.setImageDrawable(BoardCellButton.cellCross_highlighted);
                    } else {
                        button.setImageDrawable(BoardCellButton.cellCross);
                    }
                }
                break;
            case ZERO:
                if (cell.isActive()) {
                    button.setImageDrawable(BoardCellButton.cellZero_forZero);
                } else if (cell.canMakeTurn()) {
                    if (highlight) {
                        button.setImageDrawable(BoardCellButton.cellZero_forCross_highlighted);
                    } else {
                        button.setImageDrawable(BoardCellButton.cellZero_forCross);
                    }
                } else {
                    if (highlight) {
                        button.setImageDrawable(BoardCellButton.cellZero_highlighted);
                    } else {
                        button.setImageDrawable(BoardCellButton.cellZero);
                    }
                }
                break;
            case DEAD_CROSS:
                if (cell.isActive()) {
                    button.setImageDrawable(BoardCellButton.cellCrossDead_forZero);
                } else {
                    if (highlight) {
                        button.setImageDrawable(BoardCellButton.cellCrossDead_highlighted);
                    } else {
                        button.setImageDrawable(BoardCellButton.cellCrossDead);
                    }
                }
                break;
            case DEAD_ZERO:
                if (cell.isActive()) {
                    button.setImageDrawable(BoardCellButton.cellZeroDead_forCross);
                } else {
                    if (highlight) {
                        button.setImageDrawable(BoardCellButton.cellZeroDead_highlighted);
                    } else {
                        button.setImageDrawable(BoardCellButton.cellZeroDead);
                    }
                }
                break;
            case EMPTY:
                if (!cell.canMakeTurn()) {
                    button.setImageDrawable(BoardCellButton.cellEmpty);
                } else if (current == GameLogic.PlayerFigure.CROSS) {
                    button.setImageDrawable(BoardCellButton.cellEmpty_forCross);
                } else {
                    button.setImageDrawable(BoardCellButton.cellEmpty_forZero);
                }
        }
    }

    private void buildBoard() {
        if (boardRoot.getChildCount() != 0) {
            Log.wtf("gameActivity", "Board already present, not building one!");
            return;
        }

        // Init buttons' layout params, they require context to get 1dp in pixels
        LayoutParams boardButtonLayoutParams = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int marginValue = (int) Math.ceil(metrics.density * 1);
        boardButtonLayoutParams.setMargins(marginValue, marginValue, 0, 0);

        //BoardCellButton.loadDrawables(this, 30, 210);
        boardButtons = new BoardCellButton[BOARD_SIZE][BOARD_SIZE];

        for (int row = BOARD_SIZE - 1; row != -1; row--) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            for (int column = 0; column != BOARD_SIZE; column++) {
                boardButtons[row][column] = new BoardCellButton(this);
                rowLayout.addView(boardButtons[row][column], boardButtonLayoutParams);
            }
            boardRoot.addView(rowLayout, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        }


        boardRoot.invalidate();
        redrawGame(null, PlayerFigure.NONE);
    }
}
