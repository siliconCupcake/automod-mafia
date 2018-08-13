package com.deltaforce.siliconcupcake.themodfather;

import java.io.Serializable;

public class Request implements Serializable {
    private int type;
    private String data;

    public Request() {
    }

    public Request (int t, String d) {
        type = t;
        data = d;
    }

    public int getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setData(String data) {
        this.data = data;
    }
}

