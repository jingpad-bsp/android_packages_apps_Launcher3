/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.launcher3.graphics.RotationMode;
import com.android.launcher3.logging.StatsLogUtils.LogContainerProvider;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.userevent.nano.LauncherLogProto.Target;
import com.android.launcher3.views.Transposable;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.grid.HotseatController;

public class Hotseat extends CellLayout implements LogContainerProvider, Insettable, Transposable {
    private static final float ICON_SIZE_SCALE = 1.2f;
    private static final int DIVIDER_WIDTH_DP = 24;
    private final HotseatController mController;
    @ViewDebug.ExportedProperty(category = "launcher")
    public boolean mHasVerticalHotseat;
    private int dividerWidth;
    private HotseatRecentApp hotseatRecentApp;
    private boolean drawRecentApp = false;
    private Drawable hotseatRecentDivider;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        dividerWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DIVIDER_WIDTH_DP, context.getResources().getDisplayMetrics());
        hotseatRecentDivider = context.getDrawable(R.drawable.hotseat_recent_divider);
        hotseatRecentApp = new HotseatRecentApp(context);
        hotseatRecentApp.setCellDimensions(mCellWidth, mCellHeight, getCountX(), getCountY());
        addView(hotseatRecentApp);
        mController = LauncherAppMonitor.getInstance(context).getHotseatController();
        setGridSize(mActivity.getDeviceProfile().inv.numHotseatIcons, 1);
    }

    public HotseatController getController() {
        return mController;
    }

    /* Get the orientation specific coordinates given an invariant order in the hotseat. */
    public int getCellXFromOrder(int rank) {
        return mHasVerticalHotseat ? 0 : rank;
    }

    public int getCellYFromOrder(int rank) {
        return mHasVerticalHotseat ? (getCountY() - (rank + 1)) : 0;
    }

    public void resetLayout(boolean hasVerticalHotseat) {
        setGridSize(mActivity.getDeviceProfile().inv.numHotseatIcons, 1);
        removeAllViewsInLayout();
        mHasVerticalHotseat = hasVerticalHotseat;
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        child.setBackgroundResource(R.drawable.hotseat_bg);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        int mh = container.getMeasuredHeight();
        int cellHeight = mh / getCountY();
        int size = getCellSize();
        setCellDimensions(size, cellHeight);
        container.measure(
                MeasureSpec.makeMeasureSpec(size * getCountX(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mh, MeasureSpec.EXACTLY));
        int recentCount = hotseatRecentApp.getChildCount();
        hotseatRecentApp.setRotation(getRotationMode().surfaceRotation);
        hotseatRecentApp.setCellDimensions(mCellWidth, mCellHeight, recentCount, 1);
        hotseatRecentApp.measure(
                MeasureSpec.makeMeasureSpec(size * recentCount, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mh, MeasureSpec.EXACTLY));

    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (drawRecentApp) {
            hotseatRecentDivider.draw(canvas);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        drawRecentApp = hotseatRecentApp.getChildCount() > 0;
        if (getDesiredWidth() + dividerWidth + hotseatRecentApp.getMeasuredWidth() > getWidth()) {
            int available = getWidth() - getDesiredWidth() - dividerWidth;
            int cellWidth = getCellWidth();
            int count = available / cellWidth;
            if (count == 0) {
                drawRecentApp = false;
            } else {
                hotseatRecentApp.measure(
                        MeasureSpec.makeMeasureSpec(cellWidth * count, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(container.getHeight(), MeasureSpec.EXACTLY));
            }
        }
        if (drawRecentApp) {
            int left = getWidth() - container.getWidth() - dividerWidth - hotseatRecentApp.getMeasuredWidth();
            left /= 2;
            container.offsetLeftAndRight(left - container.getLeft());
            hotseatRecentApp.layout(container.getRight() + dividerWidth, container.getTop(), container.getRight() + dividerWidth + hotseatRecentApp.getMeasuredWidth(), container.getBottom());
            hotseatRecentDivider.setBounds(container.getRight(), container.getTop(), container.getRight() + dividerWidth, container.getBottom());
        } else {
            hotseatRecentApp.layout(0, 0, 0, 0);
        }
    }

    private int getCellSize() {
        return (int) (mActivity.getDeviceProfile().iconSizePx * ICON_SIZE_SCALE);
    }

    @Override
    public void cellToCenterPoint(int cellX, int cellY, int[] result) {
        cellToPoint(cellX, cellY, result);
        result[0] += getCellSize() / 2;
    }

    @Override
    void cellToPoint(int cellX, int cellY, int[] result) {
        super.cellToPoint(cellX, cellY, result);
        int ori = mActivity.getDeviceProfile().inv.numHotseatIcons;
        if (mHasVerticalHotseat) {
            if (ori != getCountY()) {
                result[1] += mCellHeight * (ori - getCountY()) / 2;
            }
        } else {
            int cellSize = getCellSize();
            result[0] = cellSize * cellX + (getWidth() - getCountX() * cellSize) / 2;
            if (drawRecentApp) {
                result[0] -= (dividerWidth + hotseatRecentApp.getWidth()) / 2;
            }
        }
    }

    @Override
    public void setCellDimensions(int width, int height) {
        super.setCellDimensions(width, height);
        hotseatRecentApp.setCellDimensions(width, height, hotseatRecentApp.getChildCount(), 1);
    }

    @Override
    public void fillInLogContainerData(View v, ItemInfo info, Target target, Target targetParent) {
        target.gridX = info.cellX;
        target.gridY = info.cellY;
        targetParent.containerType = LauncherLogProto.ContainerType.HOTSEAT;
    }

    @Override
    public void setInsets(Rect insets) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        DeviceProfile grid = mActivity.getWallpaperDeviceProfile();
        insets = grid.getInsets();

        if (grid.isVerticalBarLayout()) {
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (grid.isSeascape()) {
                lp.gravity = Gravity.LEFT;
                lp.width = grid.hotseatBarSizePx + insets.left;
            } else {
                lp.gravity = Gravity.RIGHT;
                lp.width = grid.hotseatBarSizePx + insets.right;
            }
        } else {
            lp.gravity = Gravity.BOTTOM;
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = grid.hotseatBarSizePx + insets.bottom;
        }
        Rect padding = grid.getHotseatLayoutPadding();
        setPadding(padding.left, padding.top, padding.right, padding.bottom);

        setLayoutParams(lp);
        InsettableFrameLayout.dispatchInsets(this, insets);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Don't let if follow through to workspace
        return true;
    }

    @Override
    public RotationMode getRotationMode() {
        return Launcher.getLauncher(getContext()).getRotationMode();
    }

}
