package com.android.internal.telephony;

import static android.Manifest.permission.READ_PHONE_STATE;
import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SimInfoManager;
import android.telephony.SimInfoManager.SimInfoRecord;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.PhoneProxyManager;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;

import java.util.List;

/**
 *@hide
 */
public class SimInfoUpdater extends Handler {
    private static final String LOG_TAG = "PHONE";
    private static final int sSimNum = SystemProperties.getInt(TelephonyProperties.PROPERTY_SIM_COUNT, 1);
    private static final int EVENT_OFFSET = 8;
    private static final int EVENT_QUERY_ICCID_DONE = 1;
    private static final String ICCID_STRING_FOR_NO_SIM = "";

    public static final int SIM_NOT_CHANGE = 0;
    public static final int SIM_CHANGED = -1;
    public static final int SIM_NEW = -2;
    public static final int SIM_REPOSITION = -3;
    public static final int SIM_NOT_INSERT = -99;

    private static PhoneProxyManager sPhoneProxyMgr = null;
    private static Phone[] sPhone = null;
    private static Context sContext = null;
    private static CommandsInterface[] sCi = null;
    private static IccFileHandler[] sFh = null;
    private static String sIccId[] = null;
    private static int[] sInsertSimState = null;
    private static TelephonyManager sTelephonyMgr = null;
    private static boolean needUpdate = true;

    public SimInfoUpdater() {
        logd("Constructor invoked");
        sPhoneProxyMgr = PhoneFactory.getPhoneProxyManager();
        sPhone = new Phone[sSimNum];
        sCi = new CommandsInterface[sSimNum];
        sFh = new IccFileHandler[sSimNum];
        sIccId = new String[sSimNum];
        for (int i = 0; i < sSimNum; i++) {
            sPhone[i] = ((PhoneProxy)(sPhoneProxyMgr.getPhoneProxy(i))).getActivePhone();
            logd("sPhone[" + i + "]:" + sPhone[i]);
            sCi[i] = ((PhoneBase)sPhone[i]).mCi;
            sFh[i] = null;
            sIccId[i] = null;
        }
        sContext = ((PhoneBase)sPhone[0]).getContext();
        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        sContext.registerReceiver(sReceiver, intentFilter);
        /**
         *  int[] sInsertSimState maintains all slots' SIM inserted status currently, 
         *  it may contain 4 kinds of values:
         *    SIM_NOT_INSERT : no SIM inserted in slot i now
         *    SIM_CHANGED    : a valid SIM insert in slot i and is different SIM from last time
         *                     it will later become SIM_NEW or SIM_REPOSITION during update procedure
         *    SIM_NOT_CHANGE : a valid SIM insert in slot i and is the same SIM as last time
         *    SIM_NEW        : a valid SIM insert in slot i and is a new SIM
         *    SIM_REPOSITION : a valid SIM insert in slot i and is inserted in different slot last time
         *    positive integer #: index to distinguish SIM cards with the same IccId
         */
        sInsertSimState = new int[sSimNum];
    }

    private static int encodeEventId(int event, int simId) {
        return event << (simId * EVENT_OFFSET);
    }

