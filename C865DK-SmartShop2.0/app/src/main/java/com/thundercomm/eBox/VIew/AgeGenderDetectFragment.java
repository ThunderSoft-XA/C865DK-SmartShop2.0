package com.thundercomm.eBox.VIew;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import com.thundercomm.eBox.Utils.LogUtil;
import com.thundercomm.eBox.Data.ObjectData;

import java.util.Vector;

public class AgeGenderDetectFragment extends PlayFragment {
    private static final String TAG = "AgeGenderDetectFragment";
    protected Paint paint_Object;

    @Override
    void initPaint() {
        super.initPaint();

        paint_Object = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint_Object.setColor(Color.CYAN);
        paint_Object.setShadowLayer(10f, 0, 0, Color.CYAN);
        paint_Object.setStyle(Paint.Style.STROKE);
        paint_Object.setStrokeWidth(4);
        paint_Object.setFilterBitmap(true);
    }

    public AgeGenderDetectFragment(int id) {
        super(id);
    }

    private void drawFaceRect(final SurfaceHolder mHolder, Vector<ObjectData> mObjectVec) {
        Canvas canvas = null;
        if (mHolder != null) {
            LogUtil.i(TAG, " " + mObjectVec.toString());
            try {
                if (paint_Txt == null || paint_Object == null) {
                    initPaint();
                }
                canvas = mHolder.lockCanvas();
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                for (ObjectData objectData : mObjectVec) {
                    int x = (int) objectData.rect[0];
                    int y = (int) objectData.rect[1];
                    int w = (int) objectData.rect[2];
                    int h = (int) objectData.rect[3];
                    Rect rect = new Rect(x, y, x + w, y + h);
                    drawRound(canvas, x, y, w, h, paint_Object);
                    Point show_textPoint = get_show_coordinate(mFaceRectView.getWidth(), mFaceRectView.getHeight(), rect);
                    String msg = objectData.toString();
                    //msg = msg.split(" ")[0];
                    canvas.drawText(msg, show_textPoint.x, show_textPoint.y, paint_Txt);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != canvas) {
                    mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
        hasFaceRects = false;
    }

    public void OndrawFace(Vector<ObjectData> mObjectVec) {
        drawFaceRect(mFaceViewHolder, mObjectVec);
        hasFaceRects = true;
    }

}
