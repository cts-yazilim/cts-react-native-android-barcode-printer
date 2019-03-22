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
import com.argox.sdk.barcodeprinter.BarcodePrinter;
import com.argox.sdk.barcodeprinter.connection.ConnectionState;
import com.argox.sdk.barcodeprinter.connection.IConnectionStateListener;
import com.argox.sdk.barcodeprinter.connection.tcp.TCPConnection;
import com.argox.sdk.barcodeprinter.emulation.pplz.PPLZ;

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
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class RNAndroidBarcodePrinterModule extends ReactContextBaseJavaModule {

    public static ReactApplicationContext reactContext;
    public BarcodePrinter<TCPConnection, PPLZ> printer;
    public String Ip = "192.168.1.42";
    public int Port = 9100;

    public RNAndroidBarcodePrinterModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNAndroidBarcodePrinter";
    }

   

    @ReactMethod
    public void PrintBarcode(String data) { 
        printer = new BarcodePrinter<TCPConnection, PPLZ>();
        printer.setConnection(new TCPConnection(Ip, Port));
        printer.setEmulation(new PPLZ());
        printer.getConnection().setStateListener(stateListener);
        printer.getConnection().open();

    }

    public  IConnectionStateListener stateListener = new IConnectionStateListener() {

        public void onStateChanged(final ConnectionState state) {
            reactContext.runOnUiThread(new Runnable() {

                public void run() {
                    if (state == ConnectionState.Connected) {
                        printData();
                       
                    } else {
                     Log.d("STATUS",state.name());
                    }
                }
            });
            
        }
    };

    public  void printData()
    {
    	try {
    		String path = new File(Environment.getExternalStorageDirectory(), "argox_printer.txt").getPath();
			printer.sendFile(path);
			printer.getConnection().close();
    	 } catch (Exception ex) {
        	 Toast.makeText( reactContext,  ex.getMessage(), Toast.LENGTH_LONG).show();
        }
   	  
    }


}