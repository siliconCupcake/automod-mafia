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

public class ModActivity extends AppCompatActivity {

    @BindView(R.id.init_game)
    Button startGame;

    @BindView(R.id.game_name_layout)
    TextInputLayout nameLayout;

    @BindView(R.id.game_name)
    EditText nameField;

    @BindView(R.id.character_picker)
    GridView characterCards;

    @BindView(R.id.game_config)
    RelativeLayout gameConfig;

    @BindView(R.id.connection_status)
    TextView connectionCount;

    boolean playersJoined = false;
    String gameName;
    GridViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod);

        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setCustomView(R.layout.action_bar);
        TextView title = getSupportActionBar().getCustomView().findViewById(R.id.action_bar_title);
        title.setText("ModActivity");

        ButterKnife.bind(this);

        startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!playersJoined) {
                    gameName = nameField.getText().toString().trim();
                    if (TextUtils.isEmpty(gameName)) {
                        nameLayout.setError("Name cannot be empty");
                        if (nameField.requestFocus())
                            showKeyboard();
                    } else if (adapter.getSelections().size() < 3) {
                        Snackbar.make(gameConfig, "Pick " + String.valueOf(3 - adapter.getSelections().size()) + " more.", Snackbar.LENGTH_SHORT).show();
                        nameLayout.setErrorEnabled(false);
                    } else {
                        gameConfig.setVisibility(View.GONE);
                        startGame.setText(R.string.start_game);
                        connectionCount.setVisibility(View.VISIBLE);
                        nameLayout.setErrorEnabled(false);
                        playersJoined = true;
                    }
                } else {
                    //TODO: START THE GAME
                }
            }
        });

        ArrayList<String> characters = new ArrayList<>();
        characters.add("Doctor");
        characters.add("Slut");
        characters.add("Cop");
        characters.add("Arson");
        characters.add("Vigilante");
        adapter = new GridViewAdapter(this, characters);
        characterCards.setAdapter(adapter);

        startGame.setEnabled(true);
    }

    public void showKeyboard(){
        ((InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
        super.onBackPressed();
    }
}
