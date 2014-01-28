/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.android.internal.telephony.dataconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncResult;
import android.telephony.ServiceState;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.util.AsyncChannel;
import com.android.internal.telephony.PhoneProxyManager;

import android.provider.Settings;
import android.os.SystemProperties;
import com.android.internal.telephony.TelephonyProperties;

import android.util.Log;
import java.util.HashSet;
import java.util.Iterator;
import com.android.internal.telephony.DefaultPhoneNotifier;

public class DcSwitchManager {
    private static final String LOG_TAG = "PHONE";
    private static final boolean DBG = true;

    private static final int EVENT_SIM1_DETACH = 1;
    private static final int EVENT_SIM2_DETACH = 2;
    private static final int EVENT_SIM3_DETACH = 3;
    private static final int EVENT_SIM4_DETACH = 4;
    private static final int EVENT_SIM1_RADIO_OFF = 5;
    private static final int EVENT_SIM2_RADIO_OFF = 6;
    private static final int EVENT_SIM3_RADIO_OFF = 7;
    private static final int EVENT_SIM4_RADIO_OFF = 8;

    private static final int SIM_NONE = -1;

    private static DcSwitchManager mInstance;

    private Phone mActivePhone;
    private int mSimNum;
    private boolean[] mServicePowerOffFlag;
    private Phone[] mPhones;
    private DcSwitchState[] mDcSwitchState;
    private DcSwitchAsyncChannel[] mDcSwitchAsyncChannel;
    private Handler[] mDcSwitchStateHandler;

    protected HashSet<String> mApnTypes = new HashSet<String>();

    private BroadcastReceiver mDataStateReceiver;
    protected Context mContext;

    private int mCurrentDataSim = SIM_NONE;
    private int mRequestedDataSim = SIM_NONE;

    private Handler mRspHander = new Handler() {
        public void handleMessage(Message msg){
            AsyncResult ar;
            switch(msg.what) {
                case EVENT_SIM1_DETACH:
                case EVENT_SIM2_DETACH:
                case EVENT_SIM3_DETACH:
                case EVENT_SIM4_DETACH:
                    logd("EVENT_SIM" + msg.what + "_DETACH: mRequestedDataSim=" + mRequestedDataSim);
                    mCurrentDataSim = SIM_NONE;
                    if (mRequestedDataSim != SIM_NONE) {
                        mCurrentDataSim = mRequestedDataSim;
                        mRequestedDataSim = SIM_NONE;

                        Iterator<String> itrType = mApnTypes.iterator();
                        while (itrType.hasNext()) {
                            mDcSwitchAsyncChannel[mCurrentDataSim].connectSync(itrType.next());
                        }
                        mApnTypes.clear();
                    }
                break;

                case EVENT_SIM1_RADIO_OFF:
                case EVENT_SIM2_RADIO_OFF:
                case EVENT_SIM3_RADIO_OFF:
                case EVENT_SIM4_RADIO_OFF:
                    logd("EVENT_SIM" + (msg.what - EVENT_SIM1_RADIO_OFF + 1) + "_RADIO_OFF.");
                    mServicePowerOffFlag[msg.what - EVENT_SIM1_RADIO_OFF] = true;
                break;

                default:
                break;
            }
        }
    };

    private DefaultPhoneNotifier.IDataStateChangedCallback mDataStateChangedCallback =
            new DefaultPhoneNotifier.IDataStateChangedCallback()
    {
        public void onDataStateChanged(String state, String reason,
                String apnName, String apnType, boolean unavailable, int simId)
        {
            logd("[DataStateChanged]:" + "state=" + state + ",reason=" + reason
                      + ",apnName=" + apnName + ",apnType=" + apnType + ",from simId=" + simId);
            mDcSwitchState[simId].notifyDataConnection(state, reason, apnName, apnType, unavailable, simId);
        }
    };

    public DefaultPhoneNotifier.IDataStateChangedCallback getDataStateChangedCallback() {
        return mDataStateChangedCallback;
    }

