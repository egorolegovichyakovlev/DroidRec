package com.yakovlevegor.DroidRec.shake.event;

public class ServiceConnectedEvent {
    private boolean isServiceConnected;

    public ServiceConnectedEvent(boolean isServiceConnected) {
        this.isServiceConnected = isServiceConnected;
    }

    public boolean isServiceConnected() {
        return isServiceConnected;
    }
}
