package com.example.hwc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class m extends View {

    private Paint paint;
    private Path path;

    // لتخزين النقاط المرسومة
    private Bitmap bitmap;
    private Canvas bitmapCanvas;

    public m(Context context) {
        super(context);
        init();
    }

    public m(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.BLACK); // لون القلم
        paint.setAntiAlias(true);
        paint.setStrokeWidth(8f); // سمك القلم
        paint.setStyle(Paint.Style.STROKE); // رسم فقط الحواف (بدون تعبئة)

        path = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // إعداد bitmap لتخزين الرسم عند تغييرات الحجم
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // رسم الرسم على الشاشة
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                // لا شيء هنا
                break;
            default:
                return false;
        }
        invalidate(); // طلب إعادة الرسم لعرض النقاط الجديدة
        return true;
    }

    // دالة للحصول على صورة من الرسم
    public Bitmap getBitmap() {
        return bitmap;
    }

    // دالة لمسح الرسم الحالي
    public void clearDrawing() {
        path.reset();
        bitmap.eraseColor(Color.WHITE); // مسح الرسم
        invalidate();
    }
}
