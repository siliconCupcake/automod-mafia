package com.deltaforce.siliconcupcake.themodfather;

import android.support.annotation.NonNull;

public class Endpoint {
    @NonNull private final String id;
    @NonNull private final String name;
    private String role;
    private int votes;

    public Endpoint(@NonNull String id, @NonNull String name) {
        this.id = id;
        this.name = name;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Endpoint) {
            Endpoint other = (Endpoint) obj;
            return id.equals(other.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Endpoint{id=%s, name=%s}", id, name);
    }

}
