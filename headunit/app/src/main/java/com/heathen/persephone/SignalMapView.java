package com.heathen.persephone;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;

// Programmatic-only view: its injected tile cache and selection callback cannot be inflated from XML.
@android.annotation.SuppressLint("ViewConstructor")
final class SignalMapView extends View {
    interface Selection {void select(String key);}
    private final TileCache tiles; private final Selection selection;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG),tilePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GestureDetector drag; private final ScaleGestureDetector pinch;
    private List<SignalMapModel.Point> points=Collections.emptyList();
    private boolean gameStyle;private android.location.Location receiver;
    void gameStyle(boolean enabled){gameStyle=enabled;invalidate();}
    void receiver(android.location.Location fix){if(fix==null){receiver=null;invalidate();return;}long age=android.os.SystemClock.elapsedRealtime()-fix.getElapsedRealtimeNanos()/1000000;if(!OverlayProjection.validGeo(fix.getLatitude(),fix.getLongitude())||fix.isFromMockProvider()||age<0||age>10000||!fix.hasAccuracy()||fix.getAccuracy()<=0||fix.getAccuracy()>100)return;receiver=new android.location.Location(fix);if(follow){lat=fix.getLatitude();lon=fix.getLongitude();centered=true;}invalidate();}
    private String target=""; private SignalMapModel.Estimate estimate;
    private double lat,lon; private int zoom=17; private boolean centered,follow=true;
    private final float density;
    private final RectF tileRect=new RectF();
    private final DashPathEffect dashed;
    SignalMapView(Context context,TileCache tiles,Selection selection) {
        super(context);this.tiles=tiles;this.selection=selection;density=getResources().getDisplayMetrics().density; dashed=new DashPathEffect(new float[]{8*density,5*density},0);
        setContentDescription("Interactive signal map. Drag to pan, pinch or use zoom buttons. Points are receiver observations; dashed area is a model estimate.");
        ColorMatrix matrix=new ColorMatrix(new float[]{.16f,.16f,.16f,0,0,.18f,.18f,.18f,0,8,.22f,.22f,.22f,0,14,0,0,0,1,0});tilePaint.setColorFilter(new ColorMatrixColorFilter(matrix));
        drag=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            public boolean onDown(MotionEvent e){return true;}
            public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy){double[] c=world(lat,lon);fromWorld(c[0]+dx/density,c[1]+dy/density);follow=false;invalidate();return true;}
            public boolean onSingleTapUp(MotionEvent e){SignalMapModel.Point best=null;double distance=30*density;for(SignalMapModel.Point p:points){float[] xy=screen(p.lat,p.lon);double d=Math.hypot(xy[0]-e.getX(),xy[1]-e.getY());if(d<distance){distance=d;best=p;}}if(best!=null)selection.select(best.key);return true;}
        });
        pinch=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){float factor=1;public boolean onScaleBegin(ScaleGestureDetector detector){factor=1;return true;}public boolean onScale(ScaleGestureDetector d){factor*=d.getScaleFactor();if(factor>1.35){zoom(1);factor=1;}else if(factor<.74){zoom(-1);factor=1;}return true;}});
    }
    void setData(List<SignalMapModel.Point> source,String selected){points=new ArrayList<>(source);if(points.isEmpty()&&receiver==null)centered=false;target=selected;estimate=SignalMapModel.estimate(points,target);if(receiver==null&&!points.isEmpty()&&(!centered||follow)){SignalMapModel.Point p=points.get(points.size()-1);lat=p.lat;lon=p.lon;centered=true;}invalidate();}
    SignalMapModel.Estimate estimate(){return estimate;}
    void zoom(int delta){zoom=Math.max(3,Math.min(19,zoom+delta));invalidate();}
    void center(){follow=true;if(receiver!=null){lat=receiver.getLatitude();lon=receiver.getLongitude();centered=true;invalidate();return;}if(!points.isEmpty()){SignalMapModel.Point p=points.get(points.size()-1);lat=p.lat;lon=p.lon;centered=true;}invalidate();}
    @Override public boolean onTouchEvent(MotionEvent e){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(e.getAction()!=MotionEvent.ACTION_UP&&e.getAction()!=MotionEvent.ACTION_CANCEL);pinch.onTouchEvent(e);boolean handled=drag.onTouchEvent(e);if(handled&&e.getAction()==MotionEvent.ACTION_UP)performClick();return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    private double[] world(double latitude,double longitude){double size=256*(1<<zoom),phi=Math.toRadians(Math.max(-85.0511,Math.min(85.0511,latitude)));return new double[]{(longitude+180)/360*size,(1-Math.log(Math.tan(phi)+1/Math.cos(phi))/Math.PI)/2*size};}
    private void fromWorld(double x,double y){double size=256*(1<<zoom);lon=(x/size*360-180+540)%360-180;lat=Math.toDegrees(Math.atan(Math.sinh(Math.PI*(1-2*y/size))));lat=Math.max(-85.0511,Math.min(85.0511,lat));}
    private float[] screen(double latitude,double longitude){double[] p=world(latitude,longitude),c=world(lat,lon);double dx=p[0]-c[0],size=256*(1<<zoom);if(dx>size/2)dx-=size;if(dx< -size/2)dx+=size;return new float[]{(float)(getWidth()/2+dx*density),(float)(getHeight()/2+(p[1]-c[1])*density)};}
    private void stroke(int color,float width){paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width*density);paint.setPathEffect(null);}
    private void label(Canvas canvas,String text,float x,float y,int color,float size){paint.setColor(color);paint.setStyle(Paint.Style.FILL);paint.setTypeface(Typeface.MONOSPACE);paint.setTextSize(size*density);paint.setPathEffect(null);canvas.drawText(text,x,y,paint);}
    static int color(String tag){if(tag.contains("RAVEN"))return Color.rgb(222,105,255);if(tag.contains("FALCON")||tag.contains("LPR"))return Color.rgb(255,222,67);if(tag.contains("FLOCK"))return Color.rgb(65,244,231);return Color.rgb(116,159,186);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.drawColor(Color.rgb(5,13,20));stroke(Color.rgb(20,46,58),.5f);
        for(float x=0;x<getWidth();x+=32*density)canvas.drawLine(x,0,x,getHeight(),paint);for(float y=0;y<getHeight();y+=32*density)canvas.drawLine(0,y,getWidth(),y,paint);
        if(centered){double[] center=world(lat,lon);double left=center[0]-getWidth()/density/2,top=center[1]-getHeight()/density/2;
            int startX=(int)Math.floor(left/256),startY=(int)Math.floor(top/256),endX=(int)Math.floor((left+getWidth()/density)/256),endY=(int)Math.floor((top+getHeight()/density)/256);
            for(int x=startX;x<=endX;x++)for(int y=startY;y<=endY;y++){Bitmap tile=tiles.get(zoom,x,y,this::invalidate);if(tile!=null){tileRect.set((float)((x*256-left)*density),(float)((y*256-top)*density),(float)(((x+1)*256-left)*density),(float)(((y+1)*256-top)*density));canvas.drawBitmap(tile,null,tileRect,tilePaint);}}
        }
        // The receiver route is drawn only between time-near samples: no invented jumps.
        SignalMapModel.Point previous=null;stroke(Color.rgb(42,117,119),1.5f);
        for(SignalMapModel.Point p:points){if(previous!=null&&Math.abs(p.time-previous.time)<30000&&SignalMapModel.meters(p.lat,p.lon,previous.lat,previous.lon)<500){float[] a=screen(previous.lat,previous.lon),b=screen(p.lat,p.lon);canvas.drawLine(a[0],a[1],b[0],b[1],paint);}previous=p;}
        SignalMapModel.Point strongest=null;int begin=Math.max(0,points.size()-600);
        for(int i=begin;i<points.size();i++){SignalMapModel.Point p=points.get(i);if(!gameStyle&&!target.isEmpty()&&!target.equals(p.key))continue;float[] xy=screen(p.lat,p.lon);paint.setColor(color(p.tag)==Color.rgb(116,159,186)&&p.key.contains("BLE/")?0xff79a6ff:color(p.tag));paint.setStyle(Paint.Style.FILL);paint.setAlpha(180);if(!gameStyle)canvas.drawCircle(xy[0],xy[1],(p.key.equals(target)?5:3)*density,paint);paint.setAlpha(255);if((!gameStyle||target.equals(p.key))&&(strongest==null||p.rssi>strongest.rssi))strongest=p;}
        if(gameStyle)drawGamePoints(canvas);
        if(strongest!=null&&!target.isEmpty()){float[] s=screen(strongest.lat,strongest.lon);stroke(Color.WHITE,1.5f);canvas.drawCircle(s[0],s[1],10*density,paint);label(canvas,String.format(Locale.ROOT,"STRONGEST RX ±%.0f m",strongest.accuracy),s[0]+12*density,s[1]-10*density,Color.WHITE,10);}
        if(estimate!=null){float[] e=screen(estimate.lat,estimate.lon);double metersPerPixel=Math.cos(Math.toRadians(estimate.lat))*40075016.686/(256*(1<<zoom))/density;
            stroke(Color.rgb(255,162,66),2);paint.setPathEffect(dashed);canvas.drawCircle(e[0],e[1],(float)(estimate.radius/metersPerPixel),paint);paint.setPathEffect(null);canvas.drawLine(e[0]-8*density,e[1],e[0]+8*density,e[1],paint);canvas.drawLine(e[0],e[1]-8*density,e[0],e[1]+8*density,paint);label(canvas,(estimate.demo?"DEMO ":"")+"MODEL SEARCH AREA",e[0]+12*density,e[1]+18*density,Color.rgb(255,162,66),10);
        }
        if(receiver!=null){float[] here=screen(receiver.getLatitude(),receiver.getLongitude());stroke(Color.WHITE,2);canvas.drawCircle(here[0],here[1],7*density,paint);if(receiver.hasBearing()&&receiver.hasSpeed()&&receiver.getSpeed()>=1){double angle=Math.toRadians(receiver.getBearing());canvas.drawLine(here[0],here[1],here[0]+(float)Math.sin(angle)*22*density,here[1]-(float)Math.cos(angle)*22*density,paint);}label(canvas,"YOU",here[0]+10*density,here[1]+18*density,Color.WHITE,10);}
        stroke(Color.rgb(58,244,222),1);canvas.drawRect(1,1,getWidth()-1,getHeight()-1,paint);
        label(canvas,centered?String.format(Locale.ROOT,"%.5f  %.5f   Z%02d",lat,lon,zoom):"AWAITING GPS OBSERVATIONS",10*density,20*density,Color.rgb(58,244,222),11);
        label(canvas,"POINTS = RECEIVER POSITIONS",10*density,getHeight()-26*density,Color.rgb(173,198,208),10);
        label(canvas,"© OpenStreetMap contributors",10*density,getHeight()-10*density,Color.rgb(173,198,208),10);
    }

    List<String> nearbyKeys(String key){SignalMapModel.Point chosen=null;for(SignalMapModel.Point point:points)if(point.key.equals(key)){chosen=point;break;}if(chosen==null)return Collections.singletonList(key);float[] origin=screen(chosen.lat,chosen.lon);List<String> keys=new ArrayList<>();for(SignalMapModel.Point point:points){float[] at=screen(point.lat,point.lon);if(Math.hypot(at[0]-origin[0],at[1]-origin[1])<=32*density&&!keys.contains(point.key))keys.add(point.key);}return keys;}
    private int priority(SignalMapModel.Point point){String tag=point.tag;return tag.contains("FALCON")||tag.contains("RAVEN")?4:tag.contains("FLOCK")?3:tag.contains("CAMERA")||tag.contains("LPR")?2:0;}
    private void drawGamePoints(Canvas canvas){Map<String,List<SignalMapModel.Point>> clusters=new LinkedHashMap<>();for(SignalMapModel.Point point:points){float[] xy=screen(point.lat,point.lon);if(xy[0]<-20*density||xy[1]<-20*density||xy[0]>getWidth()+20*density||xy[1]>getHeight()+20*density)continue;String cell=(int)Math.floor(xy[0]/(32*density))+"/"+(int)Math.floor(xy[1]/(32*density));clusters.computeIfAbsent(cell,key->new ArrayList<>()).add(point);}List<RectF> labels=new ArrayList<>();
        for(List<SignalMapModel.Point> cluster:clusters.values()){cluster.sort((a,b)->{if(a.key.equals(b.key))return 0;if(a.key.equals(target))return -1;if(b.key.equals(target))return 1;int rank=Integer.compare(priority(b),priority(a));return rank!=0?rank:Integer.compare(b.rssi,a.rssi);});SignalMapModel.Point point=cluster.get(0);float[] xy=screen(point.lat,point.lon);int tint=color(point.tag);if(tint==Color.rgb(116,159,186)&&point.key.contains("BLE/"))tint=0xff79a6ff;paint.setColor(tint);paint.setAlpha(255);CreatureGlyph.draw(canvas,paint,xy[0],xy[1],(float)((8+Math.max(0,Math.min(1,(point.rssi+100)/60.0))*5)*density),DexCatalog.kind(point.tag,point.key));
            if(cluster.size()>1)label(canvas,"+"+(cluster.size()-1),xy[0]-8*density,xy[1]+27*density,tint,10);
            String name=cluster.size()>1?"RX CLUSTER "+cluster.size():DexCatalog.title(getContext(),DexCatalog.kind(point.tag,point.key));name+=" / "+point.rssi+" dBm";paint.setTypeface(Typeface.MONOSPACE);paint.setTextSize(10*density);float width=paint.measureText(name),x=xy[0]+20*density;if(x+width>getWidth()-4*density)x=xy[0]-width-20*density;float y=xy[1]-9*density;RectF box=new RectF(x,y-12*density,x+Math.max(width,140*density),y+16*density);boolean collision=false;for(RectF used:labels)if(RectF.intersects(used,box)){collision=true;break;}if(!collision){labels.add(box);label(canvas,name,x,y,tint,10);label(canvas,String.format(Locale.ROOT,"%.5f %.5f ±%.0fm",point.lat,point.lon,point.accuracy),x,y+13*density,tint,8);}
        }
    }
}
