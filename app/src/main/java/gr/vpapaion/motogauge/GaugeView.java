package gr.vpapaion.motogauge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

public class GaugeView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float speed, maxSpeed, lean, maxLeft, maxRight, accuracy = -1;
    private String gpsStatus = "GPS SEARCHING";
    private boolean sensorAvailable = true;
    private RectF calibrateButton = new RectF();
    private Runnable calibrateListener;

    public GaugeView(Context context) { super(context); p.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)); setBackgroundColor(Color.rgb(5,8,13)); }
    public void setOnCalibrateListener(Runnable listener) { calibrateListener = listener; }
    public void setSensorAvailable(boolean available) { sensorAvailable = available; invalidate(); }
    public void setGpsStatus(String status) { gpsStatus = status; invalidate(); }
    public void setSpeed(float value, float acc) { speed = value < 1f ? 0f : value; maxSpeed = Math.max(maxSpeed, speed); accuracy = acc; gpsStatus = "GPS LOCK"; invalidate(); }
    public void setLean(float value) { lean = Math.max(-90f, Math.min(90f, value)); if (lean < 0) maxLeft = Math.max(maxLeft, -lean); else maxRight = Math.max(maxRight, lean); invalidate(); }
    public void resetLean() { maxLeft = maxRight = 0; invalidate(); }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w=getWidth(), h=getHeight();
        if (h >= w) { drawPortrait(c,w,h); return; }
        float u=Math.min(w/960f,h/480f);
        p.setShader(new LinearGradient(0,0,0,h,Color.rgb(11,18,28),Color.rgb(3,6,10),Shader.TileMode.CLAMP)); c.drawRect(0,0,w,h,p); p.setShader(null);
        drawPanel(c,new RectF(22*u,24*u,280*u,h-24*u),Color.rgb(15,25,39));
        drawPanel(c,new RectF(300*u,24*u,660*u,h-24*u),Color.rgb(10,17,27));
        drawPanel(c,new RectF(680*u,24*u,938*u,h-24*u),Color.rgb(15,25,39));
        drawLean(c,151*u,h/2,-lean,maxLeft,true,u);
        drawSpeed(c,480*u,h/2,u);
        drawLean(c,809*u,h/2,lean,maxRight,false,u);
        drawTop(c,w,h,u);
    }

    private void drawPortrait(Canvas c,float w,float h) {
        float u=Math.min(w/480f,h/960f);
        p.setShader(new LinearGradient(0,0,0,h,Color.rgb(11,18,28),Color.rgb(3,6,10),Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,p); p.setShader(null);

        drawPanel(c,new RectF(18*u,22*u,w-18*u,470*u),Color.rgb(10,17,27));
        text(c,"GPS SPEED",w/2,70*u,18*u,Color.rgb(120,150,172),Paint.Align.CENTER,true);
        text(c,String.valueOf(Math.round(speed)),w/2,300*u,176*u,Color.WHITE,Paint.Align.CENTER,true);
        text(c,"km/h",w/2,350*u,25*u,Color.rgb(87,230,255),Paint.Align.CENTER,true);
        p.setColor(Color.rgb(29,49,68)); c.drawRoundRect(new RectF(105*u,385*u,375*u,442*u),18*u,18*u,p);
        text(c,"MAX  "+Math.round(maxSpeed)+" km/h",w/2,422*u,20*u,Color.rgb(188,207,219),Paint.Align.CENTER,true);

        float gap=10*u, top=490*u, bottom=785*u;
        RectF leftPanel=new RectF(18*u,top,240*u-gap/2,bottom);
        RectF rightPanel=new RectF(240*u+gap/2,top,w-18*u,bottom);
        drawPanel(c,leftPanel,Color.rgb(15,25,39)); drawPanel(c,rightPanel,Color.rgb(15,25,39));
        drawPortraitLean(c,leftPanel.centerX(),-lean,maxLeft,true,u);
        drawPortraitLean(c,rightPanel.centerX(),lean,maxRight,false,u);

        calibrateButton.set(90*u,810*u,w-90*u,875*u);
        p.setColor(Color.rgb(35,49,62)); c.drawRoundRect(calibrateButton,18*u,18*u,p);
        text(c,"CALIBRATE UPRIGHT",w/2,852*u,17*u,Color.WHITE,Paint.Align.CENTER,true);
        int dot=gpsStatus.equals("GPS LOCK")?Color.rgb(83,224,148):Color.rgb(255,179,71);
        p.setColor(dot); c.drawCircle(112*u,907*u,5*u,p);
        String detail=gpsStatus+(accuracy>=0?String.format(Locale.US,"  ±%.0fm",accuracy):"");
        text(c,detail,124*u,913*u,13*u,Color.rgb(145,167,184),Paint.Align.LEFT,true);
        if(!sensorAvailable) text(c,"NO ROTATION SENSOR",w/2,940*u,13*u,Color.rgb(255,110,100),Paint.Align.CENTER,true);
    }

    private void drawPortraitLean(Canvas c,float x,float sideLean,float maximum,boolean left,float u) {
        int accent=left?Color.rgb(255,179,71):Color.rgb(87,230,255);
        text(c,left?"LEFT":"RIGHT",x,535*u,17*u,Color.rgb(120,150,172),Paint.Align.CENTER,true);
        float shown=Math.max(0,sideLean);
        text(c,String.format(Locale.US,"%.0f°",shown),x,655*u,66*u,shown>0.5f?accent:Color.rgb(91,110,126),Paint.Align.CENTER,true);
        Path tri=new Path(); float dir=left?-1:1;
        tri.moveTo(x+dir*44*u,685*u); tri.lineTo(x+dir*78*u,685*u); tri.lineTo(x+dir*78*u,651*u); tri.close();
        p.setColor(shown>0.5f?accent:Color.rgb(50,67,82)); c.drawPath(tri,p);
        text(c,"MAX  "+Math.round(maximum)+"°",x,746*u,18*u,Color.rgb(188,207,219),Paint.Align.CENTER,true);
    }

    private void drawPanel(Canvas c, RectF r, int color) { p.setColor(color); p.setStyle(Paint.Style.FILL); c.drawRoundRect(r,24,24,p); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f); p.setColor(Color.rgb(35,56,78)); c.drawRoundRect(r,24,24,p); p.setStyle(Paint.Style.FILL); }
    private void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align,boolean bold) { p.setShader(null); p.setStyle(Paint.Style.FILL); p.setTextAlign(align); p.setTextSize(size); p.setColor(color); p.setTypeface(android.graphics.Typeface.create("sans",bold?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL)); c.drawText(s,x,y,p); }

    private void drawSpeed(Canvas c,float x,float cy,float u) {
        text(c,"GPS SPEED",x,78*u,18*u,Color.rgb(120,150,172),Paint.Align.CENTER,true);
        text(c,String.valueOf(Math.round(speed)),x,285*u,172*u,Color.WHITE,Paint.Align.CENTER,true);
        text(c,"km/h",x,330*u,25*u,Color.rgb(87,230,255),Paint.Align.CENTER,true);
        p.setColor(Color.rgb(29,49,68)); c.drawRoundRect(new RectF(350*u,368*u,610*u,425*u),18*u,18*u,p);
        text(c,"MAX  "+Math.round(maxSpeed)+" km/h",480*u,405*u,20*u,Color.rgb(188,207,219),Paint.Align.CENTER,true);
    }

    private void drawLean(Canvas c,float x,float cy,float sideLean,float maximum,boolean left,float u) {
        int accent = left ? Color.rgb(255,179,71) : Color.rgb(87,230,255);
        text(c,left?"LEFT LEAN":"RIGHT LEAN",x,78*u,18*u,Color.rgb(120,150,172),Paint.Align.CENTER,true);
        float shown=Math.max(0,sideLean);
        text(c,String.format(Locale.US,"%.0f°",shown),x,238*u,78*u,shown>0.5f?accent:Color.rgb(91,110,126),Paint.Align.CENTER,true);
        Path tri=new Path(); float dir=left?-1:1; tri.moveTo(x+dir*72*u,265*u); tri.lineTo(x+dir*118*u,265*u); tri.lineTo(x+dir*118*u,219*u); tri.close(); p.setColor(shown>0.5f?accent:Color.rgb(50,67,82)); c.drawPath(tri,p);
        text(c,"MAX  "+Math.round(maximum)+"°",x,348*u,20*u,Color.rgb(188,207,219),Paint.Align.CENTER,true);
        calibrateButton.set((x-89*u),378*u,(x+89*u),428*u);
        if(left){ p.setColor(Color.rgb(35,49,62)); c.drawRoundRect(calibrateButton,16*u,16*u,p); text(c,"CALIBRATE",x,411*u,17*u,Color.WHITE,Paint.Align.CENTER,true); }
    }

    private void drawTop(Canvas c,float w,float h,float u) {
        int green=Color.rgb(83,224,148); p.setColor(gpsStatus.equals("GPS LOCK")?green:Color.rgb(255,179,71)); c.drawCircle(321*u,52*u,5*u,p);
        String detail=gpsStatus+(accuracy>=0?String.format(Locale.US,"  ±%.0fm",accuracy):""); text(c,detail,332*u,58*u,13*u,Color.rgb(145,167,184),Paint.Align.LEFT,true);
        if(!sensorAvailable) text(c,"NO ROTATION SENSOR",809*u,h-35*u,13*u,Color.rgb(255,110,100),Paint.Align.CENTER,true);
        text(c,"Tap CALIBRATE while the bike is upright",w/2,h-10*u,11*u,Color.rgb(86,105,121),Paint.Align.CENTER,false);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getAction()==MotionEvent.ACTION_UP && calibrateButton.contains(e.getX(),e.getY())) { if(calibrateListener!=null) calibrateListener.run(); performClick(); return true; }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
