package com.deltaforce.siliconcupcake.themodfather;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MafiaUtils {

    public static final String SERVICE_ID = "com.deltaforce.siliconcupcake.themodfather";

    public static final ArrayList<String> CHARACTER_TYPES = new ArrayList<>(Arrays.asList("Doctor", "Slut", "Cop", "Vigilante", "Arson"));

    public static final String WAKE_UP_MORNING = "Village wakes up to the death of ";
    public static final String WAKE_UP_NIGHT = "Wake up.";
    public static final String GO_TO_SLEEP = "Village goes to sleep.";

    public static final int RESPONSE_TYPE_ROLE = 71;
    public static final int RESPONSE_TYPE_DEATH = 74;
    public static final int RESPONSE_TYPE_ACK = 73;
    public static final int RESPONSE_TYPE_COP = 72;

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
        // transform object to stream and then to a byte array
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
}
