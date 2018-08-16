package com.deltaforce.siliconcupcake.themodfather;

import android.animation.Animator;
import android.app.Dialog;
import android.content.Context;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
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

    @BindView(R.id.player_parent)
    FrameLayout parent;

    @BindView(R.id.game_setup)
    LinearLayout gameSetupLayout;

    @BindView(R.id.player_name_layout)
    TextInputLayout playerNameLayout;

    @BindView(R.id.player_name)
    EditText nameField;

    @BindView(R.id.game_list)
    GridView availableGameList;

    @BindView(R.id.join_button)
    Button joinButton;

    @BindView(R.id.sleep_layout)
    LinearLayout sleepLayout;

    @BindView(R.id.sleep_text)
    TextView sleepText;

    @BindView(R.id.sleep_button)
    Button sleepButton;

    @BindView(R.id.vote_layout)
    LinearLayout voteLayout;

    @BindView(R.id.death_text)
    TextView deathText;

    @BindView(R.id.vote_instruction)
    TextView voteInstruction;

    @BindView(R.id.vote_list)
    GridView voteList;

    @BindView(R.id.skip_button)
    Button skipButton;

    @BindView(R.id.vote_button)
    Button voteButton;

    GridViewAdapter gamesAdapter, voteAdapter;
    String playerName;
    ArrayList<Endpoint> endpoints = new ArrayList<>();
    ArrayList<Endpoint> alive;
    ConnectionsClient mConnectionsClient;
    Dialog loadingDialog, alertDialog;
    String myRole;
    boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        myRole = "Game Settings";
        setUpActionBar();

        ButterKnife.bind(this);
        sleepLayout.setVisibility(View.GONE);
        voteLayout.setVisibility(View.GONE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        availableGameList.setVerticalScrollBarEnabled(false);
        voteList.setVerticalScrollBarEnabled(false);
        gamesAdapter = new GridViewAdapter(this, endpoints, true);
        availableGameList.setAdapter(gamesAdapter);
        mConnectionsClient = Nearby.getConnectionsClient(this);

        mConnectionsClient.startDiscovery(MafiaUtils.SERVICE_ID,
                new EndpointDiscoveryCallback() {
                    @Override
                    public void onEndpointFound(@NonNull String s, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                        Endpoint e = new Endpoint(s, discoveredEndpointInfo.getEndpointName());
                        endpoints.add(e);
                        gamesAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onEndpointLost(@NonNull String s) {
                        Endpoint e = getEndpointWithId(s);
                        if (e != null) {
                            endpoints.remove(e);
                            gamesAdapter.notifyDataSetChanged();
                        }
                    }
                }, new DiscoveryOptions(Strategy.P2P_CLUSTER));

        setUpJoinButton();
        setUpSleepButton();
        setUpVoteButton();
        setUpSkipButton();
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
                    String gameName = getEndpointWithId(s).getName();
                    Snackbar.make(parent, "Connected to " + gameName, Snackbar.LENGTH_LONG).show();
                    showLoadingDialog("Waiting for other players");
                    endpoints.clear();
                    endpoints.add(new Endpoint(s, gameName));
                    isConnected = true;
                    break;

                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Snackbar.make(parent, "Connection Rejected", Snackbar.LENGTH_LONG).show();
                    break;

                case ConnectionsStatusCodes.STATUS_ERROR:
                    Snackbar.make(parent, "Connection error", Snackbar.LENGTH_LONG).show();
                    break;
            }
        }

        @Override
        public void onDisconnected(@NonNull String s) {
            showAlertDialog("Disconnected from game.", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.dismiss();
                    isConnected = false;
                    onBackPressed();
                }
            });
        }
    };

    private final PayloadCallback processPayload = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
            Response response = new Response();
            loadingDialog.dismiss();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            else
                ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(1000);
            try {
                response = (Response) MafiaUtils.deserialize(payload.asBytes());
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("PlayerActivity", "Deserialise failed");
            }
            switch (response.getType()) {
                case MafiaUtils.RESPONSE_TYPE_ROLE:
                    myRole = (String) response.getData();
                    showAlertDialog("You are " + myRole, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            alertDialog.dismiss();
                        }
                    });
                    ((TextView) getSupportActionBar().getCustomView().findViewById(R.id.action_bar_title)).setText("Night");
                    animateViews(gameSetupLayout, sleepLayout);
                    sleepButton.setEnabled(true);
                    break;

                case MafiaUtils.RESPONSE_TYPE_WAKE:
                    alive = (ArrayList<Endpoint>) response.getData();
                    deathText.setVisibility(View.GONE);
                    animateViews(sleepLayout, voteLayout);
                    skipButton.setEnabled(true);
                    setVotingInstruction();
                    voteAdapter = new GridViewAdapter(PlayerActivity.this, alive, true);
                    voteList.setAdapter(voteAdapter);
                    voteButton.setEnabled(true);
                    break;

                case MafiaUtils.RESPONSE_TYPE_ACK:
                    if (((String) response.getData()).equals("OK")) {
                        animateViews(voteLayout, sleepLayout);
                        sleepButton.setEnabled(true);
                    } else {
                        Snackbar.make(parent, (String) response.getData(), Snackbar.LENGTH_LONG).show();
                        voteButton.setEnabled(true);
                    }
                    break;

                case MafiaUtils.RESPONSE_TYPE_LYNCH:
                    if (response.getData().equals(playerName))
                        showAlertDialog("You were killed", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                alertDialog.dismiss();
                                quitGame();
                            }
                        });
                    else {
                        showAlertDialog(response.getData() + " was killed", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                alertDialog.dismiss();
                                animateViews(voteLayout, sleepLayout);
                                sleepButton.setEnabled(true);
                                ((TextView) getSupportActionBar().getCustomView().findViewById(R.id.action_bar_title)).setText("Night");
                            }
                        });
                    }
                    break;

                case MafiaUtils.RESPONSE_TYPE_DEATH:
                    alive = (ArrayList<Endpoint>) response.getData();
                    ((TextView) getSupportActionBar().getCustomView().findViewById(R.id.action_bar_title)).setText("Day");
                    if (alive.get(0).getName().equals(playerName)) {
                        showAlertDialog("You were killed", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                alertDialog.dismiss();
                                quitGame();
                            }
                        });
                    } else {
                        deathText.setVisibility(View.VISIBLE);
                        deathText.setText(MafiaUtils.WAKE_UP_MORNING + alive.get(0).getName());
                        alive.remove(0);
                        skipButton.setEnabled(true);
                        animateViews(sleepLayout, voteLayout);
                        voteInstruction.setText("Who would you like to lynch?");
                        voteAdapter = new GridViewAdapter(PlayerActivity.this, alive, true);
                        voteList.setAdapter(voteAdapter);
                        voteButton.setEnabled(true);
                    }
                    break;

                case MafiaUtils.RESPONSE_TYPE_OVER:
                    String winner = (String) response.getData();
                    showAlertDialog("The " + winner + " win.", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            alertDialog.dismiss();
                            quitGame();
                        }
                    });
                    break;

                case MafiaUtils.RESPONSE_TYPE_COP:
                    View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            alertDialog.dismiss();
                            animateViews(voteLayout, sleepLayout);
                            sleepButton.setEnabled(true);
                        }
                    };
                    if ((Boolean) response.getData())
                        showAlertDialog("That was the Mafia", listener);
                    else
                        showAlertDialog("That was a Villager", listener);
                    break;
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    private void setUpJoinButton() {
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerName = nameField.getText().toString().trim();
                if (TextUtils.isEmpty(playerName)) {
                    playerNameLayout.setError("Name cannot be empty");
                    if (nameField.requestFocus())
                        showKeyboard();
                } else if (gamesAdapter.getSelections().size() > 1) {
                    Snackbar.make(parent, "Pick only one game.", Snackbar.LENGTH_LONG).show();
                    playerNameLayout.setErrorEnabled(false);
                } else if (gamesAdapter.getSelections().size() == 0) {
                    Snackbar.make(parent, "Pick a game.", Snackbar.LENGTH_LONG).show();
                    playerNameLayout.setErrorEnabled(false);
                } else {
                    showLoadingDialog("Connecting to " + endpoints.get(gamesAdapter.getSelections().get(0)).getName());
                    mConnectionsClient.requestConnection(playerName,
                            endpoints.get(gamesAdapter.getSelections().get(0)).getId(), connectToGame);
                    playerNameLayout.setErrorEnabled(false);
                }
            }
        });
    }

    private void setUpSleepButton() {
        sleepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Request r = new Request(MafiaUtils.REQUEST_TYPE_CONTINUE, MafiaUtils.REQUEST_DATA_SLEPT);
                sendDataToMod(endpoints.get(0).getId(), r);
                sleepButton.setEnabled(false);
                showLoadingDialog("Please wait");
            }
        });
    }

    private void setUpVoteButton() {
        voteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (voteAdapter.getSelections().size() > 1)
                    Snackbar.make(parent, "Pick only one player.", Snackbar.LENGTH_LONG).show();
                else if (voteAdapter.getSelections().size() == 0)
                    Snackbar.make(parent, "Pick a game.", Snackbar.LENGTH_LONG).show();
                else {
                    Request r = new Request(MafiaUtils.REQUEST_TYPE_VOTE, alive.get(voteAdapter.getSelections().get(0)).getName());
                    sendDataToMod(endpoints.get(0).getId(), r);
                    voteButton.setEnabled(false);
                    showLoadingDialog("Please wait");
                }
            }
        });
    }

    private void quitGame() {
        isConnected = false;
        mConnectionsClient.disconnectFromEndpoint(endpoints.get(0).getId());
        onBackPressed();
    }

    private void setUpSkipButton() {
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Request r = new Request(MafiaUtils.REQUEST_TYPE_CONTINUE, MafiaUtils.REQUEST_DATA_SKIP);
                sendDataToMod(endpoints.get(0).getId(), r);
                showLoadingDialog("Please wait");
            }
        });
    }

    private void sendDataToMod(String id, Object data) {
        try {
            mConnectionsClient.sendPayload(id, Payload.fromBytes(MafiaUtils.serialize(data)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setVotingInstruction() {
        String instruction = "";
        switch (myRole) {
            case "Mafia":
                instruction = "Who do you want to kill?";
                break;

            case "Godfather":
                instruction = "Who do you want to kill?";
                break;

            case "Doctor":
                instruction = "Who do you want to save?";
                skipButton.setEnabled(false);
                break;

            case "Slut":
                instruction = "Who do you want to sleep with?";
                alive.remove(getEndpointWithName(playerName));
                skipButton.setEnabled(false);
                break;

            case "Cop":
                instruction = "Who do you want to inspect?";
                alive.remove(getEndpointWithName(playerName));
                skipButton.setEnabled(false);
                break;

            case "Vigilante":
                instruction = "Who do you want to kill?";
                alive.remove(getEndpointWithName(playerName));
                break;
        }
        voteInstruction.setText(instruction);
    }

    private void animateViews(final View exitView, final View enterView) {
        Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_out);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                exitView.setVisibility(View.GONE);
                enterView.setVisibility(View.VISIBLE);
                enterView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_in));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        exitView.startAnimation(anim);
    }

    private Endpoint getEndpointWithId(String eid) {
        for (Endpoint e : endpoints) {
            if (e.getId().equals(eid)) {
                return e;
            }
        }
        return null;
    }

    private Endpoint getEndpointWithName(String name) {
        for (Endpoint e : alive)
            if (e.getName().equals(name))
                return e;
        return null;
    }

    private void showLoadingDialog(String message) {
        loadingDialog = new Dialog(PlayerActivity.this);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ((TextView) loadingDialog.findViewById(R.id.dialog_text)).setText(message);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.setCancelable(false);
        loadingDialog.show();
    }

    private void showAlertDialog(String message, View.OnClickListener listener) {
        alertDialog = new Dialog(PlayerActivity.this);
        alertDialog.setContentView(R.layout.dialog_alert);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ((TextView) alertDialog.findViewById(R.id.dialog_text)).setText(message);
        alertDialog.findViewById(R.id.dialog_button).setOnClickListener(listener);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void setUpActionBar() {
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setCustomView(R.layout.action_bar);
        ((TextView) getSupportActionBar().getCustomView().findViewById(R.id.action_bar_title)).setText(myRole);
    }

    @Override
    public void onBackPressed() {
        if (!isConnected) {
            mConnectionsClient.stopDiscovery();
            finish();
            overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
            super.onBackPressed();
        } else {
            Snackbar.make(parent, "You cannot quit while in game", Snackbar.LENGTH_LONG).show();
        }
    }

    public void showKeyboard() {
        ((InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);
    }
}
