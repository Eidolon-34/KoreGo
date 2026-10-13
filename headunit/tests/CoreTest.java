package com.heathen.persephone;
import java.util.*;
public final class CoreTest {
    private static int count;
    static void check(boolean value, String message) { count++; if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        check(RfCore.csv("a,\"b\"\n").equals("\"a,\"\"b\"\"\n\""), "CSV quoting");
        check(RfCore.csv(" \t=HYPERLINK(\"x\")").startsWith("\"'"), "SSID formula guard");
        check(RfCore.csv("@SUM(1)").startsWith("\"'"), "formula guard");
        check(RfCore.evidence("24:6F:28:00:00:01", "", List.of(), Set.of()).isEmpty(), "generic chip vendor must not imply surveillance");
        check(RfCore.evidence("B4:1E:52:00:00:01", "", List.of(), Set.of()).contains("does not establish"), "vendor uncertainty");
        check(RfCore.evidence("02:00:00:00:00:01", "Penguin-123", List.of(), Set.of()).contains("unverified"), "name evidence for randomized address");
        check(RfCore.evidence("", "", List.of("e8ccbb38-9532-46a8-9fe5-1814df172e6f"), Set.of()).contains("accessory"), "service evidence");
        check(RfCore.evidence("", "", List.of(), Set.of(0x09c8)).contains("weak"), "shared manufacturer uncertainty");
        check(!RfCore.matches(" , ", "AA:BB:CC:11:22:33", "hello", "", List.of()), "empty watchlist");
        check(RfCore.matches(" xx, AA:BB:CC ", "aa:bb:cc:11:22:33", "hello", "", List.of()), "case insensitive prefix");
        check(!RfCore.allowedTarget("bad", 100, 200, false, true), "malformed target");
        check(!RfCore.allowedTarget("AA:BB:CC:11:22:33", 100, 200, true, true), "demo cannot connect");
        check(!RfCore.allowedTarget("AA:BB:CC:11:22:33", 100, 200, false, false), "consent required");
        check(!RfCore.allowedTarget("AA:BB:CC:11:22:33", 100, 30101, false, true), "stale target");
        check(!RfCore.allowedTarget("AA:BB:CC:11:22:33", 100, 99, false, true), "clock reversal");
        check(RfCore.allowedTarget("AA:BB:CC:11:22:33", 100, 30100, false, true), "fresh exact target");
        byte[] salt = RfCore.salt(), hash = RfCore.hash("9818".toCharArray(), salt);
        check(RfCore.verify("9818".toCharArray(), salt, hash), "default verifies");
        check(!RfCore.verify("9819".toCharArray(), salt, hash), "wrong PIN rejects");
        check(!Arrays.equals(hash, RfCore.hash("9818".toCharArray(), RfCore.salt())), "per-install salt");
        check(Math.abs(RfCore.smooth(-80, -60) - -73) < .01, "RSSI smoothing");
        System.out.println(count + " core policy/export checks passed");
    }
}
