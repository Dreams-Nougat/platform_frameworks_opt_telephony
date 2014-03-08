/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.Manifest;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.telephony.Rlog;
import android.util.Log;
import android.net.Uri;
import android.database.Cursor;
import android.content.Intent;
import android.provider.BaseColumns;
import android.content.ContentResolver;
import android.content.ContentValues;

import com.android.internal.telephony.ISub;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import android.telephony.SubInfoRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
/**
 * SubscriptionController to provide an inter-process communication to
 * access Sms in Icc.
 */
public class SubscriptionController extends ISub.Stub {
    static final String LOG_TAG = "SUB";
    static final boolean DBG = true;

    protected final Object mLock = new Object();
    protected boolean mSuccess;

    /** The singleton instance. */
    private static SubscriptionController sInstance = null;
    protected static Phone mPhone;
    protected static Context mContext;

    public static final Uri CONTENT_URI =
            Uri.parse("content://telephony/siminfo");

    public static final int DEFAULT_INT_VALUE = -100;

    public static final String DEFAULT_STRING_VALUE = "N/A";

    public static final int EXTRA_VALUE_NEW_SIM = 1;
    public static final int EXTRA_VALUE_REMOVE_SIM = 2;
    public static final int EXTRA_VALUE_REPOSITION_SIM = 3;
    public static final int EXTRA_VALUE_NOCHANGE = 4;

    public static final String INTENT_KEY_DETECT_STATUS = "simDetectStatus";
    public static final String INTENT_KEY_SIM_COUNT = "simCount";
    public static final String INTENT_KEY_NEW_SIM_SLOT = "newSIMSlot";
    public static final String INTENT_KEY_NEW_SIM_STATUS = "newSIMStatus";

    /**
     * The ICC ID of a SIM.
     * <P>Type: TEXT (String)</P>
     */
    public static final String ICC_ID = "icc_id";

    /**
     * <P>Type: INTEGER (int)</P>
     */
    public static final String SIM_ID = "sim_id";

    public static final int SIM_NOT_INSERTED = -1;

    /**
     * The display name of a SIM.
     * <P>Type: TEXT (String)</P>
     */
    public static final String DISPLAY_NAME = "display_name";

    public static final int DEFAULT_NAME_RES = com.android.internal.R.string.unknownName;

    /**
     * The display name source of a SIM.
     * <P>Type: INT (int)</P>
     */
    public static final String NAME_SOURCE = "name_source";

    public static final int DEFAULT_SOURCE = 0;

    public static final int SIM_SOURCE = 1;

    public static final int USER_INPUT = 2;

    /**
     * The color of a SIM.
     * <P>Type: INTEGER (int)</P>
     */
    public static final String COLOR = "color";

    public static final int COLOR_1 = 0;

    public static final int COLOR_2 = 1;

    public static final int COLOR_3 = 2;

    public static final int COLOR_4 = 3;

    public static final int COLOR_DEFAULT = COLOR_1;

    /**
     * The phone number of a SIM.
     * <P>Type: TEXT (String)</P>
     */
    public static final String NUMBER = "number";

    /**
     * The number display format of a SIM.
     * <P>Type: INTEGER (int)</P>
     */
    public static final String DISPLAY_NUMBER_FORMAT = "display_number_format";

    public static final int DISPALY_NUMBER_NONE = 0;

    public static final int DISPLAY_NUMBER_FIRST = 1;

    public static final int DISPLAY_NUMBER_LAST = 2;

    public static final int DISLPAY_NUMBER_DEFAULT = DISPLAY_NUMBER_FIRST;

    /**
     * Permission for data roaming of a SIM.
     * <P>Type: INTEGER (int)</P>
     */
    public static final String DATA_ROAMING = "data_roaming";

    public static final int DATA_ROAMING_ENABLE = 1;

    public static final int DATA_ROAMING_DISABLE = 0;

    public static final int DATA_ROAMING_DEFAULT = DATA_ROAMING_DISABLE;

    private static final int RES_TYPE_BACKGROUND_DARK = 0;

    private static final int RES_TYPE_BACKGROUND_LIGHT = 1;

