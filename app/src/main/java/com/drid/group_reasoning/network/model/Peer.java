package com.drid.group_reasoning.network.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class Peer implements Parcelable {

    private String peerId;
    private String name;
    private String status;


    public static final String AVAILABLE = "Available";
    public static final String CONNECTING = "Connecting";
    public static final String CONNECTED = "Connected";

    public Peer(){

    }

    public Peer(String peerId, String name, String status) {
        this.peerId = peerId;
        this.name = name;
        this.status = status;
    }

    protected Peer(Parcel in) {
        peerId = in.readString();
        name = in.readString();
        status = in.readString();
    }

    public static final Creator<Peer> CREATOR = new Creator<Peer>() {
        @Override
        public Peer createFromParcel(Parcel in) {
            return new Peer(in);
        }

        @Override
        public Peer[] newArray(int size) {
            return new Peer[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(peerId);
        dest.writeString(name);
        dest.writeString(status);
    }


    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    @NonNull
    @Override
    public String toString() {
        return "Peer {id: " + peerId + ", name: " + name + " status " + status + "}";
    }
}