    private final BroadcastReceiver sReceiver = new  BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            logd("[BroadcastReceiver]+");
            String action = intent.getAction();
            int simId;
            logd("Action: " + action );
            if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                String simStatus = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                simId = intent.getIntExtra(PhoneConstants.SIM_ID_KEY, 0);
                logd("simId: " + simId + " simStatus: " + simStatus);
                if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(simStatus)
                        || IccCardConstants.INTENT_VALUE_ICC_LOCKED.equals(simStatus)) {
                    queryIccId(simId);
                } else if (IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(simStatus)) {
                    queryIccId(simId);
                    if (sTelephonyMgr == null) {
                        sTelephonyMgr = TelephonyManager.from(sContext);
                    }
                    setDisplayNameForNewSim(sTelephonyMgr.getSimOperatorName(simId), simId);
                } else if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simStatus)) {
                    sFh[simId] = null;
                    sIccId[simId] = ICCID_STRING_FOR_NO_SIM;
                    needUpdate = true;
                    if (isAllIccIdQueryDone() && needUpdate) {
                        updateSimInfoByIccId();
                    }
                }
            }
            logd("[BroadcastReceiver]-");
        }
    };

    private boolean isAllIccIdQueryDone() {
        for (int i = 0; i < sSimNum; i++) {
            if (sIccId[i] == null) {
                return false;
            }
        }
        logd("All IccId query complete");

        return true;
    }

    public static void setDisplayNameForNewSim(String newSimName, int simId) {
        long newNameSource = SimInfoManager.SIM_SOURCE;
        if (newSimName == null) {
            newNameSource = SimInfoManager.DEFAULT_SOURCE;
        }
        SimInfoRecord simInfo = SimInfoManager.getSimInfoBySimId(sContext, simId);
        if (simInfo != null) {
            // overwrite SIM display name if it is not assigned by user
            int oldNameSource = simInfo.mNameSource;
            String oldSimName = simInfo.mDisplayName;
            logd("[setDisplayNameForNewSim] mSimInfoIdx: " + simInfo.mSimInfoIdx + " oldSimName: " + oldSimName 
                    + " oldNameSource = " + oldNameSource + " newSimName: " + newSimName + " newNameSource = " + newNameSource);
            if (oldSimName == null || 
                (oldNameSource == SimInfoManager.DEFAULT_SOURCE && newSimName != null) ||
                (oldNameSource == SimInfoManager.SIM_SOURCE && newSimName != null && !newSimName.equals(oldSimName))) {
                SimInfoManager.setDisplayName(sContext, newSimName, simInfo.mSimInfoIdx, newNameSource);
            }
        }
    }

    public void handleMessage(Message msg) {
        AsyncResult ar = (AsyncResult)msg.obj;
        int msgNum = msg.what;
        int simId; 
        for (simId = PhoneConstants.SIM_ID_1; simId <= PhoneConstants.SIM_ID_4; simId++) {
            int pivot = 1 << (simId * EVENT_OFFSET);
            if (msgNum >= pivot) {
                continue;
            } else {
                break;
            }
        }
        simId--;
        int event = msgNum >> (simId * EVENT_OFFSET);
        switch (event) {
            case EVENT_QUERY_ICCID_DONE:
                logd("handleMessage : <EVENT_QUERY_ICCID_DONE> SIM" + (simId + 1));
                if (ar.exception == null) {
                    if (ar.result != null) {
                        byte[] data = (byte[])ar.result;
                        sIccId[simId] = IccUtils.bcdToString(data, 0, data.length);
                    } else {
                        sIccId[simId] = null;
                    }
                } else {
                    sIccId[simId] = null;
                    logd("Query IccId fail: " + ar.exception);
                }
                logd("sIccId[" + simId + "]" + sIccId[simId]);
                if (isAllIccIdQueryDone() && needUpdate) {
                    updateSimInfoByIccId();
                }
                break;
            default:
                logd("Unknown msg:" + msg.what);
        }
    }

    private void queryIccId(int simId) {
        if (sFh[simId] == null) {
            logd("Getting IccFileHandler");
            sFh[simId] = ((PhoneBase)sPhone[simId]).getIccFileHandler();
        }
        if (sFh[simId] != null) {
            if (sIccId[simId] == null) {
                logd("Querying IccId");
                sFh[simId].loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(encodeEventId(EVENT_QUERY_ICCID_DONE, simId)));
            }
        } else {
            sIccId[simId] = ICCID_STRING_FOR_NO_SIM;
            logd("sFh[" + simId + "] is null, SIM not inserted");
        }
    }

    synchronized public void updateSimInfoByIccId() {
        logd("[updateSimInfoByIccId]+ Start");
        needUpdate = false;
        for (int i = 0; i < sSimNum; i++) {
            sInsertSimState[i] = SIM_NOT_CHANGE;
        }

        int insertedSimCount = sSimNum;
        for (int i = 0; i < sSimNum; i++) {
            if ("".equals(sIccId[i])) {
                insertedSimCount--;
                sInsertSimState[i] = SIM_NOT_INSERT;
            }
        }
        logd("insertedSimCount = " + insertedSimCount);

        int index = 0;
        for (int i = 0; i < sSimNum; i++) {
            if (sInsertSimState[i] == SIM_NOT_INSERT) {
                continue;
            }
            index = 2;
            for (int j = i + 1; j < sSimNum; j++) {
                if (sInsertSimState[j] == SIM_NOT_CHANGE && sIccId[i].equals(sIccId[j])) {
                    sInsertSimState[i] = 1;
                    sInsertSimState[j] = index;
                    index++;
                }
            }
        }

        ContentResolver contentResolver = sContext.getContentResolver();
        String[] oldIccId = new String[sSimNum];
        for (int i = 0; i < sSimNum; i++) {
            SimInfoRecord oldSimInfo = SimInfoManager.getSimInfoBySimId(sContext, i);
            if (oldSimInfo != null) {
                oldIccId[i] = oldSimInfo.mIccId;
                logd("oldIccId[" + i + "] = " + oldIccId[i] + " oldSimInfoIdx:" + oldSimInfo.mSimInfoIdx); 
                if (sInsertSimState[i] == SIM_NOT_CHANGE && !sIccId[i].equals(oldIccId[i])) {
                    sInsertSimState[i] = SIM_CHANGED;
                }
                if (sInsertSimState[i] != SIM_NOT_CHANGE) {
                    ContentValues value = new ContentValues(1);
                    value.put(SimInfoManager.SIM_ID, SimInfoManager.SIM_NOT_INSERTED);
                    contentResolver.update(SimInfoManager.CONTENT_URI, value,
                                                SimInfoManager._ID + "=" + Long.toString(oldSimInfo.mSimInfoIdx), null);
                    logd("Reset slot" + i + " to -1"); 
                }
            } else {
                if (sInsertSimState[i] == SIM_NOT_CHANGE) {
                    // no SIM inserted last time, but there is one SIM inserted now
                    sInsertSimState[i] = SIM_CHANGED;
                }
                oldIccId[i] = "";
                logd("No SIM in slot " + i + " last time"); 
            }
        }

        //check if the inserted SIM is new SIM
        for (int i = 0; i < sSimNum; i++) {
            if (sInsertSimState[i] == SIM_NOT_INSERT) {
                logd("No SIM inserted in slot " + i + " this time");
            } else {
                logd("sIccId[" + i + "] : " + sIccId[i] + ", oldIccId[" + i + "] : " + oldIccId[i]);
                if (sInsertSimState[i] > 0) {
                    //some special SIMs may have the same IccIds, add suffix to distinguish them
                    SimInfoManager.addSimInfoRecord(sContext, sIccId[i] + Integer.toString(sInsertSimState[i]), i); 
                    logd("SIM" + (i + 1) + " has invalid IccId");
                } else if (sInsertSimState[i] == SIM_CHANGED) {
                    SimInfoManager.addSimInfoRecord(sContext, sIccId[i], i); 
                }
            }
        }
        
        for (int i = 0; i < sSimNum; i++) {
            if (sInsertSimState[i] == SIM_CHANGED) {
                sInsertSimState[i] = SIM_REPOSITION;
            }
        }

        for (int i = 0; i < sSimNum; i++) {
            logd("sInsertSimState[" + i + "] = " + sInsertSimState[i]);
        }

        Intent intent = new Intent(TelephonyIntents.ACTION_SIMINFO_UPDATED);
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE, UserHandle.USER_ALL);
        logd("[updateSimInfoByIccId]- SimInfo update complete");
    }

    public void dispose() {
        logd("[dispose]");
        sContext.unregisterReceiver(sReceiver);
    }

    private static void logd(String message) {
        Rlog.d(LOG_TAG, "[SimInfoUpdater]" + message);
    }
}

