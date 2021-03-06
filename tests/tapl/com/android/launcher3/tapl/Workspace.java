/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.launcher3.tapl;

import static com.android.launcher3.testing.TestProtocol.ALL_APPS_STATE_ORDINAL;

import static junit.framework.TestCase.assertTrue;

import android.graphics.Point;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiObject2;

import com.android.launcher3.testing.TestProtocol;

import java.util.regex.Pattern;

/**
 * Operations on the workspace screen.
 */
public final class Workspace extends Home {
    private static final float FLING_SPEED =
            LauncherInstrumentation.isAvd() ? 1500.0F : 3500.0F;
    private static final int DRAG_DURACTION = 2000;
    private final UiObject2 mHotseat;

    Workspace(LauncherInstrumentation launcher) {
        super(launcher);
        mHotseat = launcher.waitForLauncherObject("hotseat");
    }

    /**
     * Swipes up to All Apps.
     *
     * @return the App Apps object.
     */
    @NonNull
    public AllApps switchToAllApps() {
        try (LauncherInstrumentation.Closable c =
                     mLauncher.addContextLayer("want to switch from workspace to all apps")) {
            verifyActiveContainer();
            final UiObject2 hotseat = mHotseat;
            final Point start = hotseat.getVisibleCenter();
            start.y = hotseat.getVisibleBounds().bottom - 1;
            final int swipeHeight = mLauncher.getTestInfo(
                    TestProtocol.REQUEST_HOME_TO_ALL_APPS_SWIPE_HEIGHT).
                    getInt(TestProtocol.TEST_INFO_RESPONSE_FIELD);
            LauncherInstrumentation.log(
                    "switchToAllApps: swipeHeight = " + swipeHeight + ", slop = "
                            + mLauncher.getTouchSlop());

            mLauncher.getTestInfo(TestProtocol.REQUEST_ENABLE_DEBUG_TRACING);
            mLauncher.swipeToState(
                    start.x,
                    start.y,
                    start.x,
                    start.y - swipeHeight - mLauncher.getTouchSlop(),
                    60,
                    ALL_APPS_STATE_ORDINAL);
            mLauncher.getTestInfo(TestProtocol.REQUEST_DISABLE_DEBUG_TRACING);

            try (LauncherInstrumentation.Closable c1 = mLauncher.addContextLayer(
                    "swiped to all apps")) {
                return new AllApps(mLauncher);
            }
        }
    }

