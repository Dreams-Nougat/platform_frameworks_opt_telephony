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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.provider.Telephony.Sms;
import android.telephony.ServiceState;
import android.telephony.SmsCbMessage;
import android.telephony.SmsMessage;
import android.telephony.test.util.TelephonyTestUtil;
import android.test.AndroidTestCase;
import android.util.Log;

import com.android.internal.telephony.cdma.sms.CdmaSmsAddress;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.test.NotificationRegister;
import com.android.internal.telephony.test.RILResponseUtil;
import com.android.internal.telephony.test.RILSimulator;
import com.android.internal.telephony.test.TelephonyTestExecutor;
import com.android.internal.telephony.test.TelephonyTestExecutor.PhoneMode;
import com.android.internal.telephony.test.TelephonyTestExecutor.WrappedIccSmsInterfaceManager;

import com.android.internal.util.HexDump;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FSubmitDeliverTest extends AndroidTestCase {

    private static final String TAG = "FSubmitDeliverTest";

    private static final int EVENT_PHONE_STATE_CHANGED = 1;

    private static final int EVENT_SERVICE_STATE_CHANGED = 2;

    private static final int EVENT_DISCONNECT = 3;

    private static final int EVENT_RINGING = 4;

    private static final int EVENT_CALL_WAITING = 5;

    private static final int EVENT_RIL_API_CALLED = 6;

    private Phone mPhone;

    private TelephonyTestExecutor mExecutor;

    private RILSimulator mRilSim;

    private NotificationRegister mRegister;

    private EventHandler mHandler;

    private SmsBroadcastReceiver mSendReceiver;

    private SmsBroadcastReceiver mDeliveryReceiver;

    private SmsBroadcastReceiver mSmsReceiver;

    private SmsBroadcastReceiver mSmsWapPushReceiver;

    private PendingIntent mSentPendingIntent;

    private PendingIntent mDeliveredPendingIntent;

    private static final String SMS_SEND_ACTION = "SMS_SEND_ACTION";

    private static final String SMS_DELIVERY_ACTION = "SMS_DELIVERY_ACTION";

    private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";

    private static final String WAP_PUSH_RECEIVED_ACTION =
            "android.provider.Telephony.WAP_PUSH_RECEIVED";

    private static final String SMS_CB_RECEIVED_ACTION =
            "android.provider.Telephony.SMS_CB_RECEIVED";

    private static final String SMS_EMERGENCY_CB_RECEIVED_ACTION =
            "android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED";

    private static final String SMS_EMERGENCY_CDMA_MESSAGE_RECEIVED_ACTION =
            "android.provider.Telephony.EMERGENCY_CDMA_MESSAGE_RECEIVED_ACTION";

    private WrappedIccSmsInterfaceManager mIccSmsInterfaceManager = null;

    private static SmsMessage[] mReceivedSmsMessage = null;

    private static SmsMessage mReceivedSmsStatusReport = null;

    private static String mReceiveMessageBody = null;

    private boolean mImsRegisterd = false;

    private static SmsCbMessage mReceivedSmsCbMessage = null;

    private static int mbroadcastWaitSec = 10;

    private static final String mDefSendMessage = "test message";

    private static final String mDefSendDestAddr = "08067129312";

    private static final String mDefSendScAddr = "09023201231";

    private static final int mDefMessageRef = 1;

    private static final int mDefSubmitRepoError = 9; // CommandException::SMS_FAIL_RETRY

    private static final int mDefSubmitRepoOK = 0;

    private static final int SEND1_MSG_REF = 1;

    private static final int SEND2_MSG_REF = SEND1_MSG_REF + 1;

    private static final int SEND3_MSG_REF = SEND1_MSG_REF + 2;

    private static final String TIME_STAMP_01 = "0306120605010000";

    private static final String TIME_STAMP_02 = "0306120605020000";

    private static final String TIME_STAMP_03 = "0306120605030000";

    private static int m1xCsTimeStampCount = 0;

    class EventHandler extends Handler {

        private Map<Integer, CountDownLatch> mLatches = new HashMap<Integer, CountDownLatch>();

        private Object expectState;

        public EventHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage() - " + msg.what);
            CountDownLatch latch = mLatches.get(msg.what);
            if (latch == null) {
                return;
            }

            AsyncResult r = (AsyncResult)msg.obj;
            boolean isConsumed = false;
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED: // waiting for a special state
                    if (expectState == null
                            || (Integer)expectState == ((ServiceState)r.result).getState()) {
                        isConsumed = true;
                    }
                    break;
                case EVENT_PHONE_STATE_CHANGED:
                    Log.d(TAG, "EVENT_PHONE_STATE_CHANGED - " + mPhone.getState());
                    if (expectState == null
                            || (PhoneConstants.State)expectState == mPhone.getState()) {
                        isConsumed = true;
                    }
                    break;
                case EVENT_RIL_API_CALLED: // waiting for a RIL API to be called
                    Log.d(TAG,
                            "EVENT_RIL_API_CALLED - "
                                    + ((RILSimulator.CallRilAPIKind)r.result).toString());
                    if (expectState == null
                            || (RILSimulator.CallRilAPIKind)expectState ==
                            (RILSimulator.CallRilAPIKind)r.result) {
                        isConsumed = true;
                    }
                    break;
                default: // waiting the message only
                    isConsumed = true;
                    break;
            }

            if (isConsumed) {
                mLatches.remove(msg.what);
                latch.countDown();
                expectState = null;
            }
        }

        /**
         * Wait the current thread until the handler receives the specific
         * message.
         *
         * @param what Message code to be received.
         * @throws InterruptedException
         */
        public void waitForMessage(int what) throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            mLatches.put(what, latch);
            latch.await(10, TimeUnit.SECONDS);
        }

        /**
         * Wait the current thread until the handler receives the specific
         * message with specific status.
         *
         * @param what Message code to be received.
         * @param state
         * @throws InterruptedException
         */
        public void waitForMessageAndState(int what, Object state) throws InterruptedException {
            expectState = state;
            waitForMessage(what);
        }

        /**
         * Judge the specific message timeout.
         *
         * @param what Message code to be received.
         * @return timeout info.(true:timeout)
         * @throws InterruptedException
         */
        public boolean isTimeOut(int what) throws InterruptedException {
            CountDownLatch latch = mLatches.get(what);
            return (latch != null);
        }

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Set up Simulated FW
        mExecutor = new TelephonyTestExecutor(getContext(), PhoneMode.CDMALTE);
        mPhone = mExecutor.getPhone();
        mIccSmsInterfaceManager = mExecutor.getIccSmsInterfaceManager();
        mRilSim = mExecutor.getRILSimulator();
        mRegister = mExecutor.getNotificationRegister();

        // Register for necessary events for this test.
        mHandler = new EventHandler(mExecutor.getLooper());
        mPhone.registerForPreciseCallStateChanged(mHandler, EVENT_PHONE_STATE_CHANGED, null);
        mPhone.registerForServiceStateChanged(mHandler, EVENT_SERVICE_STATE_CHANGED, null);
        mPhone.registerForDisconnect(mHandler, EVENT_DISCONNECT, null);
        mPhone.registerForNewRingingConnection(mHandler, EVENT_RINGING, null);
        mPhone.registerForCallWaiting(mHandler, EVENT_CALL_WAITING, null);
        mRilSim.registerForCallRilApi(mHandler, EVENT_RIL_API_CALLED, null);

        mReceivedSmsMessage = null;
        mReceivedSmsStatusReport = null;
        mReceiveMessageBody = null;
        mReceivedSmsCbMessage = null;

        setupUntilStateInService();

        // prepare Intent
        Intent sendIntent = new Intent(SMS_SEND_ACTION);
        Intent deliveryIntent = new Intent(SMS_DELIVERY_ACTION);
        IntentFilter sendIntentFilter = new IntentFilter(SMS_SEND_ACTION);
        IntentFilter deliveryIntentFilter = new IntentFilter(SMS_DELIVERY_ACTION);
        mSendReceiver = new SmsBroadcastReceiver(SMS_SEND_ACTION);
        mDeliveryReceiver = new SmsBroadcastReceiver(SMS_DELIVERY_ACTION);
        getContext().registerReceiver(mSendReceiver, sendIntentFilter);
        getContext().registerReceiver(mDeliveryReceiver, deliveryIntentFilter);
        mSentPendingIntent = PendingIntent.getBroadcast(getContext(), 0, sendIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mDeliveredPendingIntent = PendingIntent.getBroadcast(getContext(), 0, deliveryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // prepare Intent
        IntentFilter smsReceivedIntentFilter = new IntentFilter(SMS_RECEIVED_ACTION);
        mSmsReceiver = new SmsBroadcastReceiver(SMS_RECEIVED_ACTION);
        getContext().registerReceiver(mSmsReceiver, smsReceivedIntentFilter);

        // prepare Intent
        IntentFilter smsWapPushReceivedIntentFilter =
                new IntentFilter(WAP_PUSH_RECEIVED_ACTION, "application/vnd.wap.mms-message");
        mSmsWapPushReceiver = new SmsBroadcastReceiver(WAP_PUSH_RECEIVED_ACTION);
        getContext().registerReceiver(mSmsWapPushReceiver, smsWapPushReceivedIntentFilter);

        // set Ims registration -->1xCs
        setImsRegistration(false);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mExecutor.dispose();
        getContext().unregisterReceiver(mSendReceiver);
        getContext().unregisterReceiver(mDeliveryReceiver);
        getContext().unregisterReceiver(mSmsReceiver);
        getContext().unregisterReceiver(mSmsWapPushReceiver);
    }

    /**
     * Set up phone until the service state is STATE_IN_SERVICE.
     *
     * @throws InterruptedException
     */
    private void setupUntilStateInService() throws InterruptedException {
        String[] regist = RILResponseUtil.RegistrationCDMA.REGISTERED_IN_HOME();
        mRilSim.setVoiceRegistrationState(regist, null);

        String[] dataRegist = RILResponseUtil.DataRegistration.IN_SERVICE_1xRTT();
        mRilSim.setDataRegistrationState(dataRegist, null);

        // *Don't care* Just set normal values.
        String[] operator = RILResponseUtil.Operator.Dummy();
        mRilSim.setOperator(operator, null);

        // *Don't care* Just set normal values.
        String[] subs = RILResponseUtil.CDMASubscription.REAL_SIM();
        mRilSim.setCDMASubscription(subs, null);

        // *Don't care* Just set normal values.
        DataCallState ret = RILResponseUtil.SetupDataCall.SUCCESS();
        mRilSim.setSetupDataCall(ret, null);

        mRilSim.changeRadioState(RadioState.RADIO_ON);
        mHandler.waitForMessageAndState(EVENT_SERVICE_STATE_CHANGED, ServiceState.STATE_IN_SERVICE);

        assertEquals(ServiceState.STATE_IN_SERVICE, mPhone.getServiceState().getState());
    }

    private static class SmsBroadcastReceiver extends BroadcastReceiver {
        private final String mAction;

        private Map<String, CountDownLatch> mLatches = new HashMap<String, CountDownLatch>();

        private String mReceiveIntet;

        SmsBroadcastReceiver(String action) {
            mAction = action;
        }

        /**
         * Wait the current thread until the BroadcastReceiver receives the
         * specific intent.
         *
         * @param what Message code to be received.
         * @throws InterruptedException
         */
        public void waitForMessage(String what) throws InterruptedException {
            mReceiveIntet = null;
            CountDownLatch latch = new CountDownLatch(1);
            mLatches.put(what, latch);
            latch.await(mbroadcastWaitSec, TimeUnit.SECONDS);
        }

        public String getReceiveIntentKind() {
            return mReceiveIntet;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(mAction)) {
                mReceiveIntet = intent.getAction();
                Log.d(TAG, "Receive " + mAction);

                if (SMS_RECEIVED_ACTION.equals(mAction)) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Object[] pdus = (Object[])bundle.get("pdus");
                        mReceivedSmsMessage = Sms.Intents.getMessagesFromIntent(intent);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mReceivedSmsMessage.length; i++) {
                            if (mReceivedSmsMessage[i] != null) {
                                sb.append(mReceivedSmsMessage[i].getMessageBody());
                            }
                        }
                        mReceiveMessageBody = sb.toString();
                    }
                } else if (SMS_DELIVERY_ACTION.equals(mAction)) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        byte[] pdu = bundle.getByteArray("pdu");
                        String format = (String)bundle.get("format");
                        mReceivedSmsStatusReport = SmsMessage.createFromPdu(pdu, format);
                    }
                } else if (SMS_CB_RECEIVED_ACTION.equals(mAction)
                        || SMS_EMERGENCY_CB_RECEIVED_ACTION.equals(mAction)) {
                    Bundle bundle = intent.getExtras();
                    mReceivedSmsCbMessage = (SmsCbMessage) bundle.get("message");
                } else if (WAP_PUSH_RECEIVED_ACTION.equals(mAction)) {
                    ;
                }
            }
            CountDownLatch latch = mLatches.get(intent.getAction());
            if (latch == null) {
                return;
            } else {
                mLatches.remove(intent.getAction());
                latch.countDown();
            }
        }
    }

    /**
     * Set Ims registration status.
     *
     * @throws InterruptedException
     */
    private void setImsRegistration(boolean setIms) throws InterruptedException {

        int imsRegisterd;
        int technologies;

        if (setIms) {
            imsRegisterd = 1; // IMS is registered
            technologies = 1; // 3GPP (GSM, WCDMA, LTE)
            mImsRegisterd = true;
        } else {
            imsRegisterd = 0; // IMS is not registered
            technologies = 2; // 3GPP2 (CDMA, EVDO)
            mImsRegisterd = false;
        }

        mRilSim.triggerNotifyImsStatus(imsRegisterd, technologies);

        // wait call getImsRegistrationState in RIL
        mHandler.waitForMessageAndState(EVENT_RIL_API_CALLED,
                RILSimulator.CallRilAPIKind.CALLED_GET_IMS_REGISTRATION_STATE);

        // wait registration status reflection(3s)
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);
    }

}
