package com.heathen.persephone;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Original creature metadata, editable descriptions only. No executable actions. */
final class DexCatalog {
    private static List<JSONObject> cached;
    static synchronized List<JSONObject> entries(Context context){if(cached!=null)return new ArrayList<>(cached);try(InputStream in=context.getAssets().open("creatures.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1){if(out.size()+n>1024*1024)throw new IOException("Catalog too large");out.write(bytes,0,n);}JSONArray rows=new JSONArray(out.toString("UTF-8"));List<JSONObject> result=new ArrayList<>();Set<String> ids=new HashSet<>();for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);String id=row.getString("id");if(!id.matches("[a-z]{1,24}")||!ids.add(id))throw new IOException("Invalid catalog identifier");row.getString("name");row.getString("function");result.add(row);}cached=result;return new ArrayList<>(result);}catch(Exception e){throw new IllegalStateException("Creature catalog unavailable",e);}}
    static String kind(String tags,String key){java.util.regex.Matcher assignment=java.util.regex.Pattern.compile("@creature=([a-z]{1,24})").matcher(tags);if(assignment.find())return assignment.group(1);if(tags.contains("FALCON")||tags.contains("LPR"))return "falcon";if(tags.contains("RAVEN"))return "raven";if(tags.contains("FLOCK"))return "flock";if(tags.contains("CAMERA"))return "camera";if(key.contains("BLE/"))return "ble";if(key.contains("Wi-Fi/"))return "wifi";return "unknown";}
    static String kind(JSONObject ping){return ping.optString("creature_id",kind(ping.optString("tags"),ping.optString("key",ping.optString("radio")+"/"+ping.optString("address"))));}
    static String title(Context context,String id){String custom=context.getSharedPreferences("dex",0).getString(id+".name","");if(!custom.isEmpty())return custom;for(JSONObject row:entries(context))if(row.optString("id").equals(id))return row.optString("name");return "Static Sprite";}
    static String function(Context context,JSONObject row){return context.getSharedPreferences("dex",0).getString(row.optString("id")+".function",row.optString("function"));}
    static void customize(Context context,String id,String name,String function){boolean valid=false;for(JSONObject row:entries(context))if(row.optString("id").equals(id))valid=true;if(!valid)throw new IllegalArgumentException("Unknown creature");if(name.trim().isEmpty()||name.length()>60||function.length()>1000)throw new IllegalArgumentException("Enter a name ≤60 and description ≤1000 characters");context.getSharedPreferences("dex",0).edit().putString(id+".name",name.trim()).putString(id+".function",function.trim()).apply();}
}
