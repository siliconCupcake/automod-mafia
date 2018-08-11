package com.deltaforce.siliconcupcake.themodfather;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MafiaUtils {

    public static final ArrayList<String> CHARACTER_TYPES = new ArrayList<>(Arrays.asList("Doctor", "Slut", "Cop", "Vigilante", "Arson", "Doctor", "Slut", "Cop", "Vigilante", "Arson"));
    public static final String WAKE_UP_MORNING = "Village wakes up to the death of ";
    public static final String WAKE_UP_NIGHT = "Wake up.";
    public static final String GO_TO_SLEEP = "Village goes to sleep.";

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
}
