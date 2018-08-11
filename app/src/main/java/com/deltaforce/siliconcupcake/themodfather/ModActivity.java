package com.deltaforce.siliconcupcake.themodfather;

import android.animation.Animator;
import android.content.Context;
import android.support.annotation.NonNull;
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


import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

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

    int connections = 0;
    boolean playersJoined = false;
    String gameName;
    GridViewAdapter adapter;
    ConnectionsClient mConnectionsClient;
    ArrayList<Endpoint> players = new ArrayList<>();

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
        mConnectionsClient = Nearby.getConnectionsClient(this);

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
                        connectionCount.setText("Connections\n" + String.valueOf(connections));
                        animateTransition();
                        nameLayout.setErrorEnabled(false);
                        playersJoined = true;
                        mConnectionsClient.startAdvertising(gameName,
                                "com.deltaforce.siliconcupcake.themodfather",
                                connectToPlayers, new AdvertisingOptions(Strategy.P2P_CLUSTER));
                    }
                } else {
                    //TODO: START THE GAME
                }
            }
        });
    }

    private final ConnectionLifecycleCallback connectToPlayers = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String s, @NonNull ConnectionInfo connectionInfo) {
            Endpoint e = new Endpoint(s, connectionInfo.getEndpointName());
            players.add(e);
            mConnectionsClient.acceptConnection(s, new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {

                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

                }
            });
        }

        @Override
        public void onConnectionResult(@NonNull String s, @NonNull ConnectionResolution connectionResolution) {
            Endpoint e = getEndpointWithId(s);
            switch (connectionResolution.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    Snackbar.make(connectionCount, "Connected to " + e.getName() , Snackbar.LENGTH_SHORT).show();
                    connections++;
                    connectionCount.setText("Connections\n" + String.valueOf(connections));
                    break;

                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Snackbar.make(connectionCount, "Connection Rejected" , Snackbar.LENGTH_SHORT).show();
                    players.remove(e);
                    break;

                case ConnectionsStatusCodes.STATUS_ERROR:
                    Snackbar.make(connectionCount, "Connection error" , Snackbar.LENGTH_SHORT).show();
                    players.remove(e);
                    break;
            }
        }

        @Override
        public void onDisconnected(@NonNull String s) {
            connections--;
            connectionCount.setText("Connections\n" + String.valueOf(connections));
        }
    };

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
        mConnectionsClient.stopAdvertising();
        finish();
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
        super.onBackPressed();
    }

    private Endpoint getEndpointWithId(String eid) {
        for (Endpoint e: players)
            if (e.getId().equals(eid))
                return e;
        return null;
    }
}
