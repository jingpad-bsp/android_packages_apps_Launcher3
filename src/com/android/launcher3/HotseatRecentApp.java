package com.android.launcher3;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.view.View;

import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.quickstep.RecentsModel;
import com.android.quickstep.TaskIconCache;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.TaskStackChangeListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.WeakHashMap;
import java.util.stream.IntStream;

@SuppressLint("NewApi")
public class HotseatRecentApp extends ShortcutAndWidgetContainer {
    private static final ArrayList<ComponentName> recentExcludes = new ArrayList<>();

    static {
        recentExcludes.add(ComponentName.createRelative("com.android.storagemanager", ".deletionhelper.DeletionHelperActivity"));
    }

    protected final Launcher mActivity;
    private final RecentsModel mModel;
    private int mTaskListChangeId = -1;
    private static View.OnClickListener recentClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Task task = (Task) view.getTag(R.id.content);
            ActivityManagerWrapper.getInstance().startActivityFromRecentsAsync(task.key, ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.getWidth() * 2, view.getHeight() * 2)
                    , null, null);
        }
    };
    private final TaskStackChangeListener stackChangeListener = new TaskStackChangeListener() {
        @Override
        public void onTaskStackChangedBackground() {
            super.onTaskStackChangedBackground();
        }

        @Override
        public void onTaskStackChanged() {
            super.onTaskStackChanged();
            reloadIfNeeded(false);
        }
    };


    public HotseatRecentApp(Context context) {
        super(context, CellLayout.HOTSEAT);
        mActivity = (Launcher) BaseActivity.fromContext(context);
        mModel = RecentsModel.INSTANCE.get(context);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ActivityManagerWrapper.getInstance().registerTaskStackListener(stackChangeListener);
        ShortcutAndWidgetContainer hotseatContainer = mActivity.getHotseat().getShortcutsAndWidgets();
        hotseatContainer.setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View view, View view1) {
                reloadIfNeeded(true);
            }

            @Override
            public void onChildViewRemoved(View view, View view1) {
                reloadIfNeeded(true);
            }
        });

        postOnAnimationDelayed(() -> reloadIfNeeded(false), 200);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ActivityManagerWrapper.getInstance().unregisterTaskStackListener(stackChangeListener);
    }

    public void reloadIfNeeded(boolean force) {
        if (force || !mModel.isTaskListValid(mTaskListChangeId)) {
            mTaskListChangeId = mModel.getTasks(this::applyLoadPlan);
        }
    }

    @SuppressLint("NewApi")
    private void applyLoadPlan(ArrayList<Task> tasks) {
        removeAllViewsInLayout();
        Collection<AppInfo> apps = mActivity.getAppsView().getAppsStore().getApps();
        ArrayList<WorkspaceItemInfo> hotseatList = new ArrayList<>();
        ShortcutAndWidgetContainer hotseatContainer = mActivity.getHotseat().getShortcutsAndWidgets();
        for (int i = 0; i < hotseatContainer.getChildCount(); i++) {
            Object tag = hotseatContainer.getChildAt(i).getTag();
            if (tag instanceof WorkspaceItemInfo) {
                hotseatList.add((WorkspaceItemInfo) tag);
            }
        }

        IntStream.iterate(tasks.size() - 1, i -> i - 1).limit(tasks.size())
                .mapToObj(tasks::get)
                .filter(task -> {
                    Intent baseIntent = task.key.baseIntent;
                    if (recentExcludes.stream().anyMatch(it -> it.equals(baseIntent.getComponent())))
                        return false;
                    return hotseatList.stream()
                            .map(it -> it.intent.getComponent())
                            .noneMatch(component -> component.equals(baseIntent.getComponent()) || component.getPackageName().equals(baseIntent.getComponent().getPackageName()));
                })
                .limit(Configuration.ORIENTATION_LANDSCAPE == getResources().getConfiguration().orientation ? 4 : 2)
                .forEach(task -> {
                    RecentsModel model = RecentsModel.INSTANCE.get(getContext());
                    TaskIconCache iconCache = model.getIconCache();
                    Intent baseIntent = task.key.baseIntent;
                    AppInfo appInfo = new AppInfo(getContext(), LauncherAppsCompat.getInstance(getContext()).resolveActivity(baseIntent, Process.myUserHandle()), Process.myUserHandle());
                    View view = mActivity.createShortcut(this, appInfo.makeWorkspaceItem());
                    view.setOnClickListener(recentClick);
                    view.setTag(R.id.content, task);
                    if (view instanceof BubbleTextView) {
                        updateViewIcon(task, iconCache, (BubbleTextView) view);
                    }
                    int index = getChildCount();
                    view.setId(index + 1);
                    addViewInLayout(view, index, new CellLayout.LayoutParams(index, 0, 1, 1));

                });
        requestLayout();
        invalidate();
    }

    private WeakHashMap<Integer, Drawable> iconCacheMap = new WeakHashMap<>();

    private void updateViewIcon(Task task, TaskIconCache iconCache, BubbleTextView textView) {
        textView.setIconVisible(false);
        textView.setTextVisibility(false);
        if (null == task.icon) {
            Drawable cache = iconCacheMap.get(task.key.id);
            if (null != cache) {
                textView.setIcon(cache);
                textView.setIconVisible(true);
            }
            iconCache.updateIconInBackground(task, (ittask) -> {
                Drawable newDrawable = ittask.icon.getConstantState().newDrawable();
                iconCacheMap.put(ittask.key.id, newDrawable);
                textView.setIcon(newDrawable);
                textView.setIconVisible(true);
            });
        } else {
            textView.setIcon(task.icon.getConstantState().newDrawable());
            textView.setIconVisible(true);
        }
    }
}
