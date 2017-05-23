/*
 * This is the source code of Supergram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.Supergram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import com.baranak.tsupergrap.AndroidUtilities;
import org.Supergram.ui.ActionBar.Theme;

public class DividerCell extends View {

    public DividerCell(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(16) + 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawLine(getPaddingLeft(), AndroidUtilities.dp(8), getWidth() - getPaddingRight(), AndroidUtilities.dp(8), Theme.dividerPaint);
    }
}
