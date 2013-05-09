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

package com.android.internal.telephony.test;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.test.util.TelephonyTestUtil;
import android.util.Log;

import com.android.internal.telephony.IccSmsInterfaceManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.SmsRawData;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.cdma.CDMALTEPhone;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.uicc.UiccController;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Creates and holds {@link HandlerThread} on which simulated Telephony FW runs.
 */
public class TelephonyTestExecutor implements Handler.Callback {

    private static final String LOG_TAG = "TelephonyTestExecutor";

    private static final int EVENT_INIT_CDMA_PHONE = 1;
    private static final int EVENT_INIT_GSM_PHONE = 2;
    private static final int EVENT_INIT_CDMA_LTE_PHONE = 3;
    private static final int EVENT_DISPOSE_PHONE = 4;

    private static final String FIELD_INSTANCE = "mInstance";

    public enum PhoneMode {
        /** for creating FW as CDMA mode. */
        CDMA(EVENT_INIT_CDMA_PHONE),

        /** for creating FW as GSM mode. */
        GSM(EVENT_INIT_GSM_PHONE),

        /** for creating FW as CDMA LTE mode. */
        CDMALTE(EVENT_INIT_CDMA_LTE_PHONE);

        int mCode;

        PhoneMode(int code) {
            mCode = code;
        }

        int getCode() {
            return mCode;
        }
    }

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private MockCommands mMockCommands;
    private MockPhoneNotifier mMockNotifier;

    private Phone mPhone;
    private WrappedIccSmsInterfaceManager mIccSmsInterfaceManager;

    private Context mContext;

    private CountDownLatch mLatch;

    public TelephonyTestExecutor(Context context, PhoneMode mode) {
        mContext = context;

        // initiate HandlerThread
        mHandlerThread = new HandlerThread("TelephonyFWTest");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), this);

        // create mock
        mMockCommands = new MockCommands(context, getPhoneType(mode));
        mMockNotifier = new MockPhoneNotifier();

        mLatch = new CountDownLatch(1);

        mHandler.sendEmptyMessage(mode.getCode());

        // wait until phone is created.
        try {
            mLatch.await();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "InterruptedException occurred at mLatch.await()" + e);
        }
    }

    public Phone getPhone() {
        return mPhone;
    }

    public WrappedIccSmsInterfaceManager getIccSmsInterfaceManager() {
        return mIccSmsInterfaceManager;
    }

    public RILSimulator getRILSimulator() {
        return mMockCommands;
    }

    public NotificationRegister getNotificationRegister() {
        return mMockNotifier;
    }

    public Looper getLooper() {
        return mHandlerThread.getLooper();
    }

    public void dispose() throws Exception {

        mLatch = new CountDownLatch(1);
        mHandler.sendEmptyMessage(EVENT_DISPOSE_PHONE);

        // wait until phone is disposed.
        mLatch.await();

        mMockNotifier.dispose();
        mHandlerThread.quit();

        mIccSmsInterfaceManager = null;
        mHandlerThread = null;
        mHandler = null;
        mMockNotifier = null;
        mMockCommands = null;
        mPhone = null;
        TelephonyTestUtil.setStaticField(UiccController.class, FIELD_INSTANCE, null);
    }

    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_INIT_CDMA_PHONE:
                // crate CDMAPhone
                clearUiccControllerInstance();
                UiccController.make(mContext, mMockCommands);
                mPhone = new CDMAPhone(mContext, mMockCommands, mMockNotifier);
                break;
            case EVENT_INIT_GSM_PHONE:
                // crate GSMPhone
                clearUiccControllerInstance();
                UiccController.make(mContext, mMockCommands);
                mPhone = new GSMPhone(mContext, mMockCommands, mMockNotifier);
                break;
            case EVENT_INIT_CDMA_LTE_PHONE:
                // crate CDMALTEPhone
                clearUiccControllerInstance();
                UiccController.make(mContext, mMockCommands);
                mPhone = new CDMALTEPhone(mContext, mMockCommands, mMockNotifier);
                break;
            case EVENT_DISPOSE_PHONE:
                // dispose phone
                ((PhoneBase)mPhone).dispose();
                break;
        }

        if (msg.what == EVENT_INIT_CDMA_PHONE || msg.what == EVENT_INIT_GSM_PHONE
                || msg.what == EVENT_INIT_CDMA_LTE_PHONE) {
            mIccSmsInterfaceManager = new WrappedIccSmsInterfaceManager((PhoneBase)this.mPhone);
        }
        mLatch.countDown();

        return true;
    }

    private int getPhoneType(PhoneMode mode) {
        int type;
        switch (mode) {
            case GSM:
                type = RILConstants.GSM_PHONE;
                break;
            case CDMA:
                type = RILConstants.CDMA_PHONE;
                break;
            case CDMALTE:
                type = RILConstants.CDMA_PHONE;
                break;
            default:
                throw new AssertionError(mode);
        }
        return type;
    }

    private void clearUiccControllerInstance() {
        try {
            if (TelephonyTestUtil.getStaticField(UiccController.class, FIELD_INSTANCE) != null) {
                TelephonyTestUtil.setStaticField(UiccController.class, FIELD_INSTANCE, null);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception occurred at UiccController.mInstance" + e);
        }
    }

    public static class WrappedIccSmsInterfaceManager extends IccSmsInterfaceManager {
        protected WrappedIccSmsInterfaceManager(PhoneBase phone) {
            super(phone);
        }

        @Override
        public void sendText(String destAddr, String scAddr, String text, PendingIntent sentIntent,
                PendingIntent deliveryIntent) {
            // Using reflection since mDispatcher#sendText() is invisible.
            Class<?>[] parameterTypes = new Class<?>[] {
                    String.class, String.class, String.class, PendingIntent.class,
                    PendingIntent.class
            };
            Object[] args = new Object[] {
                    destAddr, scAddr, text, sentIntent, deliveryIntent
            };

            try {
                TelephonyTestUtil.invokeMethod(mDispatcher, "sendText", parameterTypes, args);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public boolean copyMessageToIccEf(int arg0, byte[] arg1, byte[] arg2) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean disableCellBroadcast(int arg0) throws RemoteException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean disableCellBroadcastRange(int arg0, int arg1) throws RemoteException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean enableCellBroadcast(int arg0) throws RemoteException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean enableCellBroadcastRange(int arg0, int arg1) throws RemoteException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public List<SmsRawData> getAllMessagesFromIccEf() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean updateMessageOnIccEf(int arg0, int arg1, byte[] arg2) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        protected void deleteSms(int index, Message response) {
            // TODO Auto-generated method stub
        }

        @Override
        protected void writeSms(int status, byte[] pdu, byte[] smsc, Message response) {
            // TODO Auto-generated method stub
        }

        @Override
        protected void log(String arg0) {
            // TODO Auto-generated method stub
        }
    }
}
