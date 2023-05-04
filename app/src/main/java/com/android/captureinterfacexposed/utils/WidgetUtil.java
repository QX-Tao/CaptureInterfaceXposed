package com.android.captureinterfacexposed.utils;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.View;
import android.view.ViewOutlineProvider;

public class WidgetUtil {
    public static void setRound(View view, float radius){
        ViewOutlineProvider viewOutlineProvider = new ViewOutlineProvider(){
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),radius);
            }
        };
        view.setOutlineProvider(viewOutlineProvider);
        view.setClipToOutline(true);
    }

    public static void setRipple(View view){
        int[][] stateList = new int[][]{
                new int[]{android.R.attr.state_pressed},
                new int[]{android.R.attr.state_focused},
                new int[]{android.R.attr.state_activated},
                new int[]{}
        };
        int normalColor = Color.parseColor("#143C4043");
        int pressedColor = Color.parseColor("#28000000");
        int[] stateColorList = new int[]{
                pressedColor,
                pressedColor,
                pressedColor,
                normalColor
        };
        ColorStateList colorStateList = new ColorStateList(stateList, stateColorList);
        float[] outRadius = new float[]{10, 10, 15, 15, 20, 20, 25, 25};
        RoundRectShape roundRectShape = new RoundRectShape(outRadius, null, null);
        ShapeDrawable maskDrawable = new ShapeDrawable();
        maskDrawable.setShape(roundRectShape);
        maskDrawable.getPaint().setColor(Color.parseColor("#143C4043"));
        maskDrawable.getPaint().setStyle(Paint.Style.FILL);
        RippleDrawable rippleDrawable = new RippleDrawable(colorStateList, null, maskDrawable);
        view.setBackground(rippleDrawable);
    }

    public static void setStroke(View view, int width){
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadii(new float[]{20, 20, 20, 20, 20, 20, 20, 20});
        gradientDrawable.setStroke(width, Color.BLACK);
        view.setBackground(gradientDrawable);
    }

    public static void setRoundRippleStroke(View view, int StrokeWidth, String strokeColorString, float roundRadius){
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadii(new float[]{roundRadius, roundRadius, roundRadius, roundRadius, roundRadius, roundRadius, roundRadius, roundRadius});
        gradientDrawable.setStroke(StrokeWidth, Color.parseColor(strokeColorString));

        int[][] stateList = new int[][]{
                new int[]{android.R.attr.state_pressed},
                new int[]{android.R.attr.state_focused},
                new int[]{android.R.attr.state_activated},
                new int[]{}
        };
        int normalColor = Color.parseColor("#143C4043");
        int pressedColor = Color.parseColor("#28000000");
        int[] stateColorList = new int[]{
                pressedColor,
                pressedColor,
                pressedColor,
                normalColor
        };
        ColorStateList colorStateList = new ColorStateList(stateList, stateColorList);
        float[] outRadius = new float[]{10, 10, 15, 15, 20, 20, 25, 25};
        RoundRectShape roundRectShape = new RoundRectShape(outRadius, null, null);
        ShapeDrawable maskDrawable = new ShapeDrawable();
        maskDrawable.setShape(roundRectShape);
        maskDrawable.getPaint().setColor(Color.parseColor("#143C4043"));
        maskDrawable.getPaint().setStyle(Paint.Style.FILL);
        RippleDrawable rippleDrawable = new RippleDrawable(colorStateList, null, maskDrawable);

        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), roundRadius);
            }
        });
        view.setClipToOutline(true);
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{gradientDrawable, rippleDrawable});
        view.setBackground(layerDrawable);
    }

}
