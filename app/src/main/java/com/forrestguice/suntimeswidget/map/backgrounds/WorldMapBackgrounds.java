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
package com.forrestguice.suntimeswidget.map.backgrounds;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import com.forrestguice.annotation.NonNull;
import com.forrestguice.annotation.Nullable;
import com.forrestguice.suntimeswidget.BuildConfig;
import com.forrestguice.suntimeswidget.views.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * WorldMapBackgrounds
 */
public class WorldMapBackgrounds
{
    public static final String REQUIRED_PERMISSION = BuildConfig.SUNTIMES_PERMISSION_ROOT + ".permission.READ_CALCULATOR";

    public static final String CATEGORY_SUNTIMES_ADDON = "suntimes.SUNTIMES_ADDON";
    public static final String ACTION_SUNTIMES_ADDON_WORLDMAP_BACKGROUND = "suntimes.action.ADDON_WORLDMAP_BACKGROUND";
    public static final String KEY_BACKGROUND_PROVIDER = "WorldMapBackgroundProvider";

    /**
     * Get a list of available background providers. Each "world map background provider"
     * supplies a ContentProvider that implements WorldMapBackgroundContract.
     * @param context Context
     * @return a list of world map background provider uris
     */
    public static List<String> queryWorldMapBackgroundProviders(@NonNull Context context)
    {
        ArrayList<String> references = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        Intent packageQuery = new Intent(ACTION_SUNTIMES_ADDON_WORLDMAP_BACKGROUND);
        packageQuery.addCategory(CATEGORY_SUNTIMES_ADDON);
        List<ResolveInfo> packages = packageManager.queryIntentActivities(packageQuery, PackageManager.GET_META_DATA);
        Log.i("queryBackground", "Scanning for WorldMapBackgroundProvider references... found " + packages.size());

        for (ResolveInfo resolveInfo : packages)
        {
            if (resolveInfo != null && resolveInfo.activityInfo != null && resolveInfo.activityInfo.metaData != null)
            {
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(resolveInfo.activityInfo.packageName, PackageManager.GET_PERMISSIONS);
                    if (hasPermission(packageInfo))
                    {
                        String metaData = resolveInfo.activityInfo.metaData.getString(KEY_BACKGROUND_PROVIDER);
                        String[] values = (metaData != null) ? metaData.replace(" ","").split("\\|") : new String[0];
                        references.addAll(Arrays.asList(values));
                    } else {
                        Log.w("queryBackground", "Permission denied! " + packageInfo.packageName + " does not have required permissions.");
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("queryBackground", "Package not found! " + e);
                }
            }
        }
        return references;
    }

    /**
     * Retrieve a list of all available backgrounds from all background providers.
     * @param context Context
     * @return list of WorldMapBackgroundItem
     */
    public static List<WorldMapBackgroundItem> queryWorldMapBackgroundItems(Context context, String projection)
    {
        List<WorldMapBackgroundItem> items = new ArrayList<>();
        List<String> providers = WorldMapBackgrounds.queryWorldMapBackgroundProviders(context);
        for (String provider : providers) {
            items.addAll(WorldMapBackgrounds.queryWorldMapBackgroundItems(provider, context.getContentResolver(), projection));
        }
        return items;
    }

    /**
     * Retrieve the list of available backgrounds from a given background provider.
     * @param provider world map background provider uri (@see queryWorldMapBackgroundProviders)
     * @param resolver ContentResolver
     * @return list of WorldMapBackgroundItem
     */
    public static List<WorldMapBackgroundItem> queryWorldMapBackgroundItems(String provider, @Nullable ContentResolver resolver, String projection)
    {
        ArrayList<WorldMapBackgroundItem> items = new ArrayList<>();
        if (resolver != null && provider != null)
        {
            Uri uri = Uri.parse(provider + "/" + WorldMapBackgroundContract.QUERY_BACKGROUND_LIST + "/" + projection);
            Cursor cursor = resolver.query(uri, WorldMapBackgroundContract.QUERY_BACKGROUND_LIST_PROJECTION, null, null, null);
            if (cursor != null)
            {
                cursor.moveToFirst();
                while (!cursor.isAfterLast())
                {
                    int i_id = cursor.getColumnIndex(WorldMapBackgroundContract.COLUMN_BACKGROUND_ID);
                    int i_title = cursor.getColumnIndex(WorldMapBackgroundContract.COLUMN_BACKGROUND_TITLE);
                    int i_summary = cursor.getColumnIndex(WorldMapBackgroundContract.COLUMN_BACKGROUND_SUMMARY);
                    int i_mapproj = cursor.getColumnIndex(WorldMapBackgroundContract.COLUMN_BACKGROUND_PROJECTION);
                    int i_fileuri = cursor.getColumnIndex(WorldMapBackgroundContract.COLUMN_BACKGROUND_FILE);

                    String map_projection = (i_mapproj >= 0) ? cursor.getString(i_mapproj) : null;
                    if (map_projection == null) {
                        Log.w("queryBackground", "map projection is missing! skipping item returned from: " + provider);
                        continue;
                    }
                    String file_uri = (i_fileuri >= 0) ? cursor.getString(i_fileuri) : null;
                    if (file_uri == null) {
                        Log.w("queryBackground", "file uri is missing! skipping item returned from: " + provider);
                        continue;
                    }

                    WorldMapBackgroundItem item = new WorldMapBackgroundItem();
                    item.provider_uri = provider;
                    item.id = (i_id >= 0) ? cursor.getString(i_id) : null;
                    item.map_projection = map_projection;
                    item.file_uri = file_uri;

                    String titleValue = (i_title >= 0) ? cursor.getString(i_title) : null;
                    item.setTitle(titleValue != null ? titleValue : uri.getLastPathSegment());
                    item.summary = (i_summary >= 0) ? cursor.getString(i_summary) : null;

                    items.add(item);
                    cursor.moveToNext();
                }
                cursor.close();
            }
        }
        return items;
    }

    /**
     * hasPermission
     */
    public static boolean hasPermission(@NonNull PackageInfo packageInfo)
    {
        boolean hasPermission = false;
        if (packageInfo.requestedPermissions != null)
        {
            for (String permission : packageInfo.requestedPermissions) {
                if (permission != null && permission.equals(REQUIRED_PERMISSION)) {
                    hasPermission = true;
                    break;
                }
            }
        }
        return hasPermission;
    }

    /**
     * Populates a submenu with WorldMapBackgroundItems.
     * @param submenuItem MenuItem
     * @param backgroundItems List<WorldMapBackgroundItem>
     */
    public static void populateSubMenu(Context context, @Nullable MenuItem submenuItem, @NonNull List<WorldMapBackgroundItem> backgroundItems, @Nullable OnWorldMapBackgroundItemClick menuItemListener)
    {
        if (submenuItem != null)
        {
            SubMenu submenu = submenuItem.getSubMenu();
            if (submenu != null)
            {
                for (WorldMapBackgroundItem item : backgroundItems)
                {
                    MenuItem menuItem = submenu.add(Menu.NONE, Menu.NONE, Menu.NONE, item.getTitle());
                    menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
                    {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem)
                        {
                            if (menuItemListener != null) {
                                menuItemListener.onClick(item);
                            }
                            return true;
                        }
                    });
                }
            }
        }
    }

    public interface OnWorldMapBackgroundItemClick {
        void onClick(WorldMapBackgroundItem item);
    }

}
