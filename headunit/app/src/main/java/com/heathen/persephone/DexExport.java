package com.heathen.persephone;
import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** Plaintext species/customization/progress export, without device addresses. */
final class DexExport {
    static void export(Context context,Uri tree)throws Exception{JSONArray rows=new JSONArray();for(JSONObject creature:DexCatalog.entries(context)){JSONObject row=new JSONObject(creature.toString());String id=row.getString("id");row.put("name",DexCatalog.title(context,id)).put("function",DexCatalog.function(context,creature)).put("field_sightings",DexLedger.count(context,id,false)).put("demo_sightings",DexLedger.count(context,id,true));rows.put(row);}Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));Uri file=DocumentsContract.createDocument(context.getContentResolver(),parent,"application/json","korego-dex-"+System.currentTimeMillis()+".json");if(file==null)throw new IOException("Cannot create Dex export");try(OutputStream out=context.getContentResolver().openOutputStream(file,"wt")){if(out==null)throw new IOException("Cannot open Dex export");out.write(new JSONObject().put("schema",1).put("creatures",rows).toString().getBytes(StandardCharsets.UTF_8));}catch(Exception failure){try{DocumentsContract.deleteDocument(context.getContentResolver(),file);}catch(Exception ignored){}throw failure;}}
}
