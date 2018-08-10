package com.deltaforce.siliconcupcake.themodfather;

import android.app.Application;

public class ModfatherApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        TypefaceUtil.setDefaultFont(this, "SERIF", "fonts/Product-Sans-Regular.ttf");

    }
}
