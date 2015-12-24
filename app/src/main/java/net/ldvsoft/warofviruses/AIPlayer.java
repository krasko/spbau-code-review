package net.ldvsoft.warofviruses;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import static java.lang.Math.abs;
import static java.lang.Thread.sleep;
import static net.ldvsoft.warofviruses.GameLogic.ADJACENT_DIRECTIONS;
import static net.ldvsoft.warofviruses.GameLogic.BOARD_SIZE;
import static net.ldvsoft.warofviruses.GameLogic.CoordinatePair;
import static net.ldvsoft.warofviruses.GameLogic.GameState;
import static net.ldvsoft.warofviruses.GameLogic.PlayerFigure;
import static net.ldvsoft.warofviruses.GameLogic.isInside;

/**
 * Created by Сева on 20.10.2015.
 */
public class AIPlayer extends Player {
    public static final User AI_USER = new User(
            DBProvider.USER_AI_PLAYER,
            "uniqueGoogleTokenForAiPlayer",
//            1, //DBOpenHelper.playerClasses[1]
            "SkyNet", 1,
            30, 210,
            null);
    private AsyncTask<Void, CoordinatePair, Void> runningStrategy;

    public AIPlayer(GameLogic.PlayerFigure ownFigure) {
        this.ownFigure = ownFigure;
        this.user = AI_USER;
        this.type = 1;
    }

    public static AIPlayer deserialize(User user, GameLogic.PlayerFigure ownFigure, Context context) {
        // There is only one AI user
        return new AIPlayer(ownFigure);
    }

    @Override
    public void makeTurn() {
        Log.d("AIPlayer", "Turn passed to AI player");
        runningStrategy = new BruteforceStrategy(game);
        runningStrategy.execute();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (runningStrategy != null) {
            runningStrategy.cancel(true);
        }
    }

    @Override
    public void onGameStateChanged(GameEvent event, Player whoChanged) {
    }

    @Override
    public void setGame(Game game) {
        super.setGame(game);
        if (!game.isFinished() && game.getCurrentPlayer().equals(this)) {
            makeTurn();
        }
    }

    private class BruteforceStrategy extends AsyncTask<Void, CoordinatePair, Void> {
        private Game game;
        BruteforceStrategy(Game game) {
            this.game = game;
        }

        //always returns score for ai player.
        private double getEndGameScore(GameLogic game) {
            switch (game.getCurrentGameState()) {
                case CROSS_WON:
                    return ownFigure == PlayerFigure.CROSS ? +100000 : -100000;
                case ZERO_WON:
                    return ownFigure == PlayerFigure.ZERO  ? +100000 : -100000;
                default:
                    return 0;
            }
        }
        //always returns score for ai player.
        private double getControlledCellsScore(GameLogic game) {
            final double DANGER_FACTOR = 1.25, CONTROL_FACTOR = 1.0, TURN_COUNT_FACTOR = 0.05;

            if (game.getCurrentGameState() != GameState.RUNNING) {
                return getEndGameScore(game);
            }

            double result = 0;

            boolean currentPlayerChanged = false;
            if (game.getCurrentPlayerFigure() != ownFigure) {
                game.setCurrentPlayerToOpponent();
                currentPlayerChanged = true;
            }

            for (int sign = 1; sign >= -1; sign -= 2) {

                for (int i = 0; i < BOARD_SIZE; i++) {
                    for (int j = 0; j < BOARD_SIZE; j++) {
                        if (game.getCellAt(i, j).isActive()) {
                            result += sign * CONTROL_FACTOR;
                        }
                        if (game.getCellAt(i, j).canMakeTurn() &&
                                game.getCellAt(i, j).getOwner() == game.getOpponent(game.getCurrentPlayerFigure())) {
                            result += sign * DANGER_FACTOR;
                        }
                        if (game.getCellAt(i, j).canMakeTurn()) {
                            result += sign * TURN_COUNT_FACTOR;
                        }
                    }
                }

                game.setCurrentPlayerToOpponent();
            }

            //we've probably just damaged current game state
            if (currentPlayerChanged) {
                game.setCurrentPlayerToOpponent();
            }
           // System.err.println("Total score=" + result);
            return result;
        }

