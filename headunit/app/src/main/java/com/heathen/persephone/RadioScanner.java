package com.heathen.persephone;

import android.Manifest;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.net.wifi.*;
import android.os.*;
import android.util.SparseArray;
import org.json.*;
import java.time.Instant;
import java.util.*;

/** Discovery only. No connection/write/injection logic lives in this scanner. */
final class RadioScanner {
    interface Sink { void observation(JSONObject value); void status(String message); }
    private final Context context;
    private final Sink sink;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final WifiManager wifi;
    private final BluetoothAdapter adapter;
    private final LocationManager locationManager;
    private BluetoothLeScanner ble;
    private Location fix;
    private boolean running, registered;
    private int generation;
    private final LinkedHashMap<String, Long> wifiTimes = new LinkedHashMap<>(), bleTimes = new LinkedHashMap<>();
    RadioScanner(Context context, Sink sink) {
        this.context = context; this.sink = sink;
        wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bm == null ? null : bm.getAdapter();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }
    BluetoothAdapter adapter() { return adapter; }
    private final LocationListener locations = new LocationListener() {
        public void onLocationChanged(Location value) {
            if(!value.hasAccuracy()||!Float.isFinite(value.getAccuracy())||value.getAccuracy()<=0)return;
            long now=SystemClock.elapsedRealtime();
            if(fix!=null&&now-fix.getElapsedRealtimeNanos()/1_000_000<=5000&&value.getAccuracy()>fix.getAccuracy()*1.3f)return;
            fix=value;
        }
        public void onProviderEnabled(String provider) {}
        public void onProviderDisabled(String provider) { sink.status("Location provider disabled; new observations may have no GPS"); }
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent intent) { if (running) readWifi(); }
    };
    private final ScanCallback callbacks = new ScanCallback() {
        @Override public void onScanResult(int type, android.bluetooth.le.ScanResult result) {
            final int epoch = generation;
            handler.post(() -> { if (running && epoch == generation) readBle(result); });
        }
        @Override public void onBatchScanResults(List<android.bluetooth.le.ScanResult> results) {
            for (android.bluetooth.le.ScanResult result : results) onScanResult(0, result);
        }
        @Override public void onScanFailed(int code) { handler.post(() -> sink.status("BLE scan failed (" + code + "). Check vendor BLE support / restart Bluetooth.")); }
    };
    void start() {
        if (running) return;
        running = true; generation++; wifiTimes.clear(); bleTimes.clear(); fix = null;
        try {
            IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            else context.registerReceiver(receiver, filter);
            registered = true;
            readWifi();
        } catch (Exception e) { sink.status("Wi-Fi discovery unavailable: " + e.getMessage()); }
        try {
            if (adapter == null || !adapter.isEnabled()) sink.status("BLE unavailable or switched off; head-unit Bluetooth may be proprietary");
            else {
                ble = adapter.getBluetoothLeScanner();
                if (ble == null) sink.status("No Android BLE scanner exposed");
                else ble.startScan(null, new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callbacks);
            }
        } catch (SecurityException e) { sink.status("Bluetooth permission missing or revoked"); }
        catch (Exception e) { sink.status("BLE discovery unavailable: " + e.getMessage()); }
        try {
            if (locationManager != null && context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER})
                    if (locationManager.isProviderEnabled(provider)) locationManager.requestLocationUpdates(provider, 1000, 0, locations, Looper.getMainLooper());
            }
        } catch (Exception e) { sink.status("GPS unavailable: " + e.getMessage()); }
    }
    void stop() {
        running = false; generation++;
        if (registered) { context.unregisterReceiver(receiver); registered = false; }
        if (ble != null) { try { ble.stopScan(callbacks); } catch (SecurityException | IllegalStateException ignored) {} ble = null; }
        if (locationManager != null) { try { locationManager.removeUpdates(locations); } catch (SecurityException ignored) {} }
    }
    boolean requestWifiScan() {
        if (!running || wifi == null) return false;
        try { return wifi.startScan(); } catch (Exception e) { sink.status("Wi-Fi request rejected: " + e.getMessage()); return false; }
    }
    private void readWifi() {
        if (wifi == null) { sink.status("Wi-Fi API unavailable"); return; }
        try {
            if (!wifi.isWifiEnabled()) sink.status("Wi-Fi switched off. Enable it in Android settings.");
            for (android.net.wifi.ScanResult r : wifi.getScanResults()) {
                long age = SystemClock.elapsedRealtime() - r.timestamp / 1000;
                if (age < 0 || age > RfCore.FRESH_MS || Objects.equals(wifiTimes.get(r.BSSID), r.timestamp)) continue;
                wifiTimes.put(r.BSSID, r.timestamp); trim(wifiTimes);
                JSONObject row = base("Wi-Fi", r.BSSID, r.SSID, r.level);
                row.put("frequency", r.frequency).put("security", r.capabilities).put("scan_age_ms", age);
                row.put("evidence", RfCore.evidence(r.BSSID, r.SSID, Collections.emptyList(), Collections.emptySet()));
                addGps(row); sink.observation(row);
            }
        } catch (SecurityException e) { sink.status("Precise location permission missing or revoked"); }
        catch (Exception e) { sink.status("Wi-Fi needs precise location permission and Location enabled: " + e.getMessage()); }
    }
    private void readBle(android.bluetooth.le.ScanResult result) {
        try {
            String address = result.getDevice().getAddress();
            long now = SystemClock.elapsedRealtime();
            long received = result.getTimestampNanos() / 1_000_000;
            if (now < received || now - received > RfCore.FRESH_MS) return;
            if (bleTimes.containsKey(address) && now - bleTimes.get(address) < 1000) return;
            bleTimes.put(address, now); trim(bleTimes);
            ScanRecord record = result.getScanRecord();
            String name = record == null || record.getDeviceName() == null ? "" : record.getDeviceName();
            List<String> services = new ArrayList<>(); Set<Integer> manufacturers = new HashSet<>();
            JSONObject payloads = new JSONObject();
            if (record != null) {
                if (record.getServiceUuids() != null) for (ParcelUuid uuid : record.getServiceUuids()) services.add(uuid.toString());
                SparseArray<byte[]> data = record.getManufacturerSpecificData();
                for (int i = 0; i < data.size(); i++) { manufacturers.add(data.keyAt(i)); payloads.put(String.format(Locale.ROOT, "%04X", data.keyAt(i)), hex(data.valueAt(i))); }
            }
            JSONObject row = base("BLE", address, name, result.getRssi());
            row.put("seen_elapsed", received).put("services", new JSONArray(services)).put("manufacturer_data", payloads)
                .put("connectable", result.isConnectable()).put("evidence", RfCore.evidence(address, name, services, manufacturers));
            addGps(row); sink.observation(row);
        } catch (Exception e) { sink.status("BLE observation rejected: " + e.getMessage()); }
    }
    private JSONObject base(String radio, String address, String name, int rssi) throws JSONException {
        long now = SystemClock.elapsedRealtime();
        JSONObject row = new JSONObject().put("schema", 1).put("source", "ANDROID").put("time", Instant.now().toString())
            .put("radio", radio).put("address", address).put("name", name == null ? "" : name).put("rssi", rssi).put("seen_elapsed", now);
        return row;
    }
    void addGps(JSONObject row) throws JSONException {
        long now = SystemClock.elapsedRealtime();
        if (fix != null) {
            long age = now - fix.getElapsedRealtimeNanos() / 1_000_000;
            long scanAge=row.optLong("scan_age_ms",0);
            if (age >= 0 && age <= 10_000 && scanAge>=0 && scanAge<=5_000 && fix.hasAccuracy()) {
                row.put("latitude", fix.getLatitude()).put("longitude", fix.getLongitude()).put("accuracy", fix.getAccuracy()+(fix.hasSpeed()?fix.getSpeed()*(age+scanAge)/1000.0:0)).put("gps_age_ms", age).put("gps_mock",fix.isFromMockProvider());
            }
        }
    }
    private static void trim(LinkedHashMap<String, Long> map) { while (map.size() > 4096) map.remove(map.keySet().iterator().next()); }
    private static String hex(byte[] data) { StringBuilder s = new StringBuilder(); for (byte b : data) s.append(String.format(Locale.ROOT, "%02x", b & 255)); return s.toString(); }
}
