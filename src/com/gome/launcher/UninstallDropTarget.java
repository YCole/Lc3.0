package com.gome.launcher;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.gome.drawbitmaplib.BitmapInfo;
import com.gome.launcher.compat.UserHandleCompat;
import com.gome.launcher.util.Thunk;

public class UninstallDropTarget extends ButtonDropTarget {

    public UninstallDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UninstallDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Get the hover color
        mHoverColor = getResources().getColor(R.color.uninstall_target_hover_tint);

        //modify by liuning for gome dropTargetButton style on 2017/7/18 start
        setDrawable(R.drawable.gome_uninstall_launcher);
        //modify by liuning for gome dropTargetButton style on 2017/7/18 end
    }

    @Override
    protected boolean supportsDrop(DragSource source, Object info) {
        //modify by liiunig for gome dropTargetButton style on 2017/7/18 start
        //return supportsDrop(getContext(), info);
        if (info instanceof ShortcutInfo && ((ShortcutInfo) info).itemType == LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION) {
            if (supportsDrop(mLauncher, info) || Utilities.isSystemApp(mLauncher, (((ShortcutInfo) info).getIntent()))) {
                setText(getResources().getString(R.string.delete_target_uninstall_label));
                return true;
            }
        } else if (info instanceof FolderInfo) {
            setText(getResources().getString(R.string.delete_target_label));
            return true;
        }
        return false;
        //modify by liiunig for gome dropTargetButton style on 2017/7/18 end
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static boolean supportsDrop(Context context, Object info) {
        //delete by liiunig for gome dropTargetButton style on 2017/7/18 start
//        if (Utilities.ATLEAST_JB_MR2) {
//            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
//            Bundle restrictions = userManager.getUserRestrictions();
//            if (restrictions.getBoolean(UserManager.DISALLOW_APPS_CONTROL, false)
//                    || restrictions.getBoolean(UserManager.DISALLOW_UNINSTALL_APPS, false)) {
//                return false;
//            }
//        }
        //delete by liiunig for gome dropTargetButton style on 2017/7/18 end

        Pair<ComponentName, Integer> componentInfo = getAppInfoFlags(info);
        return componentInfo != null && (componentInfo.second & AppInfo.DOWNLOADED_FLAG) != 0;
    }

    /**
     * added by liuning for draging system app add folder into the uninstallDropTarget on 2017/7/28
     * @param d
     * @return
     */
    @Override
    public boolean acceptDrop(DragObject d) {
        if (supportsDrop(getContext(), d.dragInfo)) {
            return true;
        } else if (d.dragSource instanceof UninstallSource) {
            ((UninstallSource) d.dragSource).onRemoveSystemAppOrFolder();
            d.dragView.setColor(0);
            resetHoverColor();
            if (!(d.dragSource instanceof Folder)) {
                mLauncher.getWorkspace().onDrop(d);
            }
            if (d.dragInfo instanceof FolderInfo) {
                Toast.makeText(getContext(), R.string.folder_can_not_delete, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), R.string.system_app_can_not_uninstall, Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    /**
     * @return the component name and flags if {@param info} is an AppInfo or an app shortcut.
     */
    private static Pair<ComponentName, Integer> getAppInfoFlags(Object item) {
        if (item instanceof AppInfo) {
            AppInfo info = (AppInfo) item;
            return Pair.create(info.componentName, info.flags);
        } else if (item instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) item;
            ComponentName component = info.getTargetComponent();
            if (info.itemType == LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION
                    && component != null) {
                return Pair.create(component, info.flags);
            }
        }
        return null;
    }

    @Override
    public void onDrop(DragObject d) {
        // Differ item deletion
        //modify by liiunig for gome dropTargetButton style on 2017/7/18 start
        if (d.dragSource instanceof UninstallSource) {
            if (supportsDrop(getContext(), d.dragInfo)) {
                ((UninstallSource) d.dragSource).deferCompleteDropAfterUninstallActivity();
            }
        }
        //modify by liiunig for gome dropTargetButton style on 2017/7/18 end
        super.onDrop(d);
    }

    @Override
    void completeDrop(final DragObject d) {
        //modify by liiunig for gome dropTargetButton style on 2017/7/18 start
        if (supportsDrop(getContext(), d.dragInfo)) {
            final Pair<ComponentName, Integer> componentInfo = getAppInfoFlags(d.dragInfo);
            final UserHandleCompat user = ((ItemInfo) d.dragInfo).user;
            if (startUninstallActivity(mLauncher, d.dragInfo)) {

                final Runnable checkIfUninstallWasSuccess = new Runnable() {
                    @Override
                    public void run() {
                        String packageName = componentInfo.first.getPackageName();
                        boolean uninstallSuccessful = !AllAppsList.packageHasActivities(
                                getContext(), packageName, user);
                        sendUninstallResult(d.dragSource, uninstallSuccessful);
                    }
                };
                mLauncher.addOnResumeCallback(checkIfUninstallWasSuccess);
            } else {
                sendUninstallResult(d.dragSource, false);
            }
            //add by huanghaihao for snap error on drop on 2017/8/3 start
            mLauncher.getWorkspace().showPageScaleAinimation(true, true);
            //add by huanghaihao for snap error on drop on 2017/8/3 end
        }
        //modify by liiunig for gome dropTargetButton style on 2017/7/18 end
    }

    public static boolean startUninstallActivity(Launcher launcher, Object info) {
        final Pair<ComponentName, Integer> componentInfo = getAppInfoFlags(info);
        final UserHandleCompat user = ((ItemInfo) info).user;
        return launcher.startApplicationUninstallActivity(
                componentInfo.first, componentInfo.second, user);
    }

    @Thunk
    void sendUninstallResult(DragSource target, boolean result) {
        if (target instanceof UninstallSource) {
            ((UninstallSource) target).onUninstallActivityReturned(result);
        }
    }

    /**
     * Interface defining an object that can provide uninstallable drag objects.
     */
    public static interface UninstallSource {

        /**
         * A pending uninstall operation was complete.
         * @param result true if uninstall was successful, false otherwise.
         */
        void onUninstallActivityReturned(boolean result);

        /**
         * Indicates that an uninstall request are made and the actual result may come
         * after some time.
         */
        void deferCompleteDropAfterUninstallActivity();

        /**
         * add by liuning for gome dropTargetButton style
         * Indicates that drop an system app or folder into the uninstall target
         */
        void onRemoveSystemAppOrFolder();
    }
}
