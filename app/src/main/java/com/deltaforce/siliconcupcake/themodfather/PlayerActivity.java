package com.deltaforce.siliconcupcake.themodfather;

import android.app.Dialog;
import android.content.Context;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

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

    @BindView(R.id.game_setup)
    LinearLayout gameSetup;

    GridViewAdapter adapter;
    String playerName;
    ArrayList<String> games;
    ArrayList<Endpoint> endpoints = new ArrayList<>();
    ConnectionsClient mConnectionsClient;
    Dialog loadingDialog;
    String myRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        myRole = "PlayerActivity";
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setCustomView(R.layout.action_bar);
        ((TextView) getSupportActionBar().getCustomView().findViewById(R.id.action_bar_title)).setText(myRole);

        ButterKnife.bind(this);

        gameList.setVerticalScrollBarEnabled(false);
        games = new ArrayList<>();
        adapter = new GridViewAdapter(this, games);
        gameList.setAdapter(adapter);
        mConnectionsClient = Nearby.getConnectionsClient(this);

        mConnectionsClient.startDiscovery(MafiaUtils.SERVICE_ID,
                new EndpointDiscoveryCallback() {
                    @Override
                    public void onEndpointFound(@NonNull String s, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                        Endpoint e = new Endpoint(s, discoveredEndpointInfo.getEndpointName());
                        endpoints.add(e);
                        games.add(e.getName());
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onEndpointLost(@NonNull String s) {
                        Endpoint e = getEndpointWithId(s);
                        if (e != null) {
                            games.remove(e.getName());
                            endpoints.remove(e);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }, new DiscoveryOptions(Strategy.P2P_CLUSTER));

        joinGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerName = nameField.getText().toString().trim();
                if (TextUtils.isEmpty(playerName)) {
                    playerNameLayout.setError("Name cannot be empty");
                    if (nameField.requestFocus())
                        showKeyboard();
                } else if (adapter.getSelections().size() > 1) {
                    Snackbar.make(playerConfig, "Pick only one game.", Snackbar.LENGTH_LONG).show();
                    playerNameLayout.setErrorEnabled(false);
                } else if (adapter.getSelections().size() == 0) {
                    Snackbar.make(playerConfig, "Pick a game.", Snackbar.LENGTH_LONG).show();
                    playerNameLayout.setErrorEnabled(false);
                } else {
                    showLoadingDialog("Connecting to " + games.get(adapter.getSelections().get(0)));
                    mConnectionsClient.requestConnection(playerName,
                            endpoints.get(adapter.getSelections().get(0)).getId(), connectToGame);
                    playerNameLayout.setErrorEnabled(false);
                    myRole = "Mafia";
                }
            }
        });
    }

    private final ConnectionLifecycleCallback connectToGame = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String s, @NonNull ConnectionInfo connectionInfo) {
            mConnectionsClient.acceptConnection(s, processPayload);
        }

        @Override
        public void onConnectionResult(@NonNull String s, @NonNull ConnectionResolution connectionResolution) {
            loadingDialog.dismiss();
            switch (connectionResolution.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    mConnectionsClient.stopDiscovery();
                    gameSetup.setVisibility(View.GONE);
                    mConnectionsClient.stopDiscovery();
                    Snackbar.make(playerConfig, "Connected to " + getEndpointWithId(s).getName(), Snackbar.LENGTH_LONG).show();
                    showLoadingDialog("Waiting for other players");
                    break;

                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Snackbar.make(playerConfig, "Connection Rejected" , Snackbar.LENGTH_LONG).show();
                    break;

                case ConnectionsStatusCodes.STATUS_ERROR:
                    Snackbar.make(playerConfig, "Connection error" , Snackbar.LENGTH_LONG).show();
                    break;
            }
        }

        @Override
        public void onDisconnected(@NonNull String s) {

        }
    };

    private final PayloadCallback processPayload = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
            Response r = new Response();
            loadingDialog.dismiss();
            try {
                r = (Response) MafiaUtils.deserialize(payload.asBytes());
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("PlayerActivity", "Deserialise failed");
            }
            switch (r.getType()) {
                case MafiaUtils.RESPONSE_TYPE_ROLE:
                    myRole = (String) r.getData();
                    ((TextView) getSupportActionBar().getCustomView().findViewById(R.id.action_bar_title)).setText(myRole);
                    break;
            }

        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    private Endpoint getEndpointWithId(String eid) {
        for (Endpoint e: endpoints)
            if (e.getId().equals(eid))
                return e;
        return null;
    }

    private void showLoadingDialog(String message){
        loadingDialog = new Dialog(PlayerActivity.this);
        loadingDialog.setContentView(R.layout.dialog_connecting);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ((TextView) loadingDialog.findViewById(R.id.dialog_text)).setText(message);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.setCancelable(false);
        loadingDialog.show();
    }

    @Override
    public void onBackPressed() {
        mConnectionsClient.stopDiscovery();
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
