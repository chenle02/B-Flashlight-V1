package com.chenboda01.bflashlightv1;

import android.Manifest;
import android.app.Activity;
import android.os.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;

public class MainActivity extends Activity {
    CameraManager cm;
    String cameraId;
    boolean isOn=false, screenMode=false;
    int maxStrength=1;
    SeekBar slider;
    TextView title, mult, status;
    LinearLayout root;

    public void onCreate(Bundle b){
        super.onCreate(b);
        cm=(CameraManager)getSystemService(CAMERA_SERVICE);
        findCamera();
        build();
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA},1);
        }
    }

    void findCamera(){
        try{
            for(String id: cm.getCameraIdList()){
                CameraCharacteristics c=cm.getCameraCharacteristics(id);
                Boolean flash=c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer face=c.get(CameraCharacteristics.LENS_FACING);
                if(flash!=null && flash && face!=null && face==CameraCharacteristics.LENS_FACING_BACK){ cameraId=id; break; }
            }
            if(Build.VERSION.SDK_INT>=33 && cameraId!=null){
                Integer m=cm.getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
                if(m!=null) maxStrength=Math.max(1,m);
            }
        }catch(Exception e){}
    }

    void build(){
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(20),dp(20),dp(20),dp(20));
        root.setBackgroundColor(Color.rgb(7,19,30));
        title=t("B-Flashlight V1",30,Color.WHITE,true);
        mult=t("1.0x",60,Color.rgb(80,255,157),true);
        status=t("0.1x to 10x slider. 10x means phone hardware maximum.",15,Color.LTGRAY,false);
        slider=new SeekBar(this);
        slider.setMax(99);
        slider.setProgress(9);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean f){ updateText(); if(isOn) apply(); }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
        LinearLayout row1=row();
        row1.addView(btn("Power", v->{isOn=!isOn; apply();}));
        row1.addView(btn("Screen Mode", v->{screenMode=!screenMode; apply();}));
        LinearLayout row2=row();
        row2.addView(btn("0.1x", v->{slider.setProgress(0);}));
        row2.addView(btn("1x", v->{slider.setProgress(9);}));
        row2.addView(btn("10x", v->{slider.setProgress(99);}));
        root.addView(title);
        root.addView(mult);
        root.addView(status);
        root.addView(slider,new LinearLayout.LayoutParams(-1,dp(70)));
        root.addView(row1);
        root.addView(row2);
        root.addView(t("Real torch brightness depends on Android version and phone hardware. Screen Mode gives smoother dim light.",14,Color.LTGRAY,false));
        setContentView(root);
        updateText();
    }

    TextView t(String s,int size,int color,boolean bold){
        TextView v=new TextView(this);
        v.setText(s); v.setTextSize(size); v.setTextColor(color); v.setGravity(Gravity.CENTER); v.setPadding(4,8,4,8);
        if(bold) v.setTypeface(null,1);
        return v;
    }
    Button btn(String s,View.OnClickListener l){
        Button b=new Button(this);
        b.setText(s); b.setAllCaps(false); b.setOnClickListener(l);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(60),1); p.setMargins(6,6,6,6); b.setLayoutParams(p);
        return b;
    }
    LinearLayout row(){ LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); return r; }
    int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    float factor(){ return (slider.getProgress()+1)/10f; }
    void updateText(){ mult.setText(String.format(Locale.US,"%.1fx",factor())); }

    void apply(){
        resetColors();
        if(!isOn){ setTorch(false); setScreen(-1f); status.setText("Off"); return; }
        float f=factor();
        if(screenMode || f<1f){
            if(f>=1f) setTorch(true); else setTorch(false);
            float b=Math.max(.01f,Math.min(1f,f/10f));
            setScreen(b);
            int w=(int)(25+230*b);
            root.setBackgroundColor(Color.rgb(w,w,w));
            title.setTextColor(Color.BLACK);
            status.setTextColor(Color.DKGRAY);
            mult.setTextColor(Color.rgb(0,110,60));
            status.setText("Screen light / dim mode.");
        }else{
            setScreen(-1f);
            setTorch(true);
            status.setText(Build.VERSION.SDK_INT>=33 ? "Using torch strength if supported." : "Normal flashlight brightness on this phone.");
        }
    }
    void resetColors(){ root.setBackgroundColor(Color.rgb(7,19,30)); title.setTextColor(Color.WHITE); status.setTextColor(Color.LTGRAY); mult.setTextColor(Color.rgb(80,255,157)); }
    void setTorch(boolean enable){
        if(cameraId==null){ status.setText("No flashlight found."); return; }
        try{
            if(enable && Build.VERSION.SDK_INT>=33){
                int lvl=Math.max(1,Math.min(maxStrength,Math.round((factor()/10f)*maxStrength)));
                cm.turnOnTorchWithStrengthLevel(cameraId,lvl);
            }else cm.setTorchMode(cameraId,enable);
        }catch(Exception e){ try{ cm.setTorchMode(cameraId,enable); }catch(Exception ex){ status.setText("Flashlight permission/error."); } }
    }
    void setScreen(float b){ WindowManager.LayoutParams lp=getWindow().getAttributes(); lp.screenBrightness=b; getWindow().setAttributes(lp); }
    protected void onPause(){ super.onPause(); try{ setTorch(false); }catch(Exception e){} }
}
