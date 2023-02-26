package com.yakovlevegor.DroidRec.shake.event;

public class OnShakePreferenceChangeEvent {
    private String state;
    public OnShakePreferenceChangeEvent(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
}
