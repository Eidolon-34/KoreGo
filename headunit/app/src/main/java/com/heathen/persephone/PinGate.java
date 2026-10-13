package com.heathen.persephone;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

final class PinGate {
    private final SharedPreferences prefs;
    PinGate(Context context) {
        prefs = context.getSharedPreferences("gate", Context.MODE_PRIVATE);
        if (!prefs.contains("hash")) set("9818", true);
    }
    boolean isDefault() { return prefs.getBoolean("default", true); }
    long remainingMs() { return Math.max(0, prefs.getLong("blockedUntil", 0) - System.currentTimeMillis()); }
    boolean verify(String pin) {
        if (remainingMs() > 0) return false;
        boolean ok = RfCore.verify(pin.toCharArray(), decode("salt"), decode("hash"));
        int failures = ok ? 0 : prefs.getInt("failures", 0) + 1;
        SharedPreferences.Editor editor = prefs.edit().putInt("failures", failures);
        if (!ok && failures >= 5) editor.putLong("blockedUntil", System.currentTimeMillis() + 60_000).putInt("failures", 0);
        if (!editor.commit()) throw new IllegalStateException("Cannot persist PIN attempts");
        return ok;
    }
    void set(String pin, boolean initial) {
        byte[] salt = RfCore.salt();
        byte[] hash = RfCore.hash(pin.toCharArray(), salt);
        if (!prefs.edit().putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .putBoolean("default", initial).putInt("failures", 0).putLong("blockedUntil", 0).commit())
            throw new IllegalStateException("Cannot persist PIN");
    }
    private byte[] decode(String key) { return Base64.decode(prefs.getString(key, ""), Base64.NO_WRAP); }
}
