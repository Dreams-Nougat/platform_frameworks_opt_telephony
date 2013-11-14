/*
 * Copyright (C) 2011-2012 The Android Open Source Project
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

package com.android.internal.telephony.uicc;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.Rlog;

import com.android.internal.telephony.CommandsInterface;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.RILConstants.SimCardID;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

/**
 * This class is responsible for keeping all knowledge about
 * Universal Integrated Circuit Card (UICC), also know as SIM's,
 * in the system. It is also used as API to get appropriate
 * applications to pass them to phone and service trackers.
 *
 * UiccController is created with the call to make() function.
 * UiccController is a singleton and make() must only be called once
 * and throws an exception if called multiple times.
 *
 * Once created UiccController registers with RIL for "on" and "unsol_sim_status_changed"
 * notifications. When such notification arrives UiccController will call
 * getIccCardStatus (GET_SIM_STATUS). Based on the response of GET_SIM_STATUS
 * request appropriate tree of uicc objects will be created.
 *
 * Following is class diagram for uicc classes:
 *
 *                       UiccController
 *                            #
 *                            |
 *                        UiccCard
 *                          #   #
 *                          |   ------------------
 *                    UiccCardApplication    CatService
 *                      #            #
 *                      |            |
 *                 IccRecords    IccFileHandler
 *                 ^ ^ ^           ^ ^ ^ ^ ^
 *    SIMRecords---- | |           | | | | ---SIMFileHandler
 *    RuimRecords----- |           | | | ----RuimFileHandler
 *    IsimUiccRecords---           | | -----UsimFileHandler
 *                                 | ------CsimFileHandler
 *                                 ----IsimFileHandler
 *
 * Legend: # stands for Composition
 *         ^ stands for Generalization
 *
 * See also {@link com.android.internal.telephony.IccCard}
 * and {@link com.android.internal.telephony.uicc.IccCardProxy}
 */
public class UiccController extends Handler {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "UiccController";

    public static final int APP_FAM_3GPP =  1;
    public static final int APP_FAM_3GPP2 = 2;
    public static final int APP_FAM_IMS   = 3;

    private static final int EVENT_ICC_STATUS_CHANGED = 1;
    private static final int EVENT_GET_ICC_STATUS_DONE = 2;
    private static final int EVENT_GET_ICC_STATUS = 3;
    private static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE = 4;

    private static final Object mLock = new Object();
    private static UiccController mInstance[]={ null, null };

    private Context mContext;
    private CommandsInterface mCi;
    private UiccCard mUiccCard;

    private RegistrantList mIccChangedRegistrants = new RegistrantList();

    private SimCardID mSimCardId;

    private boolean mPhoneOnMode = true;
    private IccCardStatus status = null;

    //The retry count to get Icc card status
    private int mRetryGetIccStatus = 0;

    public static UiccController make(Context c, CommandsInterface ci) {
        synchronized (mLock) {
            if (mInstance[SimCardID.ID_ZERO.toInt()] != null) {
                throw new RuntimeException("UiccController.make() should only be called once");
            }
            mInstance[SimCardID.ID_ZERO.toInt()] = new UiccController(c, ci);
            return mInstance[SimCardID.ID_ZERO.toInt()];
        }
    }

    public static UiccController make(Context c, CommandsInterface ci, SimCardID simCardId) {
        synchronized (mLock) {
            if (mInstance[simCardId.toInt()] != null) {
                throw new RuntimeException("UiccController.make() should only be called once");
            }
            mInstance[simCardId.toInt()] = new UiccController(c, ci, simCardId);
            return mInstance[simCardId.toInt()];
        }
    }

    public static UiccController getInstance() {
        synchronized (mLock) {
            if (mInstance[SimCardID.ID_ZERO.toInt()] == null) {
                throw new RuntimeException(
                        "UiccController.getInstance can't be called before make()");
            }
            return mInstance[SimCardID.ID_ZERO.toInt()];
        }
    }

    public static UiccController getInstance(SimCardID simCardId) {
        synchronized (mLock) {
            if (mInstance[simCardId.toInt()] == null) {
                throw new RuntimeException(
                        "UiccController.getInstance can't be called before make()");
            }
            return mInstance[simCardId.toInt()];
        }
    }

    public UiccCard getUiccCard() {
        synchronized (mLock) {
            return mUiccCard;
        }
    }

    // Easy to use API
    public UiccCardApplication getUiccCardApplication(int family) {
        synchronized (mLock) {
            if (mUiccCard != null) {
                return mUiccCard.getApplication(family);
            }
            return null;
        }
    }

    // Easy to use API
    public IccRecords getIccRecords(int family) {
        synchronized (mLock) {
            if (mUiccCard != null) {
                UiccCardApplication app = mUiccCard.getApplication(family);
                if (app != null) {
                    return app.getIccRecords();
                }
            }
            return null;
        }
    }

    // Easy to use API
    public IccFileHandler getIccFileHandler(int family) {
        synchronized (mLock) {
            if (mUiccCard != null) {
                UiccCardApplication app = mUiccCard.getApplication(family);
                if (app != null) {
                    return app.getIccFileHandler();
                }
            }
            return null;
        }
    }

