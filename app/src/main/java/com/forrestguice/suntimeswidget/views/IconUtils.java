/**
    Copyright (C) 2026 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.forrestguice.suntimeswidget.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.forrestguice.annotation.NonNull;
import com.forrestguice.annotation.Nullable;
import com.forrestguice.suntimeswidget.R;
import com.forrestguice.support.content.ContextCompat;
import com.forrestguice.util.Log;

public class IconUtils
{
    /**
     * @param context Context
     * @param drawableAttr attr pointing to ic_action_ drawable
     * @param defaultDrawable default ic_action_ drawable
     * @return a Drawable suitable for AlertDialog.setIcon
     */
    @Nullable
    public static Drawable getAlertDialogIcon(Context context, int drawableAttr, int defaultDrawable)
    {
        Drawable d = ContextCompat.getDrawable(context, getThemedIcon(context, drawableAttr, defaultDrawable));
        if (d != null)
        {
            int w = d.getIntrinsicWidth();
            int h = d.getIntrinsicHeight();
            int s = (int) context.getResources().getDimension(R.dimen.alertIcon_size);

            float sizeDifferencePx = (s - w);
            if (sizeDifferencePx > 0.1)    // upscale 24x24 "action icons" to 32x32 "alert icons"
            {
                int offsetPx = (int) Math.max(sizeDifferencePx / 2f, 0);
                if (Build.VERSION.SDK_INT >= 21)
                {
                    Bitmap b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(b);
                    d.setBounds(offsetPx, offsetPx, w + offsetPx, h + offsetPx);
                    d.draw(c);
                    return new BitmapDrawable(context.getResources(), b);
                }
            }
        }
        return d;
    }

    public static int getThemedIcon(Context context, int drawableAttr, int defaultDrawable)
    {
        TypedArray typedArray = context.obtainStyledAttributes(new int[] { drawableAttr });
        int resID = typedArray.getResourceId(0, defaultDrawable);
        typedArray.recycle();
        return resID;
    }
}
