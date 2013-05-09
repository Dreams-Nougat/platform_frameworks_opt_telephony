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
import android.test.AndroidTestCase;
import android.util.Log;

import com.android.internal.telephony.cdma.sms.CdmaSmsAddress;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.test.RILResponseUtil;
import com.android.internal.telephony.test.RILSimulator;
import com.android.internal.telephony.test.TelephonyTestExecutor;
import com.android.internal.telephony.test.TelephonyTestExecutor.PhoneMode;

import com.android.internal.util.HexDump;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

public class FSMSNormalTest extends AndroidTestCase {

    private static final String TAG = "FSMSNormalTest";

    private static final int EVENT_PHONE_STATE_CHANGED = 1;
    private static final int EVENT_SERVICE_STATE_CHANGED = 2;
    private static final int EVENT_DISCONNECT = 3;
    private static final int EVENT_RINGING = 4;
    private static final int EVENT_CALL_WAITING = 5;
    private static final int EVENT_RIL_API_CALLED = 6;

    private Phone mPhone;
    private TelephonyTestExecutor mExecutor;
    private RILSimulator mRilSim;
    private EventHandler mHandler;

    private static final String SMS_SEND_ACTION = "SMS_SEND_ACTION";
    private static final String SMS_DELIVERY_ACTION = "SMS_DELIVERY_ACTION";
    private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
    private static final String SMS_CB_RECEIVED_ACTION =
            "android.provider.Telephony.SMS_CB_RECEIVED";
    private static final String SMS_EMERGENCY_CB_RECEIVED_ACTION =
            "android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED";
    private static SmsMessage[] mReceivedSmsMessage = null;
    private static SmsCbMessage mReceivedSmsCbMessage = null;

    private static final String ORIGINATE_ADDRESS_01 = "09012345678";
    private static final String TIME_STAMP_01 = "0306120606000000";
    private static final String TIME_STAMP_02 = "0306120605020000";
    private static final String IMS_TIME_STAMP_01 ="216060000000";
    private static final String IMS_TIME_STAMP_02 ="216060000011";

    private SmsBroadcastReceiver mSmsReceiver;

    private static final int mDefSubmitRepoOK = 0;
    private IccSmsInterfaceManager mIccSmsInterfaceManager = null;
    private static final String mDefSendMessage = "01234567890123456789012345678901234567"
            + "89012345678901234567890123456789012345678901234567890123456789abcdefghijab"
            + "cdefghijabcdefghijabcdefghijabcdefghijabcdefghij";