    //Notifies when card status changes
    public void registerForIccChanged(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant (h, what, obj);
            mIccChangedRegistrants.add(r);
            //Notify registrant right after registering, so that it will get the latest ICC status,
            //otherwise which may not happen until there is an actual change in ICC status.
            r.notifyRegistrant();
        }
    }

    public void unregisterForIccChanged(Handler h) {
        synchronized (mLock) {
            mIccChangedRegistrants.remove(h);
        }
    }

    @Override
    public void handleMessage (Message msg) {
        synchronized (mLock) {
            switch (msg.what) {
                case EVENT_ICC_STATUS_CHANGED:
                    if (DBG) log("Received EVENT_ICC_STATUS_CHANGED, calling getIccCardStatus");
                    mCi.getIccCardStatus(obtainMessage(EVENT_GET_ICC_STATUS_DONE));
                    break;
                case EVENT_GET_ICC_STATUS: //Used to get SIM card status when the radio state is off
                    if (DBG) log("Received EVENT_GET_ICC_STATUS: getIccCardStatus()");
                    if ((null == mUiccCard || null == mUiccCard.getCardState())
                        && (mCi.getRadioState() == RadioState.RADIO_OFF)
                        && (mRetryGetIccStatus <= 2)) {
                        if (DBG) log("start to get SIM status");
                        mCi.getIccCardStatus(obtainMessage(EVENT_GET_ICC_STATUS_DONE));
                        mRetryGetIccStatus++;
                    }
                    break;
                case EVENT_GET_ICC_STATUS_DONE:
                    if (DBG) log("Received EVENT_GET_ICC_STATUS_DONE");
                    AsyncResult ar = (AsyncResult)msg.obj;
                    onGetIccCardStatusDone(ar);
                    break;
                // No matter radio statue is on, off or not available, we should update ICC status
                case EVENT_RADIO_OFF_OR_NOT_AVAILABLE:
                    if (DBG) log("Received EVENT_RADIO_OFF_OR_NOT_AVAILABLE");
                    sendEmptyMessageDelayed(EVENT_GET_ICC_STATUS, 3500);
                    break;
                default:
                    Rlog.e(LOG_TAG, " Unknown Event " + msg.what);
            }
        }
    }

    private UiccController(Context c, CommandsInterface ci) {
        this(c, ci, SimCardID.ID_ZERO);
    }

    private UiccController(Context c, CommandsInterface ci, SimCardID simCardId) {
        if (DBG) log("Creating UiccController, SIM card ID:" + simCardId.toInt());
        mContext = c;
        mCi = ci;
        mSimCardId = simCardId;
        mCi.registerForIccStatusChanged(this, EVENT_ICC_STATUS_CHANGED, null);
        // TODO remove this once modem correctly notifies the unsols
        mCi.registerForOn(this, EVENT_ICC_STATUS_CHANGED, null);
        mCi.registerForOffOrNotAvailable(this, EVENT_RADIO_OFF_OR_NOT_AVAILABLE, null);
    }

    private synchronized void onGetIccCardStatusDone(AsyncResult ar) {
        if (ar.exception != null) {
            Rlog.e(LOG_TAG,"Error getting ICC status. "
                    + "RIL_REQUEST_GET_ICC_STATUS should "
                    + "never return an error", ar.exception);
            return;
        }
        if (mSimCardId == SimCardID.ID_ONE) {
            mPhoneOnMode = (Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.PHONE2_ON, 1)!=0);
        } else {
            mPhoneOnMode = (Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.PHONE1_ON, 1)!=0);
        }

        status = (IccCardStatus)ar.result;
        // Try to re-get SIM card status when thr radio state is off
        if (DBG) log("onGetIccCardStatusDone(), status:" + status + ", RadioState:" + mCi.getRadioState() + ", mPhoneOnMode:" + mPhoneOnMode);
        if ((mPhoneOnMode == true)
            && (null == mUiccCard || null == mUiccCard.getCardState())
            && (mCi.getRadioState() == RadioState.RADIO_OFF)
            && (mRetryGetIccStatus <= 2)) {
            if (DBG) log("onGetIccCardStatusDone(), need to get SIM status");
            sendEmptyMessageDelayed(EVENT_GET_ICC_STATUS, 3500);//Re-get ICC status
            return; // BRCM - We should not move forward before we get the Icc card status or radio is ON
        }

        mRetryGetIccStatus = 0;

        if (DBG) log("onGetIccCardStatusDone(), after re-get function, status:" + status + ", RadioState:" + mCi.getRadioState());

        if (mUiccCard == null) {
            //Create new card
            mUiccCard = new UiccCard(mContext, mCi, status, mSimCardId);
        } else {
            //Update already existing card
            mUiccCard.update(mContext, mCi , status, mSimCardId);
        }

        if (DBG) log("Notifying IccChangedRegistrants");
        mIccChangedRegistrants.notifyRegistrants();
    }

    private void log(String string) {
        Rlog.d(LOG_TAG, "SimId : " + mSimCardId + ", " + string);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("UiccController: " + this);
        pw.println(" mContext=" + mContext);
        pw.println(" mInstance=" + mInstance);
        pw.println(" mCi=" + mCi);
        pw.println(" mUiccCard=" + mUiccCard);
        pw.println(" mIccChangedRegistrants: size=" + mIccChangedRegistrants.size());
        for (int i = 0; i < mIccChangedRegistrants.size(); i++) {
            pw.println("  mIccChangedRegistrants[" + i + "]="
                    + ((Registrant)mIccChangedRegistrants.get(i)).getHandler());
        }
        pw.println();
        pw.flush();
        if (mUiccCard != null) {
            mUiccCard.dump(fd, pw, args);
        }
    }
}
