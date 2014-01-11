/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.internal.telephony;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.ApnSettings;

import java.util.ArrayList;
import java.util.List;

public class ApnUtils{

    private static int APN_INDEX = 0;
    private static int MMSC_INDEX = 1;
    private static int MMSPORT_INDEX = 2;
    private static int MMSPROXY_INDEX = 3;
    private static int TYPE_INDEX = 4;

    static String[] SELECTIONS = new String[] {Telephony.Carriers.APN,
            Telephony.Carriers.MMSC, Telephony.Carriers.MMSPORT,
            Telephony.Carriers.MMSPROXY, Telephony.Carriers.TYPE};
    /**
     * Returns the ApnSettings for specific mcc and mnc
     * @return a List of ApnSettings specified by mcc and mcn
     */
    public static List<ApnSettings> getApns(Context context, String mcc, String mnc) {
        String where = Telephony.Carriers.MCC + "=" + "\"" + mcc + "\"" + " AND " +
                Telephony.Carriers.MNC + "=" + "\"" + mnc + "\"";
        return getApns(context, where);
    }

    /**
     * Returns the ApnSettings for specific type of usage
     * @return a List of ApnSettings specified by type
     */
    public static List<ApnSettings> getApnsForType(Context context, String type) {
        String where = Telephony.Carriers.TYPE + "=" + "\"" + type + "\"";
        return getApns(context, where);
    }

    private static List<ApnSettings> getApns(Context context, String where){
        int i = 0;
        Cursor cursor = context.getContentResolver().query(
                Telephony.Carriers.CONTENT_URI, SELECTIONS, where, null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            cursor.moveToFirst();
            ArrayList<ApnSettings> result = new ArrayList<ApnSettings> ();
            while (!cursor.isAfterLast()) {
                result.add(new ApnSettings(cursor.getString(APN_INDEX),
                        cursor.getString(MMSC_INDEX), cursor.getString(MMSPORT_INDEX),
                        cursor.getString(MMSPROXY_INDEX), cursor.getString(TYPE_INDEX)));
                cursor.moveToNext();
            }
            cursor.close();
            return result;
        }
        return null;
    }
}