    private static final String mDefSendDestAddr = "09012345678";
    private static final String mDefSendScAddr = "09087654321";
    private PendingIntent mSentPendingIntent;
    private PendingIntent mDeliveredPendingIntent;
    private boolean mImsRegisterd = false;
    private SmsBroadcastReceiver mSendReceiver;
    private static final int MAX_SMS_SEND_NUM = 50;
    private static final int MAX_SECONDS = 50;

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
                case EVENT_SERVICE_STATE_CHANGED: // waiting a special state
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
                case EVENT_RIL_API_CALLED: // waiting a RIL api called
                    Log.d(TAG, "EVENT_RIL_API_CALLED - " +
                            ((RILSimulator.CallRilAPIKind)r.result).toString());
                    if (expectState == null
                            || (RILSimulator.CallRilAPIKind)expectState
                            == (RILSimulator.CallRilAPIKind)r.result) {
                        isConsumed = true;
                    }
                    break;
                default:  // waiting the message only
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
         * Wait the current thread until the handler receives the specific message.
         * @param what Message code to be received.
         * @throws InterruptedException
         */
        public void waitForMessage(int what) throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            mLatches.put(what, latch);
            latch.await(10, TimeUnit.SECONDS);
        }

        /**
         * Wait the current thread until the handler receives the specific message
         * with specific status.
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
        mExecutor = new TelephonyTestExecutor(getContext(), PhoneMode.CDMA);
        mPhone = mExecutor.getPhone();
        mRilSim = mExecutor.getRILSimulator();
        mExecutor.getNotificationRegister();

        // Register for necessary events for this test.
        mHandler = new EventHandler(mExecutor.getLooper());
        mPhone.registerForPreciseCallStateChanged(mHandler, EVENT_PHONE_STATE_CHANGED, null);
        mPhone.registerForServiceStateChanged(mHandler, EVENT_SERVICE_STATE_CHANGED, null);
        mPhone.registerForDisconnect(mHandler, EVENT_DISCONNECT, null);
        mPhone.registerForNewRingingConnection(mHandler, EVENT_RINGING, null);
        mPhone.registerForCallWaiting(mHandler, EVENT_CALL_WAITING, null);
        mRilSim.registerForCallRilApi(mHandler, EVENT_RIL_API_CALLED, null);

        mReceivedSmsMessage = null;
        mReceivedSmsCbMessage = null;

        mIccSmsInterfaceManager = mExecutor.getIccSmsInterfaceManager();

        // preparation
        setupUntilStateInService();

        // prepare Intent
        IntentFilter smsReceivedIntentFilter = new IntentFilter(SMS_RECEIVED_ACTION);
        mSmsReceiver = new SmsBroadcastReceiver(SMS_RECEIVED_ACTION);
        getContext().registerReceiver(mSmsReceiver, smsReceivedIntentFilter);

        // prepare Intent
        Intent sendIntent = new Intent(SMS_SEND_ACTION);
        Intent deliveryIntent = new Intent(SMS_DELIVERY_ACTION);
        IntentFilter sendIntentFilter = new IntentFilter(SMS_SEND_ACTION);
        mSendReceiver = new SmsBroadcastReceiver(SMS_SEND_ACTION);
        getContext().registerReceiver(mSendReceiver, sendIntentFilter);
        mSentPendingIntent = PendingIntent.getBroadcast(getContext(), 0, sendIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mDeliveredPendingIntent = PendingIntent.getBroadcast(getContext(), 0, deliveryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // set Ims registration -->1xCs
        setImsRegistration(false);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mExecutor.dispose();
        getContext().unregisterReceiver(mSmsReceiver);
        getContext().unregisterReceiver(mSendReceiver);
    }

    /**
     * Set up phone until the service state is STATE_IN_SERVICE.
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
        mHandler.waitForMessageAndState(EVENT_SERVICE_STATE_CHANGED,
                ServiceState.STATE_IN_SERVICE);

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
         * Wait the current thread until the BroadcastReceiver receives the specific intent.
         * @param what Message code to be received.
         * @throws InterruptedException
         */
        public void waitForMessage(String what) throws InterruptedException {
            mReceiveIntet = null;
            CountDownLatch latch = new CountDownLatch(1);
            mLatches.put(what, latch);
            latch.await(10, TimeUnit.SECONDS);
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
                        Object[] messages = (Object[]) bundle.get("pdus");
                        byte[][] pduObjs = new byte[messages.length][];
                        for (int i = 0; i < messages.length; i++) {
                            pduObjs[i] = (byte[]) messages[i];
                        }
                        byte[][] pdus = new byte[pduObjs.length][];
                        int pduCount = pdus.length;
                        SmsMessage[] msgs = new SmsMessage[pduCount];
                        for (int i = 0; i < pduCount; i++) {
                            pdus[i] = pduObjs[i];
                            msgs[i] = SmsMessage.createFromPdu(pdus[i]);
                        }
                        mReceivedSmsMessage = msgs;
                    }
                }
                else if (SMS_DELIVERY_ACTION.equals(mAction)) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        byte[] pdu = bundle.getByteArray("pdu");
                        SmsMessage.createFromPdu(pdu);
                    }
                }
                else if (SMS_CB_RECEIVED_ACTION.equals(mAction) ||
                        SMS_EMERGENCY_CB_RECEIVED_ACTION.equals(mAction)) {
                    Bundle bundle = intent.getExtras();
                    mReceivedSmsCbMessage = (SmsCbMessage) bundle.get("message");
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
     * @throws InterruptedException
     */
    private void setImsRegistration(boolean setIms)
            throws InterruptedException {

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
