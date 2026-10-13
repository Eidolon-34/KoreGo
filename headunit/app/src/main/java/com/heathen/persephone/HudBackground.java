package com.heathen.persephone;
import android.graphics.*;
import android.graphics.drawable.Drawable;

final class HudBackground extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    @Override public void draw(Canvas canvas){Rect b=getBounds();canvas.drawColor(Color.rgb(5,12,19));paint.setColor(Color.rgb(12,27,35));paint.setStrokeWidth(1);
        for(int x=b.left;x<b.right;x+=48)canvas.drawLine(x,b.top,x,b.bottom,paint);for(int y=b.top;y<b.bottom;y+=48)canvas.drawLine(b.left,y,b.right,y,paint);
        paint.setColor(Color.rgb(255,222,67));paint.setStrokeWidth(3);canvas.drawLine(b.left+4,b.top+14,b.left+4,b.top+70,paint);canvas.drawLine(b.right-4,b.bottom-70,b.right-4,b.bottom-14,paint);
    }
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
