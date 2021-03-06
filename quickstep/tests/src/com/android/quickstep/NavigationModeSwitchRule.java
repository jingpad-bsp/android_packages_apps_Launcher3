/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.quickstep;

import static androidx.test.InstrumentationRegistry.getInstrumentation;

import static com.android.quickstep.NavigationModeSwitchRule.Mode.ALL;
import static com.android.quickstep.NavigationModeSwitchRule.Mode.THREE_BUTTON;
import static com.android.quickstep.NavigationModeSwitchRule.Mode.TWO_BUTTON;
import static com.android.quickstep.NavigationModeSwitchRule.Mode.ZERO_BUTTON;
import static com.android.systemui.shared.system.QuickStepContract.NAV_BAR_MODE_2BUTTON_OVERLAY;
import static com.android.systemui.shared.system.QuickStepContract.NAV_BAR_MODE_3BUTTON_OVERLAY;
import static com.android.systemui.shared.system.QuickStepContract.NAV_BAR_MODE_GESTURAL_OVERLAY;

import android.content.Context;
import android.util.Log;

import androidx.test.uiautomator.UiDevice;

import com.android.launcher3.tapl.LauncherInstrumentation;
import com.android.launcher3.tapl.TestHelpers;
import com.android.systemui.shared.system.QuickStepContract;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test rule that allows executing a test with Quickstep on and then Quickstep off.
 * The test should be annotated with @QuickstepOnOff.
 */
public class NavigationModeSwitchRule implements TestRule {

    static final String TAG = "QuickStepOnOffRule";

    public enum Mode {
        THREE_BUTTON, TWO_BUTTON, ZERO_BUTTON, ALL
    }

    // Annotation for tests that need to be run with quickstep enabled and disabled.
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface NavigationModeSwitch {
        Mode mode() default ALL;
    }

    private final LauncherInstrumentation mLauncher;

    public NavigationModeSwitchRule(LauncherInstrumentation launcher) {
        mLauncher = launcher;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if (TestHelpers.isInLauncherProcess()
                && LauncherInstrumentation.hasNavigationBar(getInstrumentation().getContext())
                && description.getAnnotation(NavigationModeSwitch.class) != null) {
            Mode mode = description.getAnnotation(NavigationModeSwitch.class).mode();
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    final Context context = getInstrumentation().getContext();
                    final int currentInteractionMode =
                            LauncherInstrumentation.getCurrentInteractionMode(context);
                    final String prevOverlayPkg =
                            QuickStepContract.isGesturalMode(currentInteractionMode)
                                    ? NAV_BAR_MODE_GESTURAL_OVERLAY
                                    : QuickStepContract.isSwipeUpMode(currentInteractionMode)
                                            ? NAV_BAR_MODE_2BUTTON_OVERLAY
                                            : NAV_BAR_MODE_3BUTTON_OVERLAY;
                    final LauncherInstrumentation.NavigationModel originalMode =
                            mLauncher.getNavigationModel();
                    try {
                        if (mode == ZERO_BUTTON || mode == ALL) {
                            evaluateWithZeroButtons();
                        }
                        if (mode == TWO_BUTTON || mode == ALL) {
                            evaluateWithTwoButtons();
                        }
                        if (mode == THREE_BUTTON || mode == ALL) {
                            evaluateWithThreeButtons();
                        }
                    } finally {
                        setActiveOverlay(prevOverlayPkg, originalMode);
                    }
                }

                public void evaluateWithoutChangingSetting(Statement base) throws Throwable {
                    base.evaluate();
                }

                private void evaluateWithThreeButtons() throws Throwable {
                    setActiveOverlay(NAV_BAR_MODE_3BUTTON_OVERLAY,
                            LauncherInstrumentation.NavigationModel.THREE_BUTTON);
                    evaluateWithoutChangingSetting(base);
                }

                private void evaluateWithTwoButtons() throws Throwable {
                    setActiveOverlay(NAV_BAR_MODE_2BUTTON_OVERLAY,
                            LauncherInstrumentation.NavigationModel.TWO_BUTTON);
                    base.evaluate();
                }

                private void evaluateWithZeroButtons() throws Throwable {
                    setActiveOverlay(NAV_BAR_MODE_GESTURAL_OVERLAY,
                            LauncherInstrumentation.NavigationModel.ZERO_BUTTON);
                    base.evaluate();
                }

                private void setActiveOverlay(String overlayPackage,
                        LauncherInstrumentation.NavigationModel expectedMode) throws Exception {
                    setOverlayPackageEnabled(NAV_BAR_MODE_3BUTTON_OVERLAY,
                            overlayPackage == NAV_BAR_MODE_3BUTTON_OVERLAY);
                    setOverlayPackageEnabled(NAV_BAR_MODE_2BUTTON_OVERLAY,
                            overlayPackage == NAV_BAR_MODE_2BUTTON_OVERLAY);
                    setOverlayPackageEnabled(NAV_BAR_MODE_GESTURAL_OVERLAY,
                            overlayPackage == NAV_BAR_MODE_GESTURAL_OVERLAY);

                    if (currentSysUiNavigationMode() != expectedMode) {
                        final CountDownLatch latch = new CountDownLatch(1);
                        final Context targetContext = getInstrumentation().getTargetContext();
                        final SysUINavigationMode.NavigationModeChangeListener listener =
                                newMode -> {
                                    if (LauncherInstrumentation.getNavigationModel(newMode.resValue)
                                            == expectedMode) {
                                        latch.countDown();
                                    }
                                };
                        final SysUINavigationMode sysUINavigationMode =
                                SysUINavigationMode.INSTANCE.get(targetContext);
                        targetContext.getMainExecutor().execute(() ->
                                sysUINavigationMode.addModeChangeListener(listener));
                        latch.await(10, TimeUnit.SECONDS);
                        targetContext.getMainExecutor().execute(() ->
                                sysUINavigationMode.removeModeChangeListener(listener));
                        Assert.assertTrue("Navigation mode didn't change to " + expectedMode,
                                currentSysUiNavigationMode() == expectedMode);
                    }

                    for (int i = 0; i != 100; ++i) {
                        if (mLauncher.getNavigationModel() == expectedMode) break;
                        Thread.sleep(100);
                    }
                    Assert.assertTrue("Couldn't switch to " + overlayPackage,
                            mLauncher.getNavigationModel() == expectedMode);

                    for (int i = 0; i != 100; ++i) {
                        if (mLauncher.getNavigationModeMismatchError() == null) break;
                        Thread.sleep(100);
                    }
                    final String error = mLauncher.getNavigationModeMismatchError();
                    Assert.assertTrue("Switching nav mode: " + error, error == null);

                    Thread.sleep(5000);
                }

                private void setOverlayPackageEnabled(String overlayPackage, boolean enable)
                        throws Exception {
                    Log.d(TAG, "setOverlayPackageEnabled: " + overlayPackage + " " + enable);
                    final String action = enable ? "enable" : "disable";
                    UiDevice.getInstance(getInstrumentation()).executeShellCommand(
                            "cmd overlay " + action + " " + overlayPackage);
                }
            };
        } else {
            return base;
        }
    }

    private static LauncherInstrumentation.NavigationModel currentSysUiNavigationMode() {
        return LauncherInstrumentation.getNavigationModel(
                SysUINavigationMode.getMode(
                        getInstrumentation().
                                getTargetContext()).
                        resValue);
    }
}
