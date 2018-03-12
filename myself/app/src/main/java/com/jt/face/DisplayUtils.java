package com.jt.face;

import android.content.Context;

/**
 * Created by zgq on 2018-03-09.
 */

public class DisplayUtils {
    public static int Dp2px(Context context, float dp){
       final float scale =context.getResources().getDisplayMetrics().density;
       return (int) (dp*scale+0.5f);
    }
}
