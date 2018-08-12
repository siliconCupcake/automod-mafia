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
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
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
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ModActivity extends AppCompatActivity {

    @BindView(R.id.mod_parent)
    LinearLayout parent;

    @BindView(R.id.config_layout)
    RelativeLayout configLayout;

    @BindView(R.id.game_name_layout)
    TextInputLayout nameLayout;

    @BindView(R.id.game_name)
    EditText nameField;

    @BindView(R.id.character_list)
    GridView characterList;

    @BindView(R.id.connection_status)
    TextView connectionCount;

    @BindView(R.id.init_game)
    Button startGame;

    int connections = 0;
    boolean playersJoined = false;
    String gameName;
    GridViewAdapter adapter;
    ConnectionsClient mConnectionsClient;
    ArrayList<Endpoint> players = new ArrayList<>();
    ArrayList<String> roles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod);

        setUpActionBar();

        ButterKnife.bind(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        characterList.setVerticalScrollBarEnabled(false);
        adapter = new GridViewAdapter(this, MafiaUtils.CHARACTER_TYPES);
        characterList.setAdapter(adapter);
        mConnectionsClient = Nearby.getConnectionsClient(this);

        setUpStartButton();
    }

    private final ConnectionLifecycleCallback connectToPlayers = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String s, @NonNull ConnectionInfo connectionInfo) {
            Endpoint e = new Endpoint(s, connectionInfo.getEndpointName());
            players.add(e);
            mConnectionsClient.acceptConnection(s, processPayload);
        }

        @Override
        public void onConnectionResult(@NonNull String s, @NonNull ConnectionResolution connectionResolution) {
            Endpoint e = getEndpointWithId(s);
            switch (connectionResolution.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    Snackbar.make(connectionCount, "Connected to " + e.getName() , Snackbar.LENGTH_LONG).show();
                    connections++;
                    connectionCount.setText("Connections\n" + String.valueOf(connections));
                    break;

                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    players.remove(e);
                    break;

                case ConnectionsStatusCodes.STATUS_ERROR:
                    players.remove(e);
                    break;
            }
        }

        @Override
        public void onDisconnected(@NonNull String s) {
            connections--;
            connectionCount.setText("Connections\n" + String.valueOf(connections));
            players.remove(getEndpointWithId(s));
        }
    };

    private final PayloadCallback processPayload = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {

        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    private void setUpStartButton(){
        startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!playersJoined) {
                    gameName = nameField.getText().toString().trim();
                    if (TextUtils.isEmpty(gameName)) {
                        nameLayout.setError("Name cannot be empty");
                        if (nameField.requestFocus())
                            showKeyboard();
                    } else if (adapter.getSelections().size() < 2) {
                        Snackbar.make(parent, "Pick " + String.valueOf(3 - adapter.getSelections().size()) + " more.", Snackbar.LENGTH_LONG).show();
                        nameLayout.setErrorEnabled(false);
                    } else {
                        connectionCount.setText("Connections\n" + String.valueOf(connections));
                        animateTransition();
                        nameLayout.setErrorEnabled(false);
                        playersJoined = true;
                        mConnectionsClient.startAdvertising(gameName,
                                MafiaUtils.SERVICE_ID,
                                connectToPlayers, new AdvertisingOptions(Strategy.P2P_CLUSTER));
                    }
                } else if (connections < 3) {
                    Snackbar.make(parent, "You need a minimum of 5 players to start.", Snackbar.LENGTH_LONG).show();
                } else {
                    mConnectionsClient.stopAdvertising();
                    assignRoles(connections);
                    startGame.setEnabled(false);
                    for (int i = 0; i < players.size(); i++) {
                        String logText = "Sent Role: {" + players.get(i).getName() + ", " + roles.get(i) + "}";
                        MafiaUtils.addToLogFile(logText, gameName + ".txt");
                        Response r = new Response(MafiaUtils.RESPONSE_TYPE_ROLE, roles.get(i));
                        sendDataToPlayer(players.get(i).getId(), r);
                    }
                }
            }
        });
    }

    private void assignRoles(int n) {
        roles.add("Godfather");
        for (int i = 0; i < adapter.getSelections().size(); i++)
            roles.add(MafiaUtils.CHARACTER_TYPES.get(adapter.getSelections().get(i)));
        roles.addAll(Collections.nCopies((n/3)-1, "Mafia"));
        roles.addAll(Collections.nCopies(n-roles.size(), "Villager"));
        Collections.shuffle(roles);
    }

    private Endpoint getEndpointWithId(String eid) {
        for (Endpoint e: players)
            if (e.getId().equals(eid))
                return e;
        return null;
    }

    private void sendDataToPlayer(String id, Object data) {
        try {
            mConnectionsClient.sendPayload(id, Payload.fromBytes(MafiaUtils.serialize(data)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showKeyboard(){
        ((InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private void animateTransition(){
        configLayout.animate().scaleX(0.0f).scaleY(0.0f).setDuration(300).setListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                configLayout.setVisibility(View.GONE);
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
        if (!playersJoined) {
            mConnectionsClient.stopAdvertising();
            finish();
            overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
            super.onBackPressed();
        } else {
            Snackbar.make(parent, "You cannot quit while in game", Snackbar.LENGTH_LONG).show();
        }
    }

    private void setUpActionBar() {
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setCustomView(R.layout.action_bar);
        ((TextView) getSupportActionBar().getCustomView().findViewById(R.id.action_bar_title)).setText("Game Settings");
    }
}
