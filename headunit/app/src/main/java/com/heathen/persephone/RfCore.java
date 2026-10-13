package com.heathen.persephone;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Platform-independent policy and export helpers, shared with host tests. */
public final class RfCore {
    private RfCore() {}
    public static final int MAX_LIVE = 512;
    public static final long FRESH_MS = 30_000;
    public static final String CSV_HEADER = "source,observed_at_utc,radio,address,name,rssi_dbm,frequency_mhz,security,evidence,receiver_latitude,receiver_longitude,receiver_accuracy_m,gps_age_ms\n";
    public static String csv(String value) {
        if (value == null) value = "";
        // Neutralize formulas even when SSIDs contain leading whitespace/control chars.
        String stripped = value.replaceFirst("^[\\s\\p{Cntrl}]+", "");
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    public static boolean validMac(String mac) { return mac != null && mac.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"); }
    public static String evidence(String address, String name, Collection<String> services, Set<Integer> manufacturers) {
        List<String> evidence = new ArrayList<>();
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.startsWith("flock") || lower.startsWith("penguin-") || lower.equals("fs ext battery"))
            evidence.add("name resembles community Flock signature; unverified");
        if (validMac(address) && address.toUpperCase(Locale.ROOT).startsWith("B4:1E:52:"))
            evidence.add("Flock vendor prefix; does not establish device type");
        if (manufacturers.contains(0x09c8)) evidence.add("XUNTONG manufacturer ID; shared vendor, weak evidence");
        for (String service : services) {
            String s = service.toLowerCase(Locale.ROOT);
            if (s.equals("e8ccbb38-9532-46a8-9fe5-1814df172e6f")) evidence.add("community Flock accessory service; unverified");
            if (s.matches("00003[1-5]00-0000-1000-8000-00805f9b34fb")) evidence.add("community Raven service; unverified");
        }
        return String.join("; ", evidence);
    }
    public static boolean matches(String watchlist, String address, String name, String evidence, Collection<String> services) {
        String haystack = (address + " " + name + " " + evidence + " " + String.join(" ", services)).toLowerCase(Locale.ROOT);
        for (String token : watchlist.split(",")) {
            String t = token.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty() && haystack.contains(t)) return true;
        }
        return false;
    }
    public static byte[] salt() { byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt); return salt; }
    public static byte[] hash(char[] pin, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(pin, salt, 120_000, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (Exception e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); Arrays.fill(pin, '\0'); }
    }
    public static boolean verify(char[] pin, byte[] salt, byte[] expected) { return MessageDigest.isEqual(hash(pin, salt), expected); }
    public static boolean allowedTarget(String address, long seenElapsed, long nowElapsed, boolean demo, boolean consent) {
        return validMac(address) && !demo && consent && nowElapsed >= seenElapsed && nowElapsed - seenElapsed <= FRESH_MS;
    }
    public static float smooth(float previous, int next) { return previous * .65f + next * .35f; }
}
