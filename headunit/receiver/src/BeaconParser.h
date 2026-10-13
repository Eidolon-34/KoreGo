#pragma once
#include <cstddef>
#include <cstdint>
#include <cstring>

struct WifiMetadata { uint8_t address[6]; char name[33]; bool beacon; bool privacy; };
// No frame payload escapes this parser. Probe SSIDs are omitted.
inline bool parseWifiMetadata(const uint8_t* p, size_t length, WifiMetadata& out) {
    if (!p || length < 24 || (p[0] & 0x0f) != 0) return false;
    uint8_t subtype = (p[0] >> 4) & 15;
    if (subtype != 8 && subtype != 4) return false;
    out = {}; memcpy(out.address, p + 10, 6); out.beacon = subtype == 8;
    if (!out.beacon) return true;
    if (length < 40) return false; // fixed beacon header plus FCS
    out.privacy = (p[34] & 0x10) != 0;
    for (size_t i = 36; i + 2 <= length - 4;) {
        size_t n = p[i + 1]; if (i + 2 + n > length - 4) return false;
        if (p[i] == 0) { if (n > 32) return false; memcpy(out.name, p + i + 2, n); out.name[n] = 0; return true; }
        i += 2 + n;
    }
    return true;
}
