package com.incomingcall;
import android.app.KeyguardManager;
import android.os.Build;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.net.Uri;
import android.os.Vibrator;
import android.content.Context;
import android.media.MediaPlayer;
import android.provider.Settings;
import java.util.List;
import android.app.Activity;
import android.view.KeyEvent;
import androidx.appcompat.app.AppCompatActivity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.squareup.picasso.Picasso;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.Ringtone;



public class UnlockScreenActivity extends AppCompatActivity implements UnlockScreenActivityInterface {

    private static final String TAG = "MessagingService";
    private TextView tvName;
    private TextView tvInfo;
    private TextView tvDecline;
    private TextView tvAccept;
    private ImageView ivAvatar;
    private String uuid = "";
    static boolean active = false;
    private static Vibrator v = (Vibrator) IncomingCallModule.reactContext.getSystemService(Context.VIBRATOR_SERVICE);
    private long[] pattern = {0, 1000, 800};
    private static Ringtone ringtone = RingtoneManager.getRingtone(IncomingCallModule.reactContext, Settings.System.DEFAULT_RINGTONE_URI);

    private static MediaPlayer player = MediaPlayer.create(IncomingCallModule.reactContext, Settings.System.DEFAULT_RINGTONE_URI);
    private static Activity fa;


    private boolean checkIfCalling(Context context){

           AudioManager manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            if(manager.getMode()==AudioManager.MODE_IN_CALL ){
                return true;
            }
            else{
                return false;
            }
    }

    private boolean checkIfSilent(Context context){
        AudioManager manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
          if(manager.getRingerMode()==AudioManager.RINGER_MODE_SILENT){
            return true;
          }
          else{
            return false;
          }
    }

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fa = this;

        IncomingCallModule.setUnlockScreenActivityInstance(this);

        setContentView(R.layout.activity_call_incoming);

        tvName = findViewById(R.id.tvName);
        tvInfo = findViewById(R.id.tvInfo);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvDecline = findViewById(R.id.tvDecline);
        tvAccept = findViewById(R.id.tvAccept);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.containsKey("uuid")) {
                uuid = bundle.getString("uuid");
            }
            if (bundle.containsKey("name")) {
                String name = bundle.getString("name");
                tvName.setText(name);
            }
            if (bundle.containsKey("info")) {
                String info = bundle.getString("info");
                tvInfo.setText(info);
            }
            if (bundle.containsKey("avatar")) {
                String avatar = bundle.getString("avatar");
                if (avatar != null) {
                    Picasso.get().load(avatar).transform(new CircleTransform()).into(ivAvatar);
                }
            }
            if (bundle.containsKey("decline")) {
                String decline = bundle.getString("decline");
                tvDecline.setText(decline);
            }
            if (bundle.containsKey("accept")) {
                String accept = bundle.getString("accept");
                tvAccept.setText(accept);
            }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);


        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        try{
            audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager.getStreamVolume(AudioManager.STREAM_RING), 0);
            ringtone.setStreamType(AudioManager.STREAM_RING);
            ringtone.play();   
        }catch(Exception e){
            e.printStackTrace();
        }
        
        v.vibrate(pattern, 0);
    
        AnimateImage acceptCallBtn = findViewById(R.id.ivAcceptCall);
        acceptCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    v.cancel();
                    ringtone.stop();
                    acceptDialing();
                } catch (Exception e) {
                    WritableMap params = Arguments.createMap();
                    params.putString("message", e.getMessage());
                    sendEvent("error", params);
                    dismissDialing();
                }
            }
        });

        AnimateImage rejectCallBtn = findViewById(R.id.ivDeclineCall);
        rejectCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.cancel();
                ringtone.stop();
                dismissDialing();
            }
        });

        if(checkIfCalling(IncomingCallModule.reactContext)){
            ringtone.stop();
            v.cancel();
        }
        if(checkIfSilent(IncomingCallModule.reactContext)){
            v.cancel();
        }
    
        processQueuedUpdates();
    
    }

     public void processQueuedUpdates() {
        Log.d(TAG, "processQueuedUpdates display: ");
        while (!IncomingCallModule.updateQueue.isEmpty()) {
            final UpdateRequest request = IncomingCallModule.updateQueue.poll();

            // run on UI thread
            fa.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "processQueuedUpdates: updating display with name: " + request.getName());
                    String name = request.getName();
                    String uuid = request.getUuid();
                    updateDisplay(uuid,name);
                }
            });

        }
    }


    @Override
    public void onBackPressed() {
        // Dont back
    }

    public static void dismissIncoming() {
        v.cancel();
        ringtone.stop();
        fa.finish();
    }

    private void acceptDialing() {
        WritableMap params = Arguments.createMap();
        params.putBoolean("accept", true);
        params.putString("uuid", uuid);
        if (!IncomingCallModule.reactContext.hasCurrentActivity()) {
            params.putBoolean("isHeadless", true);
        }
    KeyguardManager mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        if (mKeyguardManager.isDeviceLocked()) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mKeyguardManager.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
              @Override
              public void onDismissSucceeded() {
                super.onDismissSucceeded();
              }
            });
          }
        }

        sendEvent("answerCall", params);

        finish();
    }

    private void dismissDialing() {
        WritableMap params = Arguments.createMap();
        params.putBoolean("accept", false);
        params.putString("uuid", uuid);
        if (!IncomingCallModule.reactContext.hasCurrentActivity()) {
            params.putBoolean("isHeadless", true);
        }

        sendEvent("endCall", params);

        finish();
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected: ");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected: ");

    }

    @Override
    public void onConnectFailure() {
        Log.d(TAG, "onConnectFailure: ");

    }

    @Override
    public void onIncoming(ReadableMap params) {
        Log.d(TAG, "onIncoming: ");
    }

    private void sendEvent(String eventName, WritableMap params) {
        IncomingCallModule.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (action == KeyEvent.ACTION_UP) {
                        v.cancel();
                        ringtone.stop();
                    }
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (action == KeyEvent.ACTION_UP) {
                        v.cancel();
                        ringtone.stop();
                    }
                    return true;
            default:
                return super.dispatchKeyEvent(event);
            }
        }

    public void updateDisplay(String callUUID,String name) {
        //log the incoming call
        Log.d(TAG, "update display from activity: " + name + callUUID);

        if(!uuid.equals(callUUID)){
            return;
        }
        tvName.setText(name);
    
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        IncomingCallModule.clearUnlockScreenActivityInstance();
    }

    

}
