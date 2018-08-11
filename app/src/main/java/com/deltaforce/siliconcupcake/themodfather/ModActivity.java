package com.deltaforce.siliconcupcake.themodfather;

import android.animation.Animator;
import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;


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

        characterCards.setVerticalScrollBarEnabled(false);
        adapter = new GridViewAdapter(this, MafiaUtils.CHARACTER_TYPES);
        characterCards.setAdapter(adapter);

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
                        animateTransition();
                        nameLayout.setErrorEnabled(false);
                        playersJoined = true;
                    }
                } else {
                    //TODO: START THE GAME
                }
            }
        });
    }

    public void showKeyboard(){
        ((InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public void animateTransition(){
        gameConfig.animate().scaleX(0.0f).scaleY(0.0f).setDuration(300).setListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                gameConfig.setVisibility(View.GONE);
                connectionCount.setVisibility(View.VISIBLE);
                connectionCount.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_in));
                startGame.setText(R.string.start_game);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
        super.onBackPressed();
    }
}
