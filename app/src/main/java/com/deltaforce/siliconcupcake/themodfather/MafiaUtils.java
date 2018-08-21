package com.deltaforce.siliconcupcake.themodfather;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

public class MafiaUtils {

    public static final String SERVICE_ID = "com.deltaforce.siliconcupcake.themodfather";

    public static final ArrayList<String> CHARACTER_TYPES = new ArrayList<>(Arrays.asList("Slut", "Doctor", "Cop", "Silencer", "President", "Hunter"));
    public static final ArrayList<String> CHARACTER_HINTS = new ArrayList<>(Arrays.asList("A villager who can cancel the power of a player each. The Slut cannot sleep with the same player twice in a row.",
            "A villager who can protect a player from being killed each night.",
            "A villager who can learn the identity of a player each night.",
            "A Mafioso who chooses a player each night and mutes the player during the day.",
            "A villager whose death results in the Mafia winning. The identity of the President is known to all vilagers but not to the Mafia.",
            "A villager who can choose to kill another player when lynched"));
    public static final String[] WINNER = {"Mafia", "Villagers", "President"};

    public static final String WAKE_UP_MORNING = "Village wakes up to the death of ";

    public static final int RESPONSE_TYPE_ROLE = 71;
    public static final int RESPONSE_TYPE_DEATH = 74;
    public static final int RESPONSE_TYPE_ACK = 73;
    public static final int RESPONSE_TYPE_WAKE = 75;
    public static final int RESPONSE_TYPE_OVER = 76;
    public static final int RESPONSE_TYPE_COP = 72;
    public static final int RESPONSE_TYPE_LYNCH = 77;
    public static final int RESPONSE_TYPE_SILENCE = 70;
    public static final int RESPONSE_TYPE_HUNTER = 79;

    public static final int REQUEST_TYPE_CONTINUE = 81;
    public static final int REQUEST_TYPE_VOTE = 82;

    public static final String REQUEST_DATA_SLEPT = "SLEPT";
    public static final String REQUEST_DATA_SKIP = "SKIP";

    public static void setDefaultFont(Context context,
                                      String staticTypefaceFieldName, String fontAssetName) {
        final Typeface regular = Typeface.createFromAsset(context.getAssets(),
                fontAssetName);
        replaceFont(staticTypefaceFieldName, regular);
    }

    public static void replaceFont(String staticTypefaceFieldName,
                                   final Typeface newTypeface) {
        try {
            final Field staticField = Typeface.class
                    .getDeclaredField(staticTypefaceFieldName);
            staticField.setAccessible(true);
            staticField.set(null, newTypeface);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] serialize(Object object) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
        objectOutputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        return objectInputStream.readObject();
    }

    public static void addToLogFile(String logText, String fileName) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TheModfather";
        File logDir = new File(path);
        if (!logDir.exists())
            if(!logDir.mkdir())
                Log.e("DIRECTORY", "Unable to create directory");
        try {
            File logFile = new File(path, fileName);
            if (!logFile.exists())
                logFile.createNewFile();
            FileWriter writer = new FileWriter(logFile, true);
            writer.append(logText);
            writer.append(System.lineSeparator());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
