package com.heathen.persephone;
import android.content.*;
import android.graphics.*;
import android.location.Location;
import android.os.SystemClock;
import android.view.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import java.io.*;
import java.lang.reflect.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35,qualifiers="w420dp-h900dp-port-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class KoreGoTest {
    Context context;
    @Before public void prepare(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("gate",0).edit().clear().commit();context.getSharedPreferences("dex-ledger",0).edit().clear().commit();context.getSharedPreferences("dex",0).edit().clear().commit();}
    @Test public void catalogCustomizationAndDemoProgressStaySeparate()throws Exception {
        assertEquals(7,DexCatalog.entries(context).size());DexCatalog.customize(context,"raven","Workshop Bat","Document volunteered sensors");assertEquals("Workshop Bat",DexCatalog.title(context,"raven"));
        JSONObject row=new JSONObject().put("source","DEMO").put("radio","BLE").put("address","AA:BB:CC:00:00:01").put("tags","RAVEN");DexLedger.record(context,row);DexLedger.record(context,row);assertEquals(1,DexLedger.count(context,"raven",true));assertEquals(0,DexLedger.count(context,"raven",false));
        row.put("source","ANDROID");DexLedger.record(context,row);assertEquals(1,DexLedger.count(context,"raven",false));row.put("address","malformed");DexLedger.record(context,row);assertEquals(1,DexLedger.count(context,"raven",false));
        try{DexCatalog.customize(context,"invented","Name","function");fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void manualAssignmentPreservesEvidenceAndExportsDex()throws Exception {
        JSONObject row=new JSONObject().put("source","ANDROID").put("radio","BLE").put("address","AA:BB:CC:00:00:08").put("name","Unclassified example").put("time",java.time.Instant.now().toString()).put("latitude",36).put("longitude",-115).put("accuracy",5).put("rssi",-60);DetectionAnnotator.annotate(row);String before=row.optString("tags");PingStore store=PingStore.get(context);String id=store.observe(row);store.creature(id,"raven");assertEquals(before,store.getPing(id).optString("tags"));assertEquals("raven",DexCatalog.kind(PingStore.point(store.getPing(id)).tag,"BLE/test"));
        HeadunitTest.TestDocuments provider=new HeadunitTest.TestDocuments();android.content.pm.ProviderInfo info=new android.content.pm.ProviderInfo();info.authority="korego.dex.documents";info.exported=true;info.grantUriPermissions=true;info.readPermission="android.permission.MANAGE_DOCUMENTS";info.writePermission=info.readPermission;provider.attachInfo(context,info);org.robolectric.shadows.ShadowContentResolver.registerProviderInternal(info.authority,provider);DexCatalog.customize(context,"raven","Export Bat","GPS note collection");DexExport.export(context,android.provider.DocumentsContract.buildTreeDocumentUri(info.authority,"root"));File file=provider.root.listFiles()[0];String data=new String(java.nio.file.Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8);assertTrue(data.contains("Export Bat"));assertFalse(data.contains("AA:BB:CC"));assertEquals(7,new JSONObject(data).getJSONArray("creatures").length());
    }
    @Test public void bearingUsesRecordedGpsAndNeverRssi(){Location gps=new Location("gps");gps.setLatitude(36);gps.setLongitude(-115);gps.setAccuracy(5);gps.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        SignalMapModel.Point strong=new SignalMapModel.Point(36.002,-115,5,-40,"Wi-Fi/test","FLOCK",0,false),weak=new SignalMapModel.Point(36.002,-115,5,-100,"Wi-Fi/test","FLOCK",0,false);
        assertEquals(KoreGoActivity.direction(gps,strong),KoreGoActivity.direction(gps,weak));assertTrue(KoreGoActivity.direction(gps,strong).contains("0°"));assertTrue(KoreGoActivity.direction(gps,new SignalMapModel.Point(36,-115,5,-60,"","",0,false)).contains("uncertainty"));assertTrue(KoreGoActivity.direction(null,strong).contains("unresolved"));
    }
    @Test public void lockedMapCannotStartAndDemoNeverStartsRadios()throws Exception {
        try(ActivityController<KoreGoActivity> controller=Robolectric.buildActivity(KoreGoActivity.class).setup()){
            KoreGoActivity game=controller.get();invoke(game,"start",new Class[]{boolean.class},false);assertFalse((boolean)field(game,"live"));assertNull(field(game,"session"));
            assertTrue(new PinGate(context).verify("9818"));invoke(game,"startDemo",new Class[]{});assertFalse((boolean)field(game,"live"));assertFalse((boolean)field(field(game,"scanner"),"running"));assertTrue((boolean)field(game,"demoMode"));
            screenshot(game,"korego-map.png");controller.pause().stop();assertFalse((boolean)field(field(game,"scanner"),"running"));
        }
    }
    @Test public void dexRendersAndPersonalCustomizationPersists()throws Exception {
        assertTrue(new PinGate(context).verify("9818"));try(ActivityController<SignalDexActivity> controller=Robolectric.buildActivity(SignalDexActivity.class).setup()){assertFalse(controller.get().isFinishing());screenshot(controller.get(),"korego-dex.png");}DexCatalog.customize(context,"flock","Sentinel","Record GPS observations");assertEquals("Sentinel",context.getSharedPreferences("dex",0).getString("flock.name",""));
    }
    @Test public void integratedMapFollowUsesPhoneGpsNotLastSignal()throws Exception {
        TileCache tiles=new TileCache(context);SignalMapView map=new SignalMapView(context,tiles,key->{});Location gps=new Location("gps");gps.setLatitude(36);gps.setLongitude(-115);gps.setAccuracy(5);gps.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());map.receiver(gps);map.setData(java.util.Collections.singletonList(new SignalMapModel.Point(37,-114,5,-60,"Wi-Fi/test","FLOCK",0,false)),"");assertEquals(36,(double)field(map,"lat"),.000001);assertEquals(-115,(double)field(map,"lon"),.000001);tiles.close();
    }
    private static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static void invoke(Object object,String name,Class[] types,Object... values)throws Exception{Method m=object.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(object,values);}
    private static void screenshot(android.app.Activity activity,String name)throws Exception{View view=activity.getWindow().getDecorView();view.measure(View.MeasureSpec.makeMeasureSpec(420,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));view.layout(0,0,420,900);Bitmap image=Bitmap.createBitmap(420,900,Bitmap.Config.ARGB_8888);view.draw(new Canvas(image));File folder=new File("build/previews");folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}}
}
