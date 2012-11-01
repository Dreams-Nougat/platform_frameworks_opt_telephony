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
import android.util.Log;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCardStatus;
import com.android.internal.telephony.IccFileHandler;
import com.android.internal.telephony.IccRecords;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.UiccCard;
import com.android.internal.telephony.UiccCardApplication;

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
 * and {@link com.android.internal.telephony.IccCardProxy}
 */
public class UiccController extends Handler {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "RIL_UiccController";

    public static final int APP_FAM_3GPP =  1;
    public static final int APP_FAM_3GPP2 = 2;
    public static final int APP_FAM_IMS   = 3;

    private static final int EVENT_ICC_STATUS_CHANGED = 1;
    private static final int EVENT_GET_ICC_STATUS_DONE = 2;

    private static final Object mLock = new Object();
    private static UiccController mInstance;

    private Context mContext;
    private CommandsInterface[] mCi;
    private UiccCard[] mUiccCards = new UiccCard[MSimConstants.RIL_MAX_CARDS];

    private RegistrantList mIccChangedRegistrants = new RegistrantList();

    public static synchronized UiccController make(Context c, CommandsInterface[] ci) {
        synchronized (mLock) {
            if (mInstance != null) {
                throw new RuntimeException("UiccController.make() should only be called once");
            }
            mInstance = new UiccController(c, ci);
            return mInstance;
        }
    }

    public static UiccController make(Context c, CommandsInterface ci) {
        synchronized (mLock) {
            if (mInstance != null) {
                throw new RuntimeException("UiccController.make() should only be called once");
            }
            CommandsInterface[] arrayCi = new CommandsInterface[1];
            arrayCi[0] = ci;
            mInstance = new UiccController(c, arrayCi);
            return mInstance;
        }
    }

    public static UiccController getInstance() {
        synchronized (mLock) {
            if (mInstance == null) {
                throw new RuntimeException(
                        "UiccController.getInstance can't be called before make()");
            }
            return mInstance;
        }
    }

    public UiccCard getUiccCard() {
        return getUiccCard(MSimConstants.DEFAULT_CARD_INDEX);
    }

    public UiccCard getUiccCard(int slotId) {
        synchronized (mLock) {
            if (slotId >= 0 && slotId < mUiccCards.length) {
                return mUiccCards[slotId];
            }
            return null;
        }
    }

    public UiccCard[] getUiccCards() {
        // Return cloned array since we don't want to give out reference
        // to internal data structure.
        synchronized (mLock) {
            return mUiccCards.clone();
        }
    }

    // Easy to use API
    public UiccCardApplication getUiccCardApplication(int family) {
        return getUiccCardApplication(MSimConstants.DEFAULT_CARD_INDEX, family);
    }

    public UiccCardApplication getUiccCardApplication(int slotId, int family) {
        synchronized (mLock) {
            if (slotId >= 0 && slotId < mUiccCards.length) {
                UiccCard c = mUiccCards[slotId];
                if (c != null) {
                    return mUiccCards[slotId].getApplication(family);
                }
            }
            return null;
        }
    }

    // Easy to use API
    public IccRecords getIccRecords(int family) {
        return getIccRecords(MSimConstants.DEFAULT_CARD_INDEX, family);
    }

    public IccRecords getIccRecords(int slotId, int family) {
        synchronized (mLock) {
            UiccCardApplication app = getUiccCardApplication(slotId, family);
            if (app != null) {
                return app.getIccRecords();
            }
            return null;
        }
    }

    // Easy to use API
    public IccFileHandler getIccFileHandler(int family) {
        return getIccFileHandler(MSimConstants.DEFAULT_CARD_INDEX, family);
    }

    public IccFileHandler getIccFileHandler(int slotId, int family) {
        synchronized (mLock) {
            UiccCardApplication app = getUiccCardApplication(slotId, family);
            if (app != null) {
                return app.getIccFileHandler();
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
            Integer index = getCiIndex(msg);

            if (index < 0 || index >= mCi.length) {
                Log.e(LOG_TAG, "Invalid index : " + index + " received with event " + msg.what);
                return;
            }

            switch (msg.what) {
                case EVENT_ICC_STATUS_CHANGED:
                    if (DBG) log("Received EVENT_ICC_STATUS_CHANGED, calling getIccCardStatus"
                            + "on index " + index);
                    mCi[index].getIccCardStatus(obtainMessage(EVENT_GET_ICC_STATUS_DONE, index));
                    break;
                case EVENT_GET_ICC_STATUS_DONE:
                    if (DBG) log("Received EVENT_GET_ICC_STATUS_DONE on index " + index);
                    AsyncResult ar = (AsyncResult)msg.obj;
                    onGetIccCardStatusDone(ar, index);
                    break;
                default:
                    Log.e(LOG_TAG, " Unknown Event " + msg.what);
            }
        }
    }

    private UiccController(Context c, CommandsInterface []ci) {
        if (DBG) log("Creating UiccController");
        mContext = c;
        mCi = ci;
        for (int i = 0; i < mCi.length; i++) {
            Integer index = new Integer(i);
            mCi[i].registerForIccStatusChanged(this, EVENT_ICC_STATUS_CHANGED, index);
            // TODO remove this once modem correctly notifies the unsols
            mCi[i].registerForOn(this, EVENT_ICC_STATUS_CHANGED, index);
        }
    }


    private Integer getCiIndex(Message msg) {
        AsyncResult ar;
        Integer index = new Integer(MSimConstants.DEFAULT_CARD_INDEX);

        /*
         * The events can be come in two ways. By explicitly sending it using
         * sendMessage, in this case the user object passed is msg.obj and from
         * the CommandsInterface, in this case the user object is msg.obj.userObj
         */
        if (msg != null) {
            if (msg.obj != null && msg.obj instanceof Integer) {
                index = (Integer)msg.obj;
            } else if(msg.obj != null && msg.obj instanceof AsyncResult) {
                ar = (AsyncResult)msg.obj;
                if (ar.userObj != null && ar.userObj instanceof Integer) {
                    index = (Integer)ar.userObj;
                }
            }
        }
        return index;
    }


    private synchronized void onGetIccCardStatusDone(AsyncResult ar, Integer index) {
        if (ar.exception != null) {
            Log.e(LOG_TAG,"Error getting ICC status. "
                    + "RIL_REQUEST_GET_ICC_STATUS should "
                    + "never return an error", ar.exception);
            return;
        }

        IccCardStatus status = (IccCardStatus)ar.result;

        if (mUiccCards[index] == null) {
            //Create new card
            mUiccCards[index] = new UiccCard(mContext, mCi[index], status, index);
        } else {
            //Update already existing card
            mUiccCards[index].update(mContext, mCi[index] , status, index);
        }

        if (DBG) log("Notifying IccChangedRegistrants");
        mIccChangedRegistrants.notifyRegistrants(new AsyncResult(null, index, null));
    }

    private void log(String string) {
        Log.d(LOG_TAG, string);
    }
}
