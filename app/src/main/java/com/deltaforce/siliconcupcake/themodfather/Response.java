package com.deltaforce.siliconcupcake.themodfather;

import java.io.Serializable;

public class Response implements Serializable {
    private int type;
    private Object data;

    public Response() {

    }

    public Response (int t, Object d) {
        type = t;
        data = d;
    }

    public int getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
