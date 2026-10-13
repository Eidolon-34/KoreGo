package com.heathen.persephone;
import android.content.*;
import org.json.JSONObject;
import java.util.*;
/** Bounded, local unique species sightings; simulated progress is isolated from field progress. */
final class DexLedger {
    static synchronized void record(Context context,JSONObject row){String radio=row.optString("radio"),address=row.optString("address");if(!radio.equals("Wi-Fi")&&!radio.equals("BLE"))return;if(!RfCore.validMac(address))return;
        String kind=DexCatalog.kind(row),scope=row.optString("source").equals("DEMO")?"demo":"field",key=scope+"/"+kind+"/"+PingStore.id(radio+"/"+address.toUpperCase(Locale.ROOT));SharedPreferences prefs=context.getSharedPreferences("dex-ledger",0);Set<String> seen=new HashSet<>(prefs.getStringSet("seen",Collections.emptySet()));if(seen.contains(key)||seen.size()>=4096)return;seen.add(key);String count=scope+"."+kind;prefs.edit().putStringSet("seen",seen).putInt(count,prefs.getInt(count,0)+1).apply();
    }
    static int count(Context context,String kind,boolean demo){return context.getSharedPreferences("dex-ledger",0).getInt((demo?"demo":"field")+"."+kind,0);}
}