    /**
     * Returns an icon for the app, if currently visible.
     *
     * @param appName name of the app
     * @return app icon, if found, null otherwise.
     */
    @Nullable
    public AppIcon tryGetWorkspaceAppIcon(String appName) {
        try (LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                "want to get a workspace icon")) {
            final UiObject2 workspace = verifyActiveContainer();
            final UiObject2 icon = workspace.findObject(
                    AppIcon.getAppIconSelector(appName, mLauncher));
            return icon != null ? new AppIcon(mLauncher, icon) : null;
        }
    }


    /**
     * Returns an icon for the app; fails if the icon doesn't exist.
     *
     * @param appName name of the app
     * @return app icon.
     */
    @NonNull
    public AppIcon getWorkspaceAppIcon(String appName) {
        return new AppIcon(mLauncher,
                mLauncher.getObjectInContainer(
                        verifyActiveContainer(),
                        AppIcon.getAppIconSelector(appName, mLauncher)));
    }

    public void interruptDragHotseatIconToWorkspace(String pkgName) {
        final UiObject2 workspace = verifyActiveContainer();
        try (LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                "interrupt dragging icon to workspace before action up.")) {
            mLauncher.getTestInfo(TestProtocol.REQUEST_ENABLE_DEBUG_TRACING);
            LauncherInstrumentation.log("dragIconToWorkspace: begin");
            final Point launchableCenter = getHotseatAppIcon(pkgName).getObject().getVisibleCenter();
            final long downTime = SystemClock.uptimeMillis();
            mLauncher.sendPointer(downTime, downTime, MotionEvent.ACTION_DOWN, launchableCenter);
            LauncherInstrumentation.log("dragIconToWorkspace: sent down");
            mLauncher.waitForLauncherObject("deep_shortcuts_container");
            LauncherInstrumentation.log("dragIconToWorkspace: indicator");
            mLauncher.movePointer(
                    downTime, SystemClock.uptimeMillis(), DRAG_DURACTION, launchableCenter,
                    new Point(mLauncher.getDevice().getDisplayWidth() / 2,
                            workspace.getVisibleBounds().centerY()));
            LauncherInstrumentation.log("dragIconToWorkspace: moved pointer");
            mLauncher.getDevice().pressBack();
            verifyActiveContainer();
        }
    }

    public void dragIconToHotseatIfCanInset(String pkgName) {
        try (LauncherInstrumentation.Closable c =
                     mLauncher.addContextLayer("dragging icon to hotseat center.")) {
            verifyActiveContainer();
            final UiObject2 hotseat = mHotseat;
            dragIconToHotseat(
                    mLauncher,
                    getWorkspaceAppIcon(pkgName),
                    hotseat.getVisibleCenter(),
                    "deep_shortcuts_container"
            );

        }
    }

    /**
     * Ensures that workspace is scrollable. If it's not, drags an icon icons from hotseat to the
     * second screen.
     */
    public void ensureWorkspaceIsScrollable() {
        final UiObject2 workspace = verifyActiveContainer();
        if (!isWorkspaceScrollable(workspace)) {
            try (LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                    "dragging icon to a second page of workspace to make it scrollable")) {
                dragIconToWorkspace(
                        mLauncher,
                        getHotseatAppIcon("Chrome"),
                        new Point(mLauncher.getDevice().getDisplayWidth(),
                                workspace.getVisibleBounds().centerY()),
                        "deep_shortcuts_container");
                verifyActiveContainer();
            }
        }
        assertTrue("Home screen workspace didn't become scrollable",
                isWorkspaceScrollable(workspace));
    }

    private boolean isWorkspaceScrollable(UiObject2 workspace) {
        return workspace.isScrollable();
    }

    @NonNull
    public AppIcon getHotseatAppIcon(String appName) {
        return new AppIcon(mLauncher, mLauncher.getObjectInContainer(
                mHotseat, AppIcon.getAppIconSelector(appName, mLauncher)));
    }

    @NonNull
    public AppIcon getHotseatAppIcon(Pattern appName) {
        return new AppIcon(mLauncher, mLauncher.getObjectInContainer(
                mHotseat, By.clazz(TextView.class).text(appName).pkg(mLauncher.getLauncherPackageName())));
    }

    @NonNull
    public Folder getHotseatFolder(String appName) {
        return new Folder(mLauncher, mLauncher.getObjectInContainer(
                mHotseat, Folder.getSelector(appName, mLauncher)));
    }

    static void dragIconToHotseat(
            LauncherInstrumentation launcher, Launchable launchable, Point dest,
            String longPressIndicator) {
        dragIconToWorkspace(launcher, launchable, dest, longPressIndicator);
    }

    static void dragIconToWorkspace(
            LauncherInstrumentation launcher, Launchable launchable, Point dest,
            String longPressIndicator) {
        launcher.getTestInfo(TestProtocol.REQUEST_ENABLE_DEBUG_TRACING);
        LauncherInstrumentation.log("dragIconToWorkspace: begin");
        final Point launchableCenter = launchable.getObject().getVisibleCenter();
        final long downTime = SystemClock.uptimeMillis();
        launcher.sendPointer(downTime, downTime, MotionEvent.ACTION_DOWN, launchableCenter);
        LauncherInstrumentation.log("dragIconToWorkspace: sent down");
        launcher.waitForLauncherObject(longPressIndicator);
        LauncherInstrumentation.log("dragIconToWorkspace: indicator");
        launcher.movePointer(
                downTime, SystemClock.uptimeMillis(), DRAG_DURACTION, launchableCenter, dest);
        LauncherInstrumentation.log("dragIconToWorkspace: moved pointer");
        launcher.sendPointer(
                downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, dest);
        LauncherInstrumentation.log("dragIconToWorkspace: end");
        launcher.waitUntilGone("drop_target_bar");
        launcher.getTestInfo(TestProtocol.REQUEST_DISABLE_DEBUG_TRACING);
    }

    /**
     * Flings to get to screens on the right. Waits for scrolling and a possible overscroll
     * recoil to complete.
     */
    public void flingForward() {
        final UiObject2 workspace = verifyActiveContainer();
        workspace.setGestureMargins(0, 0, mLauncher.getEdgeSensitivityWidth(), 0);
        workspace.fling(Direction.RIGHT, (int) (FLING_SPEED * mLauncher.getDisplayDensity()));
        mLauncher.waitForIdle();
        verifyActiveContainer();
    }

    /**
     * Flings to get to screens on the left.  Waits for scrolling and a possible overscroll
     * recoil to complete.
     */
    public void flingBackward() {
        final UiObject2 workspace = verifyActiveContainer();
        workspace.setGestureMargins(mLauncher.getEdgeSensitivityWidth(), 0, 0, 0);
        workspace.fling(Direction.LEFT, (int) (FLING_SPEED * mLauncher.getDisplayDensity()));
        mLauncher.waitForIdle();
        verifyActiveContainer();
    }

    /**
     * Opens widgets container by pressing Ctrl+W.
     *
     * @return the widgets container.
     */
    @NonNull
    public Widgets openAllWidgets() {
        verifyActiveContainer();
        mLauncher.getDevice().pressKeyCode(KeyEvent.KEYCODE_W, KeyEvent.META_CTRL_ON);
        try (LauncherInstrumentation.Closable c = mLauncher.addContextLayer("pressed Ctrl+W")) {
            return new Widgets(mLauncher);
        }
    }

    @Override
    protected String getSwipeHeightRequestName() {
        return TestProtocol.REQUEST_HOME_TO_OVERVIEW_SWIPE_HEIGHT;
    }

    @Override
    protected int getSwipeStartY() {
        return mLauncher.getRealDisplaySize().y - 1;
    }

    @Nullable
    public Widget tryGetWidget(String label, long timeout) {
        final UiObject2 widget = mLauncher.tryWaitForLauncherObject(
                By.clazz("com.android.launcher3.widget.LauncherAppWidgetHostView").desc(label),
                timeout);
        return widget != null ? new Widget(mLauncher, widget) : null;
    }

    @Nullable
    public Widget tryGetPendingWidget(long timeout) {
        final UiObject2 widget = mLauncher.tryWaitForLauncherObject(
                By.clazz("com.android.launcher3.widget.PendingAppWidgetHostView"), timeout);
        return widget != null ? new Widget(mLauncher, widget) : null;
    }

    public void touchLeftToRight(Point start, Point dest, boolean sendTouchUp) {
        LauncherInstrumentation.log("touchLeftToRight: begin");
        final long downTime = SystemClock.uptimeMillis();
        mLauncher.sendPointer(downTime, downTime, MotionEvent.ACTION_DOWN, start);
        LauncherInstrumentation.log("touchLeftToRight: sent down");
        mLauncher.movePointer(
                downTime, SystemClock.uptimeMillis(), DRAG_DURACTION, start, dest);
        LauncherInstrumentation.log("touchLeftToRight: moved pointer");
        if (sendTouchUp) {
            mLauncher.sendPointer(
                    downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, dest);
            LauncherInstrumentation.log("touchLeftToRight: end");
        }
    }

    /**
     * Swip down in workspace.
     */
    @NonNull
    public void swipDownInWorkspace() {
        mLauncher.getTestInfo(TestProtocol.REQUEST_ENABLE_DEBUG_TRACING);
        LauncherInstrumentation.log("swip down : begin");
        final Point startPoint = mLauncher.getRealDisplaySize();
        final Point endPoint = mLauncher.getRealDisplaySize();
        startPoint.y /= 2;
        startPoint.x /= 2;
        endPoint.x = startPoint.x;
        endPoint.y -= 100;
        mLauncher.getDevice().swipe(startPoint.x, startPoint.y, endPoint.x, endPoint.y, 50);
    }

}