    private static final int[] sSimBackgroundDarkRes = setSimResource(RES_TYPE_BACKGROUND_DARK);

    private static final int[] sSimBackgroundLightRes = setSimResource(RES_TYPE_BACKGROUND_LIGHT);

    private static HashMap<Integer, Long> mSimInfo = new HashMap<Integer, Long>();

    public static SubscriptionController init(Phone phone) {
        synchronized (SubscriptionController.class) {
            if (sInstance == null) {
                sInstance = new SubscriptionController(phone);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }


    private SubscriptionController(Phone phone) {
        mPhone = phone;
        mContext = phone.getContext();

        if(ServiceManager.getService("isub") == null) {
                ServiceManager.addService("isub", this);
        }

        logd("SubscriptionController init");
    }

    /**
     * Broadcast when subinfo settings has chanded
     * @SubId The unique SubInfoRecord index in database
     * @param columnName The column that is updated
     * @param intContent The updated integer value
     * @param stringContent The updated string value
     */
     private void broadcastSimInfoContentChanged(long subId,
            String columnName, int intContent, String stringContent) {
        Intent intent = new Intent(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);
        intent.putExtra(BaseColumns._ID, subId);
        intent.putExtra(TelephonyIntents.EXTRA_COLUMN_NAME, columnName);
        intent.putExtra(TelephonyIntents.EXTRA_INT_CONTENT, intContent);
        intent.putExtra(TelephonyIntents.EXTRA_STRING_CONTENT, stringContent);
        if (intContent != DEFAULT_INT_VALUE) {
            logd("SubInfoRecord" + subId + " changed, " + columnName + " -> " +  intContent);
        } else {
            logd("SubInfoRecord" + subId + " changed, " + columnName + " -> " +  stringContent);
        }
        mContext.sendBroadcast(intent);
    }

    /**
     * New SubInfoRecord instance and fill in detail info
     * @param cursor
     * @return the query result of desired SubInfoRecord
     */
    private SubInfoRecord getSubInfoRecord(Cursor cursor) {
            SubInfoRecord info = new SubInfoRecord();
            info.mSubId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            info.mIccId = cursor.getString(cursor.getColumnIndexOrThrow(ICC_ID));
            info.mSimId = cursor.getInt(cursor.getColumnIndexOrThrow(SIM_ID));
            info.mDisplayName = cursor.getString(cursor.getColumnIndexOrThrow(DISPLAY_NAME));
            info.mNameSource = cursor.getInt(cursor.getColumnIndexOrThrow(NAME_SOURCE));
            info.mColor = cursor.getInt(cursor.getColumnIndexOrThrow(COLOR));
            info.mNumber = cursor.getString(cursor.getColumnIndexOrThrow(NUMBER));
            info.mDispalyNumberFormat = cursor.getInt(cursor.getColumnIndexOrThrow(DISPLAY_NUMBER_FORMAT));
            info.mDataRoaming = cursor.getInt(cursor.getColumnIndexOrThrow(DATA_ROAMING));

            int size = sSimBackgroundDarkRes.length;
            if (info.mColor >= 0 && info.mColor < size) {
                info.mSimIconRes[RES_TYPE_BACKGROUND_DARK] = sSimBackgroundDarkRes[info.mColor];
                info.mSimIconRes[RES_TYPE_BACKGROUND_LIGHT] = sSimBackgroundLightRes[info.mColor];
            }
            logd("[getSubInfoRecord] SubId:" + info.mSubId + " iccid:" + info.mIccId + " simId:" + info.mSimId
                    + " displayName:" + info.mDisplayName + " color:" + info.mColor);

            return info;
    }

    /**
     * Query SubInfoRecord(s) from subinfo database
     * @param context Context provided by caller
     * @param selection A filter declaring which rows to return
     * @param queryKey query key content
     * @return Array list of queried result from database
     */
     private List<SubInfoRecord> getSubInfo(String selection, Object queryKey) {
        logd("selection:" + selection + " " + queryKey);
        String[] selectionArgs = null;
        if (queryKey != null) {
            selectionArgs = new String[] {queryKey.toString()};
        }
        ArrayList<SubInfoRecord> subList = null;
        Cursor cursor = mContext.getContentResolver().query(CONTENT_URI,
                null, selection, selectionArgs, null);
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    SubInfoRecord subInfo = getSubInfoRecord(cursor);
                    if (subInfo != null)
                    {
                        if (subList == null)
                        {
                            subList = new ArrayList<SubInfoRecord>();
                        }
                        subList.add(subInfo);
                }
                }
            } else {
                logd("Query fail");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return subList;
    }
    
    /**
     * Get the SubInfoRecord according to an index
     * @param context Context provided by caller
     * @param subId The unique SubInfoRecord index in database
     * @return SubInfoRecord, maybe null
     */
    @Override
    public SubInfoRecord getSubInfoUsingSubId(long subId) {
        logd("[getSubInfoUsingSubIdx]+ subId:" + subId);
        if (subId <= 0) {
            logd("[getSubInfoUsingSubIdx]- subId <= 0");
            return null;
        }
        Cursor cursor = mContext.getContentResolver().query(CONTENT_URI,
                null, BaseColumns._ID + "=?", new String[] {Long.toString(subId)}, null);
        try {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    logd("[getSubInfoUsingSubIdx]- Info detail:");
                    return getSubInfoRecord(cursor);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logd("[getSubInfoUsingSubIdx]- null info return");

        return null;
    }

    /**
     * Get the SubInfoRecord according to an IccId
     * @param context Context provided by caller
     * @param iccId the IccId of SIM card
     * @return SubInfoRecord, maybe null
     */
    @Override
    public List<SubInfoRecord> getSubInfoUsingIccId(String iccId) {
        logd("[getSubInfoUsingIccId]+ iccId:" + iccId);
        if (iccId == null) {
            logd("[getSubInfoUsingIccId]- null iccid");
            return null;
        }
        Cursor cursor = mContext.getContentResolver().query(CONTENT_URI,
                null, ICC_ID + "=?", new String[] {iccId}, null);
        ArrayList<SubInfoRecord> subList = null;
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    SubInfoRecord subInfo = getSubInfoRecord(cursor);
                    if (subInfo != null)
                    {
                        if (subList == null)
                        {
                            subList = new ArrayList<SubInfoRecord>();
                        }
                        subList.add(subInfo);
                }
                }
            } else {
                logd("Query fail");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return subList;
    }

    /**
     * Get the SubInfoRecord according to simId
     * @param context Context provided by caller
     * @param simId the slot which the SIM is inserted
     * @return SubInfoRecord, maybe null
     */
    @Override
    public List<SubInfoRecord> getSubInfoUsingSimId(int simId) {
        logd("[getSubInfoUsingSimId]+ simId:" + simId);
        if (simId < 0) {
            logd("[getSubInfoUsingSimId]- simId < 0");
            return null;
        }
        Cursor cursor = mContext.getContentResolver().query(CONTENT_URI,
                null, SIM_ID + "=?", new String[] {String.valueOf(simId)}, null);
        ArrayList<SubInfoRecord> subList = null;
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    SubInfoRecord subInfo = getSubInfoRecord(cursor);
                    if (subInfo != null)
                    {
                        if (subList == null)
                        {
                            subList = new ArrayList<SubInfoRecord>();
                        }
                        subList.add(subInfo);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logd("[getSubInfoUsingSimId]- null info return");

        return subList;
    }

    /**
     * Get all the SubInfoRecord(s) in subinfo database
     * @param context Context provided by caller
     * @return Array list of all SubInfoRecords in database, include thsoe that were inserted before
     */
    @Override
    public List<SubInfoRecord> getAllSubInfoList() {
        logd("[getAllSubInfoList]+");
        List<SubInfoRecord> subList = null;
        subList = getSubInfo(null, null);
        if (subList != null) {
            logd("[getAllSubInfoList]- " + subList.size() + " infos return");
        } else {
            logd("[getAllSubInfoList]- no info return");
        }

        return subList;
    }

    /**
     * Get the SubInfoRecord(s) of the currently inserted SIM(s)
     * @param context Context provided by caller
     * @return Array list of currently inserted SubInfoRecord(s)
     */
    @Override
    public List<SubInfoRecord> getActivatedSubInfoList() {
        logd("[getActivatedSubInfoList]+");
        List<SubInfoRecord> subList = null;
        subList = getSubInfo(SIM_ID + "!=" + SIM_NOT_INSERTED, null);
        if (subList != null) {
            logd("[getActivatedSubInfoList]- " + subList.size() + " infos return");
        } else {
            logd("[getActivatedSubInfoList]- no info return");
        }

        return subList;
    }

    /**
     * Get the SUB count of all SUB(s) in subinfo database
     * @param context Context provided by caller
     * @return all SIM count in database, include what was inserted before
     */
    @Override
    public int getAllSubInfoCount() {
        logd("[getAllSubInfoCount]+");
        Cursor cursor = mContext.getContentResolver().query(CONTENT_URI,
                null, null, null, null);
        try {
            if (cursor != null) {
                int count = cursor.getCount();
                logd("[getAllSubInfoCount]- " + count + " SUB(s) in DB");
                return count;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logd("[getAllSubInfoCount]- no SUB in DB");

        return 0;
    }

    /**
     * Add a new SubInfoRecord to subinfo database if needed
     * @param context Context provided by caller
     * @param iccId the IccId of the SIM card
     * @param simId the slot which the SIM is inserted
     * @return the URL of the newly created row or the updated row
     */
    @Override
    public int addSubInfoRecord(String iccId, int simId) {
        logd("[addSubInfoRecord]+ iccId:" + iccId + " simId:" + simId);
        if (iccId == null) {
            logd("[addSubInfoRecord]- null iccId");
        }
        Uri uri = null;
        String nameToSet;
        nameToSet = "SUB 0"+Integer.toString(simId+1);
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(CONTENT_URI, new String[] {BaseColumns._ID, SIM_ID, NAME_SOURCE},
                ICC_ID + "=?", new String[] {iccId}, null);
        try {
            if (cursor == null || !cursor.moveToFirst()) {
                ContentValues value = new ContentValues();
                value.put(ICC_ID, iccId);
                // default SIM color differs between slots
                value.put(COLOR, simId);
                value.put(SIM_ID, simId);
                value.put(DISPLAY_NAME, nameToSet);
                uri = resolver.insert(CONTENT_URI, value);
                logd("[addSubInfoRecord]- New record created");
            } else {
                long subId = cursor.getLong(0);
                int oldSimInfoId = cursor.getInt(1);
                int nameSource = cursor.getInt(2);
                ContentValues value = new ContentValues();

                if (simId != oldSimInfoId) {
                    value.put(SIM_ID, simId);
                }

                if (nameSource != USER_INPUT) {
                    value.put(DISPLAY_NAME, nameToSet);
                }

                resolver.update(CONTENT_URI, value,
                                    BaseColumns._ID + "=" + Long.toString(subId), null);

                logd("[addSubInfoRecord]- Record already exist");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        cursor = resolver.query(CONTENT_URI,
                null, SIM_ID + "=?", new String[] {String.valueOf(simId)}, null);

        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long subId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
                    mSimInfo.put(simId,subId);
                    logd("[addSubInfoRecord]- hashmap("+simId+","+subId+")");
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        int size = mSimInfo.size();
        logd("[addSubInfoRecord]- info size="+size);

        return 1;
    }

    /**
     * Set SIM color by simInfo index
     * @param context Context provided by caller
     * @param color the color of the SIM
     * @param subId the unique SubInfoRecord index in database
     * @return the number of records updated
     */
    @Override
    public int setColor(int color, long subId) {
        logd("[setColor]+ color:" + color + " subId:" + subId);
        int size = sSimBackgroundDarkRes.length;
        if (subId <= 0 || color < 0 || color >= size) {
            logd("[setColor]- fail");
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put(COLOR, color);
        logd("[setColor]- color:" + color + " set");

        int result = mContext.getContentResolver().update(CONTENT_URI, value,
                                                    BaseColumns._ID + "=" + Long.toString(subId), null);
        broadcastSimInfoContentChanged(subId, COLOR, color, DEFAULT_STRING_VALUE);

        return result;
    }

    /**
     * Set display name by simInfo index
     * @param context Context provided by caller
     * @param displayName the display name of SIM card
     * @param subId the unique SubInfoRecord index in database
     * @return the number of records updated
     */
    @Override
    public int setDisplayName(String displayName, long subId) {
        return setDisplayNameUsingSrc(displayName, subId, -1);
    }

    /**
     * Set display name by simInfo index with name source
     * @param context Context provided by caller
     * @param displayName the display name of SIM card
     * @param subId the unique SubInfoRecord index in database
     * @param nameSource, 0: DEFAULT_SOURCE, 1: SIM_SOURCE, 2: USER_INPUT
     * @return the number of records updated
     */
    @Override
    public int setDisplayNameUsingSrc(String displayName, long subId, long nameSource) {
        logd("[setDisplayName]+  displayName:" + displayName + " subId:" + subId + " nameSource:" + nameSource);
        if (subId <= 0) {
            logd("[setDisplayName]- fail");
            return -1;
        }
        String nameToSet;
        if (displayName == null) {
            nameToSet = mContext.getString(DEFAULT_NAME_RES);
        } else {
            nameToSet = displayName;
        }
        ContentValues value = new ContentValues(1);
        value.put(DISPLAY_NAME, nameToSet);
        if (nameSource >= DEFAULT_SOURCE) {
            logd("Set nameSource=" + nameSource);
            value.put(NAME_SOURCE, nameSource);
        }
        logd("[setDisplayName]- mDisplayName:" + nameToSet + " set");

        int result = mContext.getContentResolver().update(CONTENT_URI, value,
                                                  BaseColumns._ID + "=" + Long.toString(subId), null);
        broadcastSimInfoContentChanged(subId, DISPLAY_NAME, DEFAULT_INT_VALUE, nameToSet);

        return result;
    }

    /**
     * Set phone number by subId
     * @param context Context provided by caller
     * @param number the phone number of the SIM
     * @param subId the unique SubInfoRecord index in database
     * @return the number of records updated
     */
    @Override
    public int setDispalyNumber(String number, long subId) {
        logd("[setDispalyNumber]+ number:" + number + " subId:" + subId);
        if (number == null || subId <= 0) {
            logd("[setDispalyNumber]- fail");
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put(NUMBER, number);
        logd("[setDispalyNumber]- number:" + number + " set");

        int result = mContext.getContentResolver().update(CONTENT_URI, value,
                                                  BaseColumns._ID + "=" + Long.toString(subId), null);
        broadcastSimInfoContentChanged(subId, NUMBER, DEFAULT_INT_VALUE, number);

        return result;
    }

    /**
     * Set number display format. 0: none, 1: the first four digits, 2: the last four digits
     * @param context Context provided by caller
     * @param format the display format of phone number
     * @param subId the unique SubInfoRecord index in database
     * @return the number of records updated
     */
    @Override
    public int setDispalyNumberFormat(int format, long subId) {
        logd("[setDispalyNumberFormat]+ format:" + format + " subId:" + subId);
        if (format < 0 || subId <= 0) {
            logd("[setDispalyNumberFormat]- fail, return -1");
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put(DISPLAY_NUMBER_FORMAT, format);
        logd("[setDispalyNumberFormat]- format:" + format + " set");

        int result = mContext.getContentResolver().update(CONTENT_URI, value,
                                                  BaseColumns._ID + "=" + Long.toString(subId), null);
        broadcastSimInfoContentChanged(subId, DISPLAY_NUMBER_FORMAT, format, DEFAULT_STRING_VALUE);

        return result;
    }

    /**
     * Set data roaming by simInfo index
     * @param context Context provided by caller
     * @param roaming 0:Don't allow data when roaming, 1:Allow data when roaming
     * @param subId the unique SubInfoRecord index in database
     * @return the number of records updated
     */
    @Override
    public int setDataRoaming(int roaming, long subId) {
        logd("[setDataRoaming]+ roaming:" + roaming + " subId:" + subId);
        if (roaming < 0 || subId <= 0) {
            logd("[setDataRoaming]- fail");
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put(DATA_ROAMING, roaming);
        logd("[setDataRoaming]- roaming:" + roaming + " set");

        int result = mContext.getContentResolver().update(CONTENT_URI, value,
                                                  BaseColumns._ID + "=" + Long.toString(subId), null);
        broadcastSimInfoContentChanged(subId, DATA_ROAMING, roaming, DEFAULT_STRING_VALUE);

        return result;
    }

    public int getSimId(long subId) {
        logd("[getSimId]+ subId:" + subId);
        if (subId <= 0) {
            logd("[getSimId]- subId <= 0");
            return SIM_NOT_INSERTED;
        }

        int size = mSimInfo.size();
        logd("[getSimId]- info size="+size);

        if (size == 0)
        {
            return SIM_NOT_INSERTED;
        }

        for (Entry<Integer, Long> entry: mSimInfo.entrySet()) {
            int sim = entry.getKey();
            long sub = entry.getValue();
            logd("[getSimId]- entry, sim ="+sim+", sub="+ sub);

            if (subId == sub)
            {
                logd("[getSimId]- return ="+sim);
                return sim;
            }
        }

        logd("[getSimId]- return fail");
        return (int)(subId-1);

    }

    public long[] getSubId(int simId) {
        logd("[getSubId]+ simId:" + simId);

        if (simId < 0) {
            logd("[getSubId]- simId < 0");
            return null;
        }

        int size = mSimInfo.size();
        logd("[getSubId]- info size="+size);
        
        if (size == 0)
        {
            return null;
        }

        long[] subId = new long[] {(long)simId+1, (long)simId+1};
        int subIdx = 0;

        for (Entry<Integer, Long> entry: mSimInfo.entrySet()) {
            int sim = entry.getKey();
            long sub = entry.getValue();

            logd("[getSubId]- entry, sim ="+sim+", sub="+ sub);
            if (simId == sim)
            {
                subId[subIdx] = sub;
                logd("[getSubId]- subId["+subIdx+"] = "+subId[subIdx]);
                subIdx++;
            }
        }

        logd("[getSubId]-, subId = "+subId[0]);
        return subId;
    }

    public int getPhoneId(long subId) {
        logd("[getPhoneId]+ subId:" + subId);
        if (subId <= 0) {
            logd("[getPhoneId]- subId <= 0");
            return SIM_NOT_INSERTED;
        }

        int size = mSimInfo.size();
        logd("[getPhoneId]- info size="+size);

        if (size == 0)
        {
            return SIM_NOT_INSERTED;
        }

        for (Entry<Integer, Long> entry: mSimInfo.entrySet()) {
            int sim = entry.getKey();
            long sub = entry.getValue();
            logd("[getPhoneId]- entry, sim ="+sim+", sub="+ sub);

            if (subId == sub)
            {
                logd("[getPhoneId]- return ="+sim);
                return sim;
            }
        }

        logd("[getPhoneId]- return fail");
        return (int)(subId-1);

    }

    public int clearSubInfo()
    {
        logd("[clearSubInfo]+");

        int size = mSimInfo.size();
        logd("[getSubId]- info size="+size);

        if (size == 0)
        {
            return 0;
        }

        mSimInfo.clear();
        logd("[clearSubInfo]-");
        return 0;

    }

    private static int[] setSimResource(int type) {
        int[] simResource = null;

        switch (type) {
            case RES_TYPE_BACKGROUND_DARK:
                simResource = new int[] {
                    com.android.internal.R.drawable.sim_dark_blue,
                    com.android.internal.R.drawable.sim_dark_orange,
                    com.android.internal.R.drawable.sim_dark_green,
                    com.android.internal.R.drawable.sim_dark_purple
                };
                break;
            case RES_TYPE_BACKGROUND_LIGHT:
                simResource = new int[] {
                    com.android.internal.R.drawable.sim_light_blue,
                    com.android.internal.R.drawable.sim_light_orange,
                    com.android.internal.R.drawable.sim_light_green,
                    com.android.internal.R.drawable.sim_light_purple
                };
                break;
        }

        return simResource;
    }

    private void logd(String msg) {
        Rlog.d(LOG_TAG, "[SubController]" + msg);
    }

    public long getDefaultSubId()
    {
        logd("[getDefaultSubId] return 1");
        return 1;
    }
}
