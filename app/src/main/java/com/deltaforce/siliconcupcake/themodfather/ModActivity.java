package com.deltaforce.siliconcupcake.themodfather;

import android.animation.Animator;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

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

    @BindView(R.id.connection_layout)
    RelativeLayout connectionLayout;

    @BindView(R.id.connection_status)
    TextView connectionCount;

    @BindView(R.id.connection_list)
    TextView connectionList;

    @BindView(R.id.init_game)
    Button startGame;

    int sleeping = 0, voted = 0, skipped = 0, nightStage = -1;
    boolean playersJoined = false;
    boolean isNight = true;
    boolean hunterVote = false;
    boolean mafiaVoted = false;
    boolean havePresident = false;
    boolean sandmanValid = false;

    String gameName;
    ArrayList<String> gameRoles = new ArrayList<>();
    GridViewAdapter adapter;
    HashMap<String, Endpoint> nightChoices = new HashMap<>();
    ConnectionsClient mConnectionsClient;
    ArrayList<Endpoint> players = new ArrayList<>();
    Endpoint prevSlut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod);

        setUpActionBar();

        ButterKnife.bind(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        gameName = getSharedPreferences("defaults", MODE_PRIVATE).getString("gName", "");
        nameField.append(gameName);

        characterList.setVerticalScrollBarEnabled(false);
        adapter = new GridViewAdapter(this, MafiaUtils.CHARACTER_TYPES, false);
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
                    Snackbar.make(connectionCount, e.getName() + " connected to game", Snackbar.LENGTH_LONG).show();
                    refreshConnectionsList();
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
            Endpoint player = getEndpointWithId(s);
            players.remove(player);
            try {
                if (playersJoined) {
                    gameRoles.remove(player.getRole());
                    refreshConnectionsList();
                    if (!isNight) {
                        if(hunterVote) {
                            MafiaUtils.addToLogFile(player.getRole() + " chose to skip.", gameName + ".txt");
                            Response r = new Response(MafiaUtils.RESPONSE_TYPE_ACK, "OK");
                            for (Endpoint p: players)
                                sendDataToPlayer(p.getId(), r);
                        } else if (sleeping == players.size()) {
                            sleeping = 0;
                            isNight = true;
                            int isOver = isGameOver();
                            if (isOver == 3) {
                                nightStage = -1;
                                nightChoices.clear();
                                nightVoting();
                            } else {
                                Response r = new Response(MafiaUtils.RESPONSE_TYPE_OVER, MafiaUtils.WINNER[isOver]);
                                for (Endpoint p : players) {
                                    sendDataToPlayer(p.getId(), r);
                                    MafiaUtils.addToLogFile("Send to: {" + p.getName() + ", " + r.getData() + " win.}", gameName + ".txt");
                                }
                                playersJoined = false;
                                onBackPressed();
                            }
                        } else if (voted == players.size()) {
                            voted = 0;
                            calculateLynch();
                        }
                    } else if (getMafia().contains(player)) {
                        if (sleeping == getMafia().size()) {
                            sleeping = 0;
                            mafiaVoted = true;
                            nightVoting();
                        } else if (voted == getMafia().size()) {
                            voted = 0;
                            checkMafiaVotes();
                        }
                    } else if (player.getRole().equals(gameRoles.get(nightStage-1))){
                        nightVoting();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private final PayloadCallback processPayload = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
            Request request = new Request();
            try {
                request = (Request) MafiaUtils.deserialize(payload.asBytes());
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ModActivity", "Deserialize failed");
            }
            switch (request.getType()) {
                case MafiaUtils.REQUEST_TYPE_CONTINUE:
                    handleContinueRequest(request.getData(), getEndpointWithId(s));
                    break;

                case MafiaUtils.REQUEST_TYPE_VOTE:
                    Endpoint votee;
                    if (!isNight) {
                        if(hunterVote){
                            votee = getEndpointWithName(request.getData());
                            MafiaUtils.addToLogFile("Receive from: {" + getEndpointWithId(s).getName() + ", HUNTER VOTE: " + votee.getName() + "}", gameName + ".txt");
                            Response r = new Response(MafiaUtils.RESPONSE_TYPE_LYNCH, votee.getName());
                            for (Endpoint p: players)
                                sendDataToPlayer(p.getId(), r);
                            sendDataToPlayer(s, new Response(MafiaUtils.RESPONSE_TYPE_ACK, "KILL_HUNTER"));
                            hunterVote = false;
                        } else {
                            votee = getEndpointWithName(request.getData());
                            MafiaUtils.addToLogFile("Receive from: {" + getEndpointWithId(s).getName() + ", VOTE: " + votee.getName() + "}", gameName + ".txt");
                            votee.setVotes(votee.getVotes() + 1);
                            voted++;
                            if (voted == players.size()) {
                                voted = 0;
                                calculateLynch();
                            }
                        }
                    } else if (getMafia().contains(getEndpointWithId(s)) && !mafiaVoted) {
                        votee = getEndpointWithName(request.getData());
                        MafiaUtils.addToLogFile("Receive from: {" + getEndpointWithId(s).getName() + ", VOTE: " + votee.getName() + "}", gameName + ".txt");
                        votee.setVotes(votee.getVotes() + 1);
                        voted++;
                        if (voted == getMafia().size()) {
                            voted = 0;
                            checkMafiaVotes();
                        }
                    } else if (getEndpointWithId(s).getRole().equals("Cop")) {
                        Endpoint cop = getEndpointWithId(s);
                        votee = getEndpointWithName(request.getData());
                        MafiaUtils.addToLogFile("Receive from: {" + cop.getName() + ", VOTE: " + votee.getName() + "}", gameName + ".txt");
                        if ((nightChoices.containsKey("Slut") && nightChoices.get("Slut").equals(getEndpointWithId(s))) || nightChoices.containsKey("Silencer")) {
                            Response r = new Response(MafiaUtils.RESPONSE_TYPE_COP, false);
                            sendDataToPlayer(s, r);
                            MafiaUtils.addToLogFile("Send to: {" + cop.getName() + ", COP: Villager}", gameName + ".txt");
                        } else if (votee.getRole().equals("Mafia") || (votee.getRole().equals("Godfather") && nightChoices.containsKey("Slut") && nightChoices.get("Slut").getRole().equals("Godfather"))) {
                            Response r = new Response(MafiaUtils.RESPONSE_TYPE_COP, true);
                            sendDataToPlayer(s, r);
                            MafiaUtils.addToLogFile("Send to: {" + cop.getName() + ", COP: Mafia}", gameName + ".txt");
                        } else {
                            Response r = new Response(MafiaUtils.RESPONSE_TYPE_COP, false);
                            sendDataToPlayer(s, r);
                            MafiaUtils.addToLogFile("Send to: {" + cop.getName() + ", COP: Villager}", gameName + ".txt");
                        }
                    } else if (getEndpointWithId(s).getRole().equals("Sandman")) {
                        sandmanValid = false;
                        nightChoices.clear();
                        nightChoices.put("Sandman", null);
                        gameRoles.remove("Sandman");
                        sendDataToPlayer(s, new Response(MafiaUtils.RESPONSE_TYPE_ACK, "OK"));
                    } else {
                        votee = getEndpointWithName(request.getData());
                        MafiaUtils.addToLogFile("Receive from: {" + getEndpointWithId(s).getName() + ", VOTE: " + votee.getName() + "}", gameName + ".txt");
                        Response r;
                        if (getEndpointWithId(s).getRole().equals("Slut") && votee.equals(prevSlut)) {
                            r = new Response(MafiaUtils.RESPONSE_TYPE_ACK, "You can't sleep with the same guy twice in a row.");
                            MafiaUtils.addToLogFile("Send to: {" + getEndpointWithId(s).getName() + ", ACK: You can't sleep with the same guy twice in a row.}", gameName + ".txt");
                        } else if (getEndpointWithId(s).getRole().equals("Slut")) {
                            prevSlut = votee;
                            r = new Response(MafiaUtils.RESPONSE_TYPE_ACK, "OK");
                            nightChoices.put(getEndpointWithId(s).getRole(), votee);
                            MafiaUtils.addToLogFile(getEndpointWithId(s).getRole() + " chose " + votee.getName() + "}", gameName + ".txt");
                        } else {
                            r = new Response(MafiaUtils.RESPONSE_TYPE_ACK, "OK");
                            nightChoices.put(getEndpointWithId(s).getRole(), votee);
                            MafiaUtils.addToLogFile(getEndpointWithId(s).getRole() + " chose " + votee.getName() + "}", gameName + ".txt");
                        }
                        sendDataToPlayer(s, r);
                    }
                    break;
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    private void checkMafiaVotes() {
        Collections.sort(players);
        String logText;
        Response r;
        boolean isKilled = true;
        if (skipped == getMafia().size()) {
            r = new Response(MafiaUtils.RESPONSE_TYPE_ACK, "OK");
            logText = "Mafia chose nobody";
        } else if (players.get(0).getVotes() == getMafia().size()) {
            logText = "Mafia chose " + players.get(0).getName() + "}";
            r = new Response(MafiaUtils.RESPONSE_TYPE_ACK, "OK");
            nightChoices.put("Mafia", players.get(0));
        } else {
            r = new Response(MafiaUtils.RESPONSE_TYPE_ACK, "Vote needs to be unanimous, please vote again");
            logText = "Send to: {%s, ACK: Vote Again}";
            isKilled = false;
        }
        clearAllVotes();
        for (Endpoint m : getMafia()) {
            sendDataToPlayer(m.getId(), r);
            if (!isKilled) {
                MafiaUtils.addToLogFile(String.format(logText, m.getName()), gameName + ".txt");
            }
        }
        if (isKilled)
            MafiaUtils.addToLogFile(logText, gameName + ".txt");
    }

    private void clearAllVotes() {
        for (Endpoint player : players)
            player.setVotes(0);
        skipped = 0;
        voted = 0;
    }

    private void setUpStartButton() {
        startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!playersJoined) {
                    gameName = nameField.getText().toString().trim();
                    getSharedPreferences("defaults", MODE_PRIVATE).edit().putString("gName", gameName).apply();
                    if (TextUtils.isEmpty(gameName)) {
                        nameLayout.setError("Name cannot be empty");
                        if (nameField.requestFocus())
                            showKeyboard();
                    } else if (adapter.getSelections().size() < 3) {
                        Snackbar.make(parent, "Pick " + String.valueOf(3 - adapter.getSelections().size()) + " more.", Snackbar.LENGTH_LONG).show();
                        nameLayout.setErrorEnabled(false);
                    } else {
                        refreshConnectionsList();
                        animateTransition();
                        nameLayout.setErrorEnabled(false);
                        playersJoined = true;
                        mConnectionsClient.startAdvertising(gameName,
                                MafiaUtils.SERVICE_ID,
                                connectToPlayers, new AdvertisingOptions(Strategy.P2P_CLUSTER));
                    }
                } else if (players.size() < 5) {
                    Snackbar.make(parent, "You need a minimum of 6 players to start.", Snackbar.LENGTH_LONG).show();
                } else if (!assignRoles(players.size())) {
                    Snackbar.make(parent, "You have insufficient players.", Snackbar.LENGTH_LONG).show();
                } else {
                    mConnectionsClient.stopAdvertising();
                    File logFile = new File(Environment.getExternalStorageDirectory().getPath() + "/TheModfather/" + gameName + ".txt");
                    if (logFile.exists())
                        logFile.delete();
                    startGame.setEnabled(false);
                    for (int i = 0; i < players.size(); i++) {
                        String data;
                        if (getEndpointWithRole("President") != null && !getMafia().contains(players.get(i)))
                            data = players.get(i).getRole() + "," + getEndpointWithRole("President").getName();
                        else
                            data = players.get(i).getRole();
                        Response r = new Response(MafiaUtils.RESPONSE_TYPE_ROLE, data);
                        String logText = "Sent to: {" + players.get(i).getName() + ", " + data + "}";
                        MafiaUtils.addToLogFile(logText, gameName + ".txt");
                        sendDataToPlayer(players.get(i).getId(), r);
                    }
                    isNight = false;
                    clearAllVotes();
                }
            }
        });
    }

    private void handleContinueRequest(String data, Endpoint player) {
        MafiaUtils.addToLogFile("Receive from: {" + player.getName() + ", CONTINUE: " + data + "}", gameName + ".txt");
        switch (data) {
            case MafiaUtils.REQUEST_DATA_SLEPT:
                if (!isNight) {
                    sleeping++;
                    if (sleeping == players.size()) {
                        sleeping = 0;
                        isNight = true;
                        int isOver = isGameOver();
                        if (isOver == 3) {
                            nightStage = -1;
                            nightChoices.clear();
                            nightVoting();
                        } else {
                            Response r = new Response(MafiaUtils.RESPONSE_TYPE_OVER, MafiaUtils.WINNER[isOver]);
                            for (Endpoint p : players) {
                                sendDataToPlayer(p.getId(), r);
                                MafiaUtils.addToLogFile("Send to: {" + p.getName() + ", " + r.getData() + " win.}", gameName + ".txt");
                            }
                            playersJoined = false;
                            onBackPressed();
                        }
                    }
                } else if (getMafia().contains(player) && !mafiaVoted) {
                    sleeping++;
                    if (sleeping == getMafia().size()) {
                        sleeping = 0;
                        mafiaVoted = true;
                        nightVoting();
                    }
                } else {
                    nightVoting();
                }
                break;

            case MafiaUtils.REQUEST_DATA_SKIP:
                if (!isNight) {
                    if(hunterVote) {
                        MafiaUtils.addToLogFile(player.getRole() + " chose to skip.", gameName + ".txt");
                        Response r = new Response(MafiaUtils.RESPONSE_TYPE_ACK, "OK");
                        for (Endpoint p: players)
                            sendDataToPlayer(p.getId(), r);
                        sendDataToPlayer(player.getId(), new Response(MafiaUtils.RESPONSE_TYPE_ACK, "KILL_HUNTER"));
                        hunterVote = false;
                    } else {
                        voted++;
                        skipped++;
                        if (voted == players.size())
                            calculateLynch();
                    }
                } else if (getMafia().contains(player)) {
                    voted++;
                    skipped++;
                    if (voted == getMafia().size())
                        checkMafiaVotes();
                } else {
                    Response r = new Response(MafiaUtils.RESPONSE_TYPE_ACK, "OK");
                    sendDataToPlayer(player.getId(), r);
                    MafiaUtils.addToLogFile(player.getRole() + " chose to skip.", gameName + ".txt");
                }
                break;
        }
    }

    private void refreshConnectionsList(){
        String conList = "";
        connectionCount.setText("Connections: " + String.valueOf(players.size()));
        for (Endpoint p: players)
            conList = conList + p.getName() + "\n";
        connectionList.setText(conList);
    }

    private int isGameOver() {
        if (getMafia().size() >= players.size() - getMafia().size())
            return 0;
        else if (getMafia().size() == 0)
            return 1;
        else if (havePresident && (getEndpointWithRole("President") == null))
            return 2;
        else
            return 3;
    }

    private void nightVoting() {
        Response r;
        if (nightStage < gameRoles.size()) {
            if (nightStage == -1) {
                ArrayList<Endpoint> mafia = getMafia();
                r = new Response(MafiaUtils.RESPONSE_TYPE_WAKE, players);
                for (Endpoint m : mafia) {
                    sendDataToPlayer(m.getId(), r);
                    MafiaUtils.addToLogFile("Send to: {" + m.getName() + ", WAKE: " + r.getData() + "}", gameName + ".txt");
                }
                nightStage++;
            } else {
                Endpoint p = getEndpointWithRole(gameRoles.get(nightStage));
                r = new Response(MafiaUtils.RESPONSE_TYPE_WAKE, players);
                MafiaUtils.addToLogFile("Send to: {" + p.getName() + ", WAKE: " + r.getData() + "}", gameName + ".txt");
                sendDataToPlayer(p.getId(), r);
                nightStage++;
            }
        } else {
            wakeUpMorning();
        }
    }

    private void calculateNightKill() {
        //MAFIA KILL
        if (nightChoices.containsKey("Slut") && ((nightChoices.get("Slut").getRole().equals("Mafia") || nightChoices.get("Slut").getRole().equals("Godfather")) || nightChoices.get("Slut").getRole().equals("Silencer"))) {
            players.add(0, new Endpoint("DEATH", "nobody"));
        } else if (nightChoices.containsKey("Mafia") && nightChoices.containsKey("Doctor") && nightChoices.get("Mafia").equals(nightChoices.get("Doctor"))) {
            if (nightChoices.containsKey("Slut") && nightChoices.get("Slut").getRole().equals("Doctor")) {
                int killed = players.indexOf(nightChoices.get("Mafia"));
                Collections.swap(players, 0, killed);
            } else
                players.add(0, new Endpoint("DEATH", "nobody"));
        } else if (nightChoices.containsKey("Mafia")) {
            int killed = players.indexOf(nightChoices.get("Mafia"));
            Collections.swap(players, 0, killed);
        } else
            players.add(0, new Endpoint("DEATH", "nobody"));

        //VIGILANTE KILL
        if ((nightChoices.containsKey("Slut") && nightChoices.get("Slut").getRole().equals("Vigilante")) || !nightChoices.containsKey("Vigilante"))
            players.add(1, new Endpoint("DEATH", "nobody"));
        else {
            int killed = players.indexOf(nightChoices.get("Vigilante"));
            Collections.swap(players, 1, killed);
        }
    }

    private void calculateLynch() {
        Collections.sort(players);
        Response r;
        String logText;
        if (skipped >= players.get(0).getVotes()) {
            r = new Response(MafiaUtils.RESPONSE_TYPE_LYNCH, "Nobody");
            logText = "Send to: {%s, LYNCH: Nobody}";
        } else if (players.get(0).getVotes() == players.get(1).getVotes()) {
            r = new Response(MafiaUtils.RESPONSE_TYPE_ACK, "There is a tie, please vote again");
            logText = "Send to: {%s, ACK: Vote Again}";
        } else if (players.get(0).getRole().equals("Hunter")) {
            r = new Response(MafiaUtils.RESPONSE_TYPE_HUNTER, players.get(0).getName());
            logText = "Send to: {%s, HUNTER: " + players.get(0).getName() + "}";
            hunterVote = true;
        } else {
            r = new Response(MafiaUtils.RESPONSE_TYPE_LYNCH, players.get(0).getName());
            logText = "Send to: {%s, LYNCH: " + players.get(0).getName() + "}";
        }
        clearAllVotes();
        for (Endpoint player : players) {
            sendDataToPlayer(player.getId(), r);
            MafiaUtils.addToLogFile(String.format(logText, player.getName()), gameName + ".txt");
        }
        if (players.get(0).getName().equals(r.getData())) {
            gameRoles.remove(players.get(0).getRole());
            players.remove(0);
        }
    }

    private void wakeUpMorning() {
        isNight = false;
        mafiaVoted = false;
        calculateNightKill();
        calculateSilence();
        Endpoint mafiaKill = players.get(0);
        Endpoint vigilanteKill = players.get(1);
        players.remove(0);
        players.remove(0);
        int isOver = isGameOver();
        if (isOver == 3) {
            players.add(0, mafiaKill);
            players.add(1, vigilanteKill);
            Response r = new Response(MafiaUtils.RESPONSE_TYPE_DEATH, players);
            for (Endpoint p : players) {
                if (nightChoices.containsKey("Silencer") && !nightChoices.get("Silencer").getName().equals(players.get(0).getName())) {
                    sendDataToPlayer(p.getId(), new Response(MafiaUtils.RESPONSE_TYPE_SILENCE, nightChoices.get("Silencer")));
                    MafiaUtils.addToLogFile("Send to: {" + p.getName() + ", SILENCE: " + nightChoices.get("Silencer") + "}", gameName + ".txt");
                }
                sendDataToPlayer(p.getId(), r);
                MafiaUtils.addToLogFile("Send to: {" + p.getName() + ", DEATH: " + r.getData() + "}", gameName + ".txt");
            }
            players.remove(0);
            players.remove(0);
        } else {
            players.add(0, mafiaKill);
            players.add(1, vigilanteKill);
            Response r = new Response(MafiaUtils.RESPONSE_TYPE_OVER, MafiaUtils.WINNER[isOver]);
            for (Endpoint p : players) {
                sendDataToPlayer(p.getId(), r);
                MafiaUtils.addToLogFile("Send to: {" + p.getName() + ", " + r.getData() + " win.}", gameName + ".txt");
            }
            playersJoined = false;
            onBackPressed();
        }
    }

    private void calculateSilence() {
        if (nightChoices.containsKey("Silencer"))
            if (nightChoices.containsKey("Slut") && nightChoices.get("Slut").getRole().equals("Silencer"))
                nightChoices.remove("Silencer");
    }

    private boolean assignRoles(int n) {
        ArrayList<String> roles = new ArrayList<>();
        ArrayList<Integer> selections = adapter.getSelections();
        Collections.sort(selections);
        gameRoles = new ArrayList<>();
        for (int i = 0; i < selections.size(); i++) {
            roles.add(MafiaUtils.CHARACTER_TYPES.get(selections.get(i)));
        }
        gameRoles.addAll(roles);
        sandmanValid = gameRoles.contains("Sandman");
        havePresident = gameRoles.contains("President");
        gameRoles.remove("President");
        gameRoles.remove("Hunter");
        roles.add("Godfather");
        if (roles.contains("Silencer")) {
            if ((n / 3) - 2 < 0)
                return false;
            roles.addAll(Collections.nCopies((n / 3) - 2, "Mafia"));
        }
        else {
            if ((n / 3) - 1 < 0)
                return false;
            roles.addAll(Collections.nCopies((n / 3) - 1, "Mafia"));
        }
        if (n - roles.size() < 0)
            return false;
        roles.addAll(Collections.nCopies(n - roles.size(), "Villager"));
        Collections.shuffle(roles);
        Collections.shuffle(players);
        for (int i = 0; i < players.size(); i++)
            players.get(i).setRole(roles.get(i));
        return true;
    }

    private Endpoint getEndpointWithId(String eid) {
        for (Endpoint e : players)
            if (e.getId().equals(eid))
                return e;
        return null;
    }

    private Endpoint getEndpointWithName(String name) {
        for (Endpoint e : players)
            if (e.getName().equals(name))
                return e;
        return null;
    }

    private Endpoint getEndpointWithRole(String role) {
        for (Endpoint e : players)
            if (e.getRole().equals(role))
                return e;
        return null;
    }

    private void sendDataToPlayer(String id, Object data) {
        try {
            mConnectionsClient.sendPayload(id, Payload.fromBytes(MafiaUtils.serialize(data)));
        } catch (Exception e) {
            Log.e("Sending data", "Failed");
            e.printStackTrace();
        }
    }

    private ArrayList<Endpoint> getMafia() {
        ArrayList<Endpoint> list = new ArrayList<>();
        for (Endpoint e : players)
            if (e.getRole().equals("Mafia") || e.getRole().equals("Godfather") || e.getRole().equals("Silencer"))
                list.add(e);
        return list;
    }

    private void showKeyboard() {
        ((InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private void animateTransition() {
        configLayout.animate().scaleX(0.0f).scaleY(0.0f).setDuration(300).setListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                configLayout.setVisibility(View.GONE);
                connectionLayout.setVisibility(View.VISIBLE);
                connectionLayout.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_in));
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