    private class DataStateReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            synchronized(this) {
                if (intent.getAction().equals(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED)) {
                    ServiceState ss = ServiceState.newFromBundle(intent.getExtras());
                    int simId = intent.getIntExtra(PhoneConstants.SIM_ID_KEY, PhoneConstants.SIM_ID_1);
                    boolean prevPowerOff = mServicePowerOffFlag[simId];
                    if (ss != null) {
                        int state = ss.getState();
                        switch (state) {
                            case ServiceState.STATE_POWER_OFF:
                                mServicePowerOffFlag[simId] = true;
                                logd("Recv STATE_POWER_OFF Intent from simId=" + simId);
                                break;
                            case ServiceState.STATE_IN_SERVICE:
                                mServicePowerOffFlag[simId] = false;
                                logd("Recv STATE_IN_SERVICE Intent from simId=" + simId);
                                break;
                            case ServiceState.STATE_OUT_OF_SERVICE:
                                logd("Recv STATE_OUT_OF_SERVICE Intent from simId=" + simId);
                                if (mServicePowerOffFlag[simId]) {
                                    mServicePowerOffFlag[simId] = false;
                                }
                                break;
                            case ServiceState.STATE_EMERGENCY_ONLY:
                                logd("Recv STATE_EMERGENCY_ONLY Intent from simId=" + simId);
                                break;
                            default:
                                logd("Recv SERVICE_STATE_CHANGED invalid state");
                                break;
                        }

                        if (prevPowerOff && mServicePowerOffFlag[simId] == false &&
                                mCurrentDataSim == SIM_NONE &&
                                simId == getDataConnectionFromSetting()) {
                            logd("Current SIM is none and default SIM is " + simId + ", then enableApnType()");
                            enableApnType(PhoneConstants.APN_TYPE_DEFAULT, simId);
                        }
                    }
                }
            }
        }
    }

    public static DcSwitchManager getInstance() {
        if (mInstance == null)
            mInstance = new DcSwitchManager();
        return mInstance;
    }

    private DcSwitchManager() {
        mSimNum = SystemProperties.getInt(TelephonyProperties.PROPERTY_SIM_COUNT, 2);
        mServicePowerOffFlag = new boolean[mSimNum];
        mPhones = PhoneProxyManager.getPhoneProxys();
        mDcSwitchState = new DcSwitchState[mSimNum];
        mDcSwitchAsyncChannel = new DcSwitchAsyncChannel[mSimNum];
        mDcSwitchStateHandler = new Handler[mSimNum];

        mActivePhone = mPhones[0];

        for (int i=0; i<mSimNum; ++i) {
            int dcId = i + 1;
            mServicePowerOffFlag[i] = true;
            mDcSwitchState[i] = new DcSwitchState(PhoneProxyManager.getPhoneProxy(i), "DcSwitchState-" + dcId, dcId);
            mDcSwitchState[i].start();
            mDcSwitchAsyncChannel[i] = new DcSwitchAsyncChannel(mDcSwitchState[i], dcId);
            mDcSwitchStateHandler[i] = new Handler();

            int status = mDcSwitchAsyncChannel[i].fullyConnectSync(mPhones[i].getContext(), mDcSwitchStateHandler[i], mDcSwitchState[i].getHandler());
            if (status == AsyncChannel.STATUS_SUCCESSFUL) {
                logd("Connect success: " + i);
            } else {
                loge("Could not connect to " + i);
            }

            mDcSwitchState[i].registerForIdle(mRspHander, EVENT_SIM1_DETACH + i, null);

            //M: Register for radio state change
            PhoneBase phoneBase = (PhoneBase)((PhoneProxy)mPhones[i]).getActivePhone();
            phoneBase.mCi.registerForOffOrNotAvailable(mRspHander, EVENT_SIM1_RADIO_OFF + i, null);
        }

        mContext = mActivePhone.getContext();

        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_DATA_CONNECTION_FAILED);
        filter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);

        mDataStateReceiver = new DataStateReceiver();
        Intent intent = mContext.registerReceiver(mDataStateReceiver, filter);

    }

    /*public void resetAsRadioOff(int mode) {
        for (int i=0; i<mSimNum; ++i) {
            if ((mode & (GeminiNetworkSubUtil.MODE_SIM1_ONLY << i)) == 0 ||
                    mode == GeminiNetworkSubUtil.MODE_POWER_OFF) {

                if (i == mCurrentDataSim) {
                    logd("resetAsRadioOff: reset SIM " + i + " data connection [mode=" + mode + ", allowDetach=" + mDcSwitchState[i].isAllowDetach() + "].");
                    //for ALPS00771109
                    //when power off, we should not send AT+EGTYPE=0 or AT+CGATT=0
                    //so we skip power off case here
                    if (mode == GeminiNetworkSubUtil.MODE_POWER_OFF) {
                        mDcSwitchState[i].transitToIdleState();
                    } else if ((mode & (GeminiNetworkSubUtil.MODE_SIM1_ONLY << i)) == 0) {
                        if (mDcSwitchState[i].isAllowDetach()) {
                            PhoneProxyManager.getPhoneProxy(i).setGprsConnType(GeminiNetworkSubUtil.CONN_TYPE_WHEN_NEEDED);
                        }
                        mDcSwitchState[i].transitToIdleState();
                    } else {
                        if (mDcSwitchState[i].isAllowDetach()) {
                            PhoneProxyManager.getPhoneProxy(i).setGprsConnType(GeminiNetworkSubUtil.CONN_TYPE_WHEN_NEEDED);
                        }
                        mDcSwitchState[i].cleanupAllConnection();
                    }
                }
                if (i == mRequestedDataSim) {
                    logd("resetAsRadioOff: reset request SIM " + i + ".");
                    mRequestedDataSim = SIM_NONE;
                }
            }
        }
    }*/

    private IccCardConstants.State getIccCardState(int simId) {
        return PhoneProxyManager.getPhoneProxy(simId).getIccCard().getState();
    }

    private void updateActivePhoneProxy() {
        int defaultSimId = getDataConnectionFromSetting();
        if (isValidSimId(defaultSimId) &&
                 getIccCardState(defaultSimId) == IccCardConstants.State.READY) {
            mActivePhone = mPhones[defaultSimId];
            logd("UAPP_C1: active Phone =" + defaultSimId);
            return;
        }

        for (int i=0; i<mSimNum; ++i) {
            if (getIccCardState(i) == IccCardConstants.State.READY) {
                mActivePhone = mPhones[i];
                logd("UAPP_C3: active Phone =" + i);
                return;
            }
        }

        //Use SIM1 as default SIM
        mActivePhone = mPhones[PhoneConstants.SIM_ID_1];
    }

    /**
     * Enable PDP interface by apn type and sim id
     *
     * @param type enable pdp interface by apn type, such as PhoneConstants.APN_TYPE_MMS, etc.
     * @param simId Indicate which sim(slot) to query
     * @return PhoneConstants.APN_REQUEST_STARTED: action is already started
     * PhoneConstants.APN_ALREADY_ACTIVE: interface has already active
     * PhoneConstants.APN_TYPE_NOT_AVAILABLE: invalid APN type
     * PhoneConstants.APN_REQUEST_FAILED: request failed
     * PhoneConstants.APN_REQUEST_FAILED_DUE_TO_RADIO_OFF: readio turn off
     * @see #disableApnType()
     */
    public synchronized int enableApnType(String type, int simId) {
        if (simId == SIM_NONE || !isValidSimId(simId)) {
            logw("enableApnType(): with SIM_NONE or Invalid SIM ID");
            return PhoneConstants.APN_REQUEST_FAILED;
        }

        logd("enableApnType():type=" + type + ",simId=" + simId + ",powerOff=" + mServicePowerOffFlag[simId]);

        for (int peerSimId=PhoneConstants.SIM_ID_1; peerSimId<mSimNum; peerSimId++) {
            // check peer SIM has non default APN activated as receiving non default APN request.
            if (simId == peerSimId) {
                continue;
            }

            if (!PhoneConstants.APN_TYPE_DEFAULT.equals(type)) {
                String[] activeApnTypes = mPhones[peerSimId].getActiveApnTypes();
                if (activeApnTypes != null && activeApnTypes.length != 0) {
                    for (int i=0; i<activeApnTypes.length; i++) {
                        if (!PhoneConstants.APN_TYPE_DEFAULT.equals(activeApnTypes[i]) &&
                                mPhones[peerSimId].getDataConnectionState(activeApnTypes[i]) != PhoneConstants.DataState.DISCONNECTED) {
                            logd("enableApnType():Peer SIM still have non default active APN type: activeApnTypes[" + i + "]=" + activeApnTypes[i]);
                            return PhoneConstants.APN_REQUEST_FAILED;
                        }
                    }
                }
            }
        }

        logd("enableApnType(): CurrentDataSim=" +
                mCurrentDataSim + ", RequestedDataSim=" + mRequestedDataSim);

        if (simId == mCurrentDataSim &&
               !mDcSwitchAsyncChannel[mCurrentDataSim].isIdleOrDeactingSync()) {
           mRequestedDataSim = SIM_NONE;
           logd("enableApnType(): currentDataSim equals request SIM ID.");
           return mDcSwitchAsyncChannel[simId].connectSync(type);
        } else {
            // Only can switch data when mCurrentDataSim is SIM_NONE, it is set to SIM_NONE only as receiving EVENT_SIMX_DETACH
            if (mCurrentDataSim == SIM_NONE) {
                mCurrentDataSim = simId;
                mRequestedDataSim = SIM_NONE;
                logd("enableApnType(): current SIM is NONE or IDLE, currentDataSim=" + mCurrentDataSim);
                return mDcSwitchAsyncChannel[simId].connectSync(type);
            } else {
                logd("enableApnType(): current SIM:" + mCurrentDataSim + " is active.");
                if (simId != mRequestedDataSim) {
                    mApnTypes.clear();
                }
                mApnTypes.add(type);
                mRequestedDataSim = simId;
                mDcSwitchState[mCurrentDataSim].cleanupAllConnection();
            }
        }
        return PhoneConstants.APN_REQUEST_STARTED;
    }

    /**
     * disable PDP interface by apn type and sim id
     *
     * @param type enable pdp interface by apn type, such as PhoneConstants.APN_TYPE_MMS, etc.
     * @param simId Indicate which sim(slot) to query
     * @return PhoneConstants.APN_REQUEST_STARTED: action is already started
     * PhoneConstants.APN_ALREADY_ACTIVE: interface has already active
     * PhoneConstants.APN_TYPE_NOT_AVAILABLE: invalid APN type
     * PhoneConstants.APN_REQUEST_FAILED: request failed
     * PhoneConstants.APN_REQUEST_FAILED_DUE_TO_RADIO_OFF: readio turn off
     * @see #enableApnTypeGemini()
     */
    public synchronized int disableApnType(String type, int simId) {
        if (simId == SIM_NONE || !isValidSimId(simId)) {
            logw("disableApnType(): with SIM_NONE or Invalid SIM ID");
            return PhoneConstants.APN_REQUEST_FAILED;
        }
        logd("disableApnType():type=" + type + ",simId=" + simId + ",powerOff=" + mServicePowerOffFlag[simId]);
        return mDcSwitchAsyncChannel[simId].disconnectSync(type);
    }

    public boolean isDataConnectivityPossible(String type, int simId) {
        if (simId == SIM_NONE || !isValidSimId(simId)) {
            logw("isDataConnectivityPossible(): with SIM_NONE or Invalid SIM ID");
            return false;
        } else {
            return mPhones[simId].isDataConnectivityPossible(type);
        }
    }

    public boolean isIdleOrDeacting(int simId) {
        if (mDcSwitchAsyncChannel[simId].isIdleOrDeactingSync()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isValidSimId(int simid) {
        return simid >= PhoneConstants.SIM_ID_1 && simid <= mSimNum;
    }

    private boolean isValidApnType(String apnType) {
         if (apnType.equals(PhoneConstants.APN_TYPE_DEFAULT)
             || apnType.equals(PhoneConstants.APN_TYPE_MMS)
             || apnType.equals(PhoneConstants.APN_TYPE_SUPL)
             || apnType.equals(PhoneConstants.APN_TYPE_DUN)
             || apnType.equals(PhoneConstants.APN_TYPE_HIPRI)
             || apnType.equals(PhoneConstants.APN_TYPE_FOTA)
             || apnType.equals(PhoneConstants.APN_TYPE_IMS)
             || apnType.equals(PhoneConstants.APN_TYPE_CBS))
        {
            return true;
        } else {
            return false;
        }
    }

    private int getDataConnectionFromSetting() {
        int dataSimId = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.MOBILE_DATA, -1);
        logd("Default Data Setting value=" + dataSimId);
        return dataSimId;
    }

    private static void logv(String s) {
        Log.v(LOG_TAG, "[DcSwitchManager] " + s);
    }

    private static void logd(String s) {
        Log.d(LOG_TAG, "[DcSwitchManager] " + s);
    }

    private static void logw(String s) {
        Log.w(LOG_TAG, "[DcSwitchManager] " + s);
    }

    private static void loge(String s) {
        Log.e(LOG_TAG, "[DcSwitchManager] " + s);
    }
}