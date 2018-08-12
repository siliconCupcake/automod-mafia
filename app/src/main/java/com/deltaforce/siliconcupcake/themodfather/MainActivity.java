package com.deltaforce.siliconcupcake.themodfather;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.greeting)
    TextView greetText;

    @BindView(R.id.choose_role)
    LinearLayout rolePicker;

    @BindView(R.id.pick_mod)
    Button pickMod;

    @BindView(R.id.pick_player)
    Button pickPlayer;

    public boolean permissionsGranted;
    private int mode;
    public static final int MY_PERMISSIONS_REQUEST_PLAYER = 99;
    public static final int MY_PERMISSIONS_REQUEST_MOD = 91;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionsGranted = getSharedPreferences("appdata", MODE_PRIVATE).getBoolean("Permissions", false);

        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setCustomView(R.layout.action_bar);

        ButterKnife.bind(this);

        pickPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mode = 1;
                if (permissionsGranted) {
                    getSharedPreferences("appdata", MODE_PRIVATE).edit().putBoolean("Permissions", permissionsGranted).apply();
                    startActivity(new Intent(getApplicationContext(), PlayerActivity.class));
                    overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
                } else {
                    permissionsGranted = checkPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_PLAYER);
                }
            }
        });

        pickMod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mode = 0;
                if (permissionsGranted) {
                    getSharedPreferences("appdata", MODE_PRIVATE).edit().putBoolean("Permissions", permissionsGranted).apply();
                    startActivity(new Intent(getApplicationContext(), ModActivity.class));
                    overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
                } else {
                    permissionsGranted = checkPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_MOD);
                }
            }
        });

    }

    private boolean checkPermissions(String[] permissions, int reqCode){
        ArrayList<String> temp = new ArrayList<>();
        for(String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) {
                temp.add(perm);
            }
        }
        String[] required = new String[temp.size()];
        required = temp.toArray(required);
        if(required.length == 0){
            return true;
        }
        else{
            ActivityCompat.requestPermissions(this, required, reqCode);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted = false;
                    Snackbar.make(rolePicker, "Please allow all permissions to proceed", Snackbar.LENGTH_LONG).show();
                    return;
                }
            }
        }
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_MOD:
                pickMod.performClick();
                break;

            case MY_PERMISSIONS_REQUEST_PLAYER:
                pickPlayer.performClick();
                break;

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
