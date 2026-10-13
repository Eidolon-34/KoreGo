package com.heathen.persephone;
import android.graphics.*;
/** Original circuit silhouettes. No Pokémon/Ingress assets. */
final class CreatureGlyph {
    static void draw(Canvas canvas,Paint paint,float x,float y,float radius,String kind){int color=paint.getColor();paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,radius/8));canvas.drawCircle(x,y,radius+4,paint);Path shape=new Path();
        if(kind.equals("ble")||kind.equals("raven")){shape.moveTo(x,y);shape.lineTo(x-radius,y-radius);shape.lineTo(x-radius,y+radius*.7f);shape.lineTo(x,y+radius*.3f);shape.lineTo(x+radius,y+radius*.7f);shape.lineTo(x+radius,y-radius);shape.close();}
        else if(kind.equals("flock")){shape.moveTo(x-radius,y+radius);shape.lineTo(x-radius,y-radius);shape.lineTo(x-radius*.4f,y-radius*.3f);shape.lineTo(x+radius*.4f,y-radius*.3f);shape.lineTo(x+radius,y-radius);shape.lineTo(x+radius,y+radius);shape.close();}
        else if(kind.equals("camera")){canvas.drawRoundRect(x-radius,y-radius*.8f,x+radius,y+radius*.8f,radius/3,radius/3,paint);canvas.drawCircle(x,y,radius*.5f,paint);}
        else{shape.moveTo(x,y-radius);shape.lineTo(x+radius,y);shape.lineTo(x,y+radius);shape.lineTo(x-radius,y);shape.close();}canvas.drawPath(shape,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);canvas.drawCircle(x,y,Math.max(2,radius/5),paint);paint.setColor(color);
    }
}
