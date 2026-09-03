package gr.vpapaion.motogauge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import java.util.Locale;

public class SpeedView extends View {
    private static final int WHITE=Color.rgb(245,248,252);
    private static final int GREEN=Color.rgb(83,224,148);
    private static final int YELLOW=Color.rgb(255,211,78);
    private static final int ORANGE=Color.rgb(255,138,61);
    private static final int RED=Color.rgb(255,77,77);
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private float speed, maxSpeed, accuracy=-1f;
    private String gpsStatus="GPS SEARCHING";
    private boolean timerArmed=true, timerRunning=false, timerComplete=false;
    private long timerStartMs;
    private double timerResultSeconds;

    public SpeedView(Context context) {
        super(context);
        p.setTypeface(android.graphics.Typeface.create("sans",android.graphics.Typeface.NORMAL));
        setBackgroundColor(Color.rgb(3,6,10));
    }

    public void setGpsStatus(String status) { gpsStatus=status; invalidate(); }

    public void setSpeed(float value,float acc) {
        float previous=speed;
        speed=value;
        accuracy=acc;
        maxSpeed=Math.max(maxSpeed,speed);
        gpsStatus="GPS LOCK";
        long now=SystemClock.elapsedRealtime();

        if (speed==0f) {
            if (timerRunning || previous>0f) {
                timerRunning=false;
                timerComplete=false;
                timerResultSeconds=0d;
            }
            timerArmed=true;
        } else if (timerArmed && !timerRunning && !timerComplete) {
            timerStartMs=now;
            timerRunning=true;
            timerArmed=false;
        }

        if (timerRunning && speed>=100f) {
            timerResultSeconds=(now-timerStartMs)/1000d;
            timerRunning=false;
            timerComplete=true;
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w=getWidth(),h=getHeight();
        p.setShader(new LinearGradient(0,0,0,h,Color.rgb(11,18,28),Color.rgb(3,6,10),Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,p); p.setShader(null);
        if (w>=h) drawLandscape(c,w,h); else drawPortrait(c,w,h);
        if (timerRunning || speed>=130f) postInvalidateDelayed(80L);
    }

    private void drawPortrait(Canvas c,float w,float h) {
        float u=Math.min(w/480f,h/960f);
        text(c,"GPS SPEED",w/2,90*u,20*u,Color.rgb(120,150,172),Paint.Align.CENTER,true);
        int color=speedColor();
        text(c,String.valueOf(Math.round(speed)),w/2,465*u,255*u,color,Paint.Align.CENTER,true);
        text(c,"km/h",w/2,535*u,34*u,color,Paint.Align.CENTER,true);
        drawBandLegend(c,35*u,585*u,w-35*u,u);
        drawTimer(c,new RectF(28*u,660*u,w-28*u,845*u),u);
        drawStatus(c,w,h,u);
    }

    private void drawLandscape(Canvas c,float w,float h) {
        float u=Math.min(w/960f,h/480f);
        text(c,"GPS SPEED",325*u,67*u,18*u,Color.rgb(120,150,172),Paint.Align.CENTER,true);
        int color=speedColor();
        text(c,String.valueOf(Math.round(speed)),325*u,330*u,205*u,color,Paint.Align.CENTER,true);
        text(c,"km/h",325*u,395*u,30*u,color,Paint.Align.CENTER,true);
        drawTimer(c,new RectF(585*u,70*u,925*u,345*u),u);
        drawBandLegend(c,585*u,380*u,925*u,u);
        drawStatus(c,w,h,u);
    }

    private int speedColor() {
        if (speed>=130f) return ((SystemClock.elapsedRealtime()/350L)%2L==0L)?RED:Color.rgb(65,10,10);
        if (speed>=120f) return RED;
        if (speed>=110f) return ORANGE;
        if (speed>=100f) return YELLOW;
        if (speed>80f) return GREEN;
        return WHITE;
    }

    private void drawBandLegend(Canvas c,float left,float y,float right,float u) {
        float total=right-left,gap=5*u,seg=(total-gap*4)/5f;
        int[] colors={WHITE,GREEN,YELLOW,ORANGE,RED};
        String[] labels={"0–80","81–99","100–109","110–119","120+"};
        for(int i=0;i<5;i++) {
            float x=left+i*(seg+gap); p.setColor(colors[i]);
            c.drawRoundRect(new RectF(x,y,x+seg,y+8*u),4*u,4*u,p);
            text(c,labels[i],x+seg/2,y+30*u,9*u,Color.rgb(116,137,153),Paint.Align.CENTER,true);
        }
    }

    private void drawTimer(Canvas c,RectF r,float u) {
        p.setColor(Color.rgb(14,24,37)); c.drawRoundRect(r,24*u,24*u,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f*u); p.setColor(Color.rgb(35,56,78)); c.drawRoundRect(r,24*u,24*u,p); p.setStyle(Paint.Style.FILL);
        text(c,"0–100 km/h",r.centerX(),r.top+43*u,17*u,Color.rgb(120,150,172),Paint.Align.CENTER,true);
        double seconds=timerRunning?(SystemClock.elapsedRealtime()-timerStartMs)/1000d:timerResultSeconds;
        String value=String.format(Locale.US,"%.2f",seconds);
        text(c,value,r.centerX(),r.centerY()+36*u,73*u,timerComplete?GREEN:WHITE,Paint.Align.CENTER,true);
        text(c,"SECONDS",r.centerX(),r.bottom-25*u,13*u,Color.rgb(120,150,172),Paint.Align.CENTER,true);
        String state=timerComplete?"COMPLETE":timerRunning?"RUNNING":"READY — STOPPED";
        text(c,state,r.centerX(),r.bottom+32*u,12*u,timerComplete?GREEN:Color.rgb(145,167,184),Paint.Align.CENTER,true);
    }

    private void drawStatus(Canvas c,float w,float h,float u) {
        int dot=gpsStatus.equals("GPS LOCK")?GREEN:ORANGE;
        p.setColor(dot); c.drawCircle(36*u,h-33*u,5*u,p);
        String detail=gpsStatus+(accuracy>=0?String.format(Locale.US,"  ±%.0fm",accuracy):"");
        text(c,detail,49*u,h-28*u,12*u,Color.rgb(145,167,184),Paint.Align.LEFT,true);
        text(c,"MAX "+Math.round(maxSpeed)+" km/h",w-28*u,h-28*u,12*u,Color.rgb(145,167,184),Paint.Align.RIGHT,true);
    }

    private void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align,boolean bold) {
        p.setShader(null); p.setStyle(Paint.Style.FILL); p.setTextAlign(align); p.setTextSize(size); p.setColor(color);
        p.setTypeface(android.graphics.Typeface.create("sans",bold?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL));
        c.drawText(s,x,y,p);
    }
}
