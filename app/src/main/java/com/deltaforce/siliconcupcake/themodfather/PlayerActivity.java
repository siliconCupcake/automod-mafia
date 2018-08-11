package com.deltaforce.siliconcupcake.themodfather;

import android.content.Context;

import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PlayerActivity extends AppCompatActivity {

    @BindView(R.id.player_name_layout)
    TextInputLayout playerNameLayout;

    @BindView(R.id.player_name)
    EditText nameField;

    @BindView(R.id.join_game)
    Button joinGame;

    @BindView(R.id.game_picker)
    GridView gameList;

    @BindView(R.id.player_config)
    RelativeLayout playerConfig;

    GridViewAdapter adapter;
    String playerName;
    ArrayList<String> games;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setCustomView(R.layout.action_bar);
        TextView title = getSupportActionBar().getCustomView().findViewById(R.id.action_bar_title);
        title.setText("PlayerActivity");

        ButterKnife.bind(this);

        gameList.setVerticalScrollBarEnabled(false);
        games = new ArrayList<>();
        adapter = new GridViewAdapter(this, games);
        gameList.setAdapter(adapter);

        joinGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerName = nameField.getText().toString().trim();
                if (TextUtils.isEmpty(playerName)) {
                    playerNameLayout.setError("Name cannot be empty");
                    if (nameField.requestFocus())
                        showKeyboard();
                } else if (adapter.getSelections().size() > 1) {
                    Snackbar.make(playerConfig, "Pick only one game.", Snackbar.LENGTH_SHORT).show();
                    playerNameLayout.setErrorEnabled(false);
                } else if (adapter.getSelections().size() == 0) {
                    Snackbar.make(playerConfig, "Pick a game.", Snackbar.LENGTH_SHORT).show();
                    playerNameLayout.setErrorEnabled(false);
                } else {
                    playerNameLayout.setErrorEnabled(false);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
        super.onBackPressed();
    }

    public void showKeyboard() {
        ((InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);
    }
}
