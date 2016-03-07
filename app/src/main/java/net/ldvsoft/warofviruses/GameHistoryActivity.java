package net.ldvsoft.warofviruses;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import static net.ldvsoft.warofviruses.GameLogic.CellType;
import static net.ldvsoft.warofviruses.GameLogic.PlayerFigure;

/**
 * Activity which displays all the played finished games that stored locally.
 * User can select some game to replay it
 */
public class GameHistoryActivity extends AppCompatActivity {
    private MyAdapter adapter;
    private FigureSet figureSet = new FigureSet();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_history);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        RecyclerView recycler = (RecyclerView) findViewById(R.id.games);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyAdapter();
        recycler.setAdapter(adapter);

        adapter.notifyDataSetChanged();

        /*FIXME*/
        for (PlayerFigure figure : PlayerFigure.values()) {
            figureSet.setFigureSource(figure, DefaultFigureSource.NAME);
        }
    }

    private class GameHistoryLoader extends AsyncTask<Void, Void, Void> {
        private ArrayList<Game> gameHistory = null;

        @Override
        protected void onPostExecute(Void aVoid) {
            if (gameHistory == null) {
                return;
            }
            adapter.fetchData(/*FIXME*/0, gameHistory);
        }

        @Override
        protected Void doInBackground(Void... params) {
            gameHistory = DBOpenHelper.getInstance(GameHistoryActivity.this).getGameHistory();

            return null;
        }

    }
    @Override
    protected void onStart() {
        super.onStart();
        new GameHistoryLoader().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                new GameHistoryLoader().execute();
                return true;
            default:
                return false;
        }
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private long userId;
        private ArrayList<Game> data;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(GameHistoryActivity.this).inflate(R.layout.row_game_relpay, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Game game = data.get(position);
            switch (game.getMyFigure(userId)) {
                case NONE:
                    holder.opponent.setText(getString(R.string.game_status_local));
                    holder.figure.setFigure(figureSet, BoardCellState.get(CellType.EMPTY));
                    switch (game.getGameState()) {
                        case CROSS_WON:
                            holder.result.setText(getString(R.string.game_status_cross_won));
                            break;
                        case ZERO_WON:
                            holder.result.setText(getString(R.string.game_status_zero_won));
                            break;
                        case DRAW:
                            holder.result.setText(getString(R.string.game_status_draw));
                            break;
                    }
                    break;
                case CROSS:
                    holder.opponent.setText(game.getZeroPlayer().getName());
                    switch (game.getGameState()) {
                        case CROSS_WON:
                            holder.figure.setFigure(figureSet, BoardCellState.get(CellType.CROSS));
                            holder.result.setText(getString(R.string.game_status_won));
                            break;
                        case ZERO_WON:
                            holder.figure.setFigure(figureSet, BoardCellState.get(CellType.DEAD_CROSS));
                            holder.result.setText(getString(R.string.game_status_lost));
                            break;
                        case DRAW:
                            holder.figure.setFigure(figureSet, BoardCellState.get(CellType.EMPTY));
                            holder.result.setText(getString(R.string.game_status_draw));
                    }
                    break;
                case ZERO:
                    holder.opponent.setText(game.getCrossPlayer().getName());
                    switch (game.getGameState()) {
                        case CROSS_WON:
                            holder.figure.setFigure(figureSet, BoardCellState.get(CellType.DEAD_ZERO));
                            holder.result.setText(getString(R.string.game_status_lost));
                            break;
                        case ZERO_WON:
                            holder.figure.setFigure(figureSet, BoardCellState.get(CellType.ZERO));
                            holder.result.setText(getString(R.string.game_status_won));
                            break;
                        case DRAW:
                            holder.figure.setFigure(figureSet, BoardCellState.get(CellType.EMPTY));
                            holder.result.setText(getString(R.string.game_status_draw));
                    }
                    break;
            }
            Date date = new GregorianCalendar(2016, 9, 1, 15, 35).getTime();
            holder.date.setText(DateFormat.getDateFormat(GameHistoryActivity.this).format(date));
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(GameHistoryActivity.this, GameActivityReplay.class);
                    intent.putExtra(WoVPreferences.REPLAY_GAME_ID, game.getGameId());
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            if (data == null) {
                return 0;
            }
            return data.size();
        }

        @Override
        public long getItemId(int position) {
            return data.get(position).getGameId();
        }

        public void fetchData(long userId, ArrayList<Game> data) {
            this.userId = userId;
            this.data = data;
            notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private BoardCellButton figure;
            private TextView opponent;
            private TextView result;
            private TextView date;

            private ViewHolder(View view) {
                super(view);
                figure = (BoardCellButton) view.findViewById(R.id.figure);
                opponent = (TextView) view.findViewById(R.id.opponent);
                result = (TextView) view.findViewById(R.id.result);
                date = (TextView) view.findViewById(R.id.date);
            }
        }
    }

}