        private void runStrategy(GameLogic gameLogic) {
            ArrayList<CoordinatePair> optMoves = bruteforceMoves(gameLogic);
            if (isCancelled() || optMoves == null) {
                return;
            }

            for (CoordinatePair move : optMoves) {
                publishProgress(move);
                try {
                    sleep(750);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void orderMovesByCost(ArrayList<CoordinatePair> moves, GameLogic curGameLogic) {
            class Pair {
                CoordinatePair move;
                double cost;
                Pair(CoordinatePair move, double cost) {
                    this.move = move;
                    this.cost = cost;
                }
            }

            ArrayList<Pair> movesWithCosts = new ArrayList<>();
            for (CoordinatePair move : moves) {
                GameLogic logic = new GameLogic(curGameLogic);
                logic.doTurn(move.x, move.y);
                movesWithCosts.add(new Pair(move, getControlledCellsScore(logic)));
            }

            Collections.sort(movesWithCosts, new Comparator<Pair>() {
                @Override
                public int compare(Pair lhs, Pair rhs) {
                    if (abs(lhs.cost - rhs.cost) < 1e-4) {
                        return 0;
                    }
                    return lhs.cost < rhs.cost ? -1 : 1;
                }
            });

            moves.clear();
            for (Pair pair : movesWithCosts) {
                moves.add(pair.move);
            }
        }

        private ArrayList<CoordinatePair> bruteforceMoves(GameLogic gameLogic) {
            if (isCancelled()) {
                return null;
            }

            ArrayList<CoordinatePair> result = new ArrayList<>();

            if (gameLogic.getCurrentPlayerFigure() != ownFigure) {
                return result;
            }

            ArrayList<CoordinatePair> moves = gameLogic.getMoves();
            if (moves.size() == 0) {
                return result;
            }

            orderMovesByCost(moves, gameLogic);
            Collections.reverse(moves); //take into account only moves that give us highest scores
            while (moves.size() > 5) {
                moves.remove(moves.size() - 1);
            }

            double optScore = -10000;

            for (CoordinatePair move: moves) {
                GameLogic tmpGameLogic = new GameLogic(gameLogic);
                tmpGameLogic.doTurn(move.x, move.y);
                ArrayList<CoordinatePair> optMoves = bruteforceMoves(tmpGameLogic);

                if (optMoves == null) {
                    return null;
                }

                for (CoordinatePair optMove : optMoves) {
                    tmpGameLogic.doTurn(optMove.x, optMove.y);
                }
                for (int i = 0; i < 1; i++) { //to check for immediate danger
                    ArrayList<CoordinatePair> enemyMoves = tmpGameLogic.getMoves();
                    orderMovesByCost(enemyMoves, tmpGameLogic);
                    if (enemyMoves.size() == 0) {
                        break;
                    }
                    tmpGameLogic.doTurn(enemyMoves.get(0).x, enemyMoves.get(0).y); //greedy minimize AI score
                }

                double newScore = getControlledCellsScore(tmpGameLogic);
                if (newScore > optScore) {
                    optScore = newScore;
                    result = optMoves;
                    result.add(0, move);
                }
            }
            return result;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d("AIPlayer", "AIPlayer::run");
            GameLogic gameLogic = game.getGameLogic();
            if (!gameLogic.canMove()) {
                publishProgress(new CoordinatePair(-1, -1));
            }
            runStrategy(gameLogic);
            Log.d("AIPlayer", "Turn finished");
            return null;
        }

        @Override
        protected void onProgressUpdate(CoordinatePair... cells) {
            if (isCancelled()) {
                return;
            }

            Log.d("AIPlayer", "Update progress: do move to " + cells[0].x + " " + cells[0].y);
            if (cells[0].x < 0) {
                game.skipTurn(AIPlayer.this);
            } else {
                game.doTurn(AIPlayer.this, cells[0].x, cells[0].y);
            }

        }
    }
}
