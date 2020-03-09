package com.barcodeprinter;

import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.argox.sdk.barcodeprinter.BarcodePrinter;
import com.argox.sdk.barcodeprinter.BarcodePrinterIllegalArgumentException;
import com.argox.sdk.barcodeprinter.connection.ConnectionState;
import com.argox.sdk.barcodeprinter.connection.IConnectionStateListener;
import com.argox.sdk.barcodeprinter.connection.tcp.TCPConnection;
import com.argox.sdk.barcodeprinter.emulation.pplz.PPLZ;
import com.argox.sdk.barcodeprinter.emulation.pplz.PPLZFont;
import com.argox.sdk.barcodeprinter.emulation.pplz.PPLZOrient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import android.os.StrictMode;
public class RNAndroidBarcodePrinterModule extends ReactContextBaseJavaModule {

    public static ReactApplicationContext reactContext;
    public BarcodePrinter<TCPConnection, PPLZ> printer;
    public ConnectionState LastState = ConnectionState.Connecting;
    public String FilePath;
    public String Ip = "";
    public int PortNumber = 0;

    public RNAndroidBarcodePrinterModule(ReactApplicationContext reactContext) {
        super(reactContext);
        
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNAndroidBarcodePrinter";
    }

    @ReactMethod
    public void PrintBarcode(String data, String PrinterIpAdress, int PortNum) {
        try {
            Ip = PrinterIpAdress;
            PortNumber = PortNum;

            try {

                FilePath = Environment.getExternalStorageDirectory().toString() + "/Printer.txt";
                File BarcodeFile = new File(FilePath);
                if (BarcodeFile.exists()) {
                    BarcodeFile.delete();
                }
                BarcodeFile.createNewFile();

                if (BarcodeFile.exists()) {
                    OutputStream fo = new FileOutputStream(BarcodeFile);
                    fo.write(data.getBytes());
                    fo.close();
                    FilePath = BarcodeFile.getPath();
                }

            } catch (Exception ex) {
                Log.d("HATA", "HATADOSYA");
                WritableMap map = new WritableNativeMap();
                map.putString("HasError", "True");
                map.putString("ErrMsg",
                        "Barkod dosyası oluşturulurken bir hata ile karşılaşıldı. Hata : " + ex.getMessage());
                sendEvent("BarcodePrinter", map);
            }

            printer = new BarcodePrinter<TCPConnection, PPLZ>();
            printer.setConnection(new TCPConnection(Ip, PortNumber));
            printer.setEmulation(new PPLZ());
            printer.getConnection().setStateListener(stateListener);
            printer.getConnection().open();
        } catch (Exception ex) {
            Log.d("HATA", "HATA CONNECT");
            WritableMap map = new WritableNativeMap();
            map.putString("HasError", "True");
            map.putString("ErrMsg", "Barkod yazdırılırken bir hata ile karşılaşıldı. Hata : " + ex.getMessage());
            sendEvent("BarcodePrinter", map);
        }

    }

    public IConnectionStateListener stateListener = new IConnectionStateListener() {

        public void onStateChanged(final ConnectionState state) {
            UiThreadUtil.runOnUiThread(new Runnable() {

                public void run() {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                    
                    if (state == ConnectionState.Connected) {
                        LastState=state;
                        printData();

                    } else if (LastState == ConnectionState.Connecting && state == ConnectionState.Disconnected) {
                        Log.d("HATA", "BAĞLANTI");
                        WritableMap map = new WritableNativeMap();
                        map.putString("HasError", "True");
                        map.putString("ErrMsg", "Yazıcı ile bağlantı sağlanamadı");
                        sendEvent("BarcodePrinter", map);
                    }
                }
            });

        }
    };

    public void printData() {
        try {
            Log.d("OK", "SENDING");
            printer.sendFile(FilePath);
            printer.getConnection().close();

            WritableMap map = new WritableNativeMap();
            map.putString("HasError", "False");
            map.putString("ErrMsg", "");
            sendEvent("BarcodePrinter", map);
        } catch (Exception ex) {
            Log.d("HATA", "HATAPRINT");
            WritableMap map = new WritableNativeMap();
            map.putString("HasError", "True");
            map.putString("ErrMsg", "Barkod yazdırılırken bir hata ile karşılaşıldı. Hata : " + ex.getMessage());
            sendEvent("BarcodePrinter", map);
        }
    }

    private void sendEvent(String eventName, WritableMap map) {
        try {

            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, map);
        } catch (Exception e) {
            Log.d("ReactNativeJS", "Exception in sendEvent in ReferrerBroadcastReceiver is:" + e.toString());
        }

    }

}