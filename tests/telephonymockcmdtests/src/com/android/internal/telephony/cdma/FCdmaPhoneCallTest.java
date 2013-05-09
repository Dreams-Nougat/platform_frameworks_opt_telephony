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

package com.android.internal.telephony.cdma;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.ServiceState;
import android.test.AndroidTestCase;
import android.util.Log;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DataCallState;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.test.RILResponseUtil;
import com.android.internal.telephony.test.NotificationRegister;
import com.android.internal.telephony.test.RILSimulator;
import com.android.internal.telephony.test.TelephonyTestExecutor;
import com.android.internal.telephony.test.TelephonyTestExecutor.PhoneMode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Tests of making CDMA phone calls.
 */
public class FCdmaPhoneCallTest extends AndroidTestCase {

    private static final String TAG = "FCdmaPhoneCallTest";

    private Phone mPhone;
    private TelephonyTestExecutor mExecutor;
    private RILSimulator mSimulator;
    private NotificationRegister mRegister;
    private EventHandler mHandler;

    private static final int EVENT_PHONE_STATE_CHANGED = 1;
    private static final int EVENT_SERVICE_STATE_CHANGED = 2;
    private static final int EVENT_DISCONNECT = 3;
    private static final int EVENT_RINGING = 4;
    private static final int EVENT_CALL_WAITING = 5;

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
                default:  // waiting for the message only
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
         * Wait until the handler receives the specific message.
         * @param what Message code to be received.
         * @throws InterruptedException
         */
        public void waitForMessage(int what) throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            mLatches.put(what, latch);
            latch.await(10, TimeUnit.SECONDS);
        }

        /**
         * Wait until the handler receives the specific message with specific status.
         * @param what Message code to be received.
         * @param state
         * @throws InterruptedException
         */
        public void waitForMessageAndState(int what, Object state) throws InterruptedException {
            expectState = state;
            waitForMessage(what);
        }

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Set up Simulated FW
        mExecutor = new TelephonyTestExecutor(getContext(), PhoneMode.CDMA);
        mPhone = mExecutor.getPhone();
        mSimulator = mExecutor.getRILSimulator();
        mRegister = mExecutor.getNotificationRegister();

        // Register for necessary events for this test.
        mHandler = new EventHandler(mExecutor.getLooper());
        mPhone.registerForPreciseCallStateChanged(mHandler, EVENT_PHONE_STATE_CHANGED, null);
        mPhone.registerForServiceStateChanged(mHandler, EVENT_SERVICE_STATE_CHANGED, null);
        mPhone.registerForDisconnect(mHandler, EVENT_DISCONNECT, null);
        mPhone.registerForNewRingingConnection(mHandler, EVENT_RINGING, null);
        mPhone.registerForCallWaiting(mHandler, EVENT_CALL_WAITING, null);
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mExecutor.dispose();
    }


    /**
     * Test making an outgoing call and hanging up.
     *
     * Phone state transition:
     *   (dial)-> OFFHOOK -(hang up)-> IDLE
     * Foreground Call state transition:
     *   (dial)-> DIALING -(answer)-> ACTIVE -(hang up)-> DISCONNECTED
     */
    public void testOutgoingCall() throws Exception {
        // ---- preparation ----
        setupUntilStateInService();

        // ---- execution ----
        Connection cn = mPhone.dial("0123-456-789");

        mHandler.waitForMessage(EVENT_PHONE_STATE_CHANGED);

        // ---- check ----
        assertEquals(PhoneConstants.State.OFFHOOK, mPhone.getState());
        assertEquals(Call.State.DIALING, mPhone.getForegroundCall().getState());
        assertTrue(mPhone.getForegroundCall().isDialingOrAlerting());
        assertEquals(1, mPhone.getForegroundCall().getConnections().size());

        // ---- execution ----
        mSimulator.progressConnectingCallState();
        mHandler.waitForMessage(EVENT_PHONE_STATE_CHANGED);

        // ---- check ----
        assertEquals(PhoneConstants.State.OFFHOOK, mPhone.getState());
        assertEquals(Call.State.ACTIVE, mPhone.getForegroundCall().getState());
        assertFalse(mPhone.getForegroundCall().isDialingOrAlerting());

        // ---- execution ----
        mPhone.getForegroundCall().hangup();
        mHandler.waitForMessage(EVENT_DISCONNECT);

        // ---- check ----
        assertEquals(PhoneConstants.State.IDLE, mPhone.getState());
        assertEquals(Call.State.DISCONNECTED, mPhone.getForegroundCall().getState());

        // ---- execution ----
        mPhone.clearDisconnected();

        // ---- check ----
        assertEquals(0, mPhone.getForegroundCall().getConnections().size());
    }


    /**
     * Test receiving an incoming call.
     *
     * Phone state transition:
     *   (trigger ring)-> RINGING -(accept call)-> OFFHOOK -(hang up)-> IDLE
     * Ringing Call state transition:
     *   (trigger ring)-> INCOMING -(accept call)-> IDLE
     * Foreground Call state transition:
     *   (accept call)-> ACTIVE -(hang up)-> DISCONNECTED
     */
    public void testIncomingCall() throws Exception {
        // ---- preparation ----
        setupUntilStateInService();

        // ---- execution ----
        mSimulator.triggerRing("987-654-3210");
        mHandler.waitForMessage(EVENT_SERVICE_STATE_CHANGED);
        mHandler.waitForMessage(EVENT_RINGING);

        // ---- check ----
        assertEquals(PhoneConstants.State.RINGING, mPhone.getState());
        assertTrue(mPhone.getRingingCall().isRinging());

        assertEquals(Call.State.INCOMING, mPhone.getRingingCall().getState());

        // ---- execution ----
        mPhone.acceptCall();
        mHandler.waitForMessageAndState(EVENT_PHONE_STATE_CHANGED, PhoneConstants.State.RINGING);
        mHandler.waitForMessageAndState(EVENT_PHONE_STATE_CHANGED, PhoneConstants.State.OFFHOOK);

        // ---- check ----
        assertEquals(PhoneConstants.State.OFFHOOK, mPhone.getState());
        assertFalse(mPhone.getRingingCall().isRinging());

        assertEquals(Call.State.IDLE, mPhone.getRingingCall().getState());
        assertEquals(Call.State.ACTIVE, mPhone.getForegroundCall().getState());

        // ---- execution ----
        mPhone.getForegroundCall().hangup();
        mHandler.waitForMessage(EVENT_DISCONNECT);

        // ---- check ----
        assertEquals(PhoneConstants.State.IDLE, mPhone.getState());
        assertEquals(Call.State.DISCONNECTED, mPhone.getForegroundCall().getState());

        // ---- execution ----
        mPhone.clearDisconnected();

        // ---- check ----
        assertEquals(0, mPhone.getForegroundCall().getConnections().size());
    }

    /**
     * Set up the phone until the service state is STATE_IN_SERVICE.
     * @throws InterruptedException
     */
    private void setupUntilStateInService() throws InterruptedException {
        String[] regist = RILResponseUtil.RegistrationCDMA.REGISTERED_IN_HOME();
        mSimulator.setVoiceRegistrationState(regist, null);

        String[] dataRegist = RILResponseUtil.DataRegistration.IN_SERVICE_1xRTT();
        mSimulator.setDataRegistrationState(dataRegist, null);

        // *Don't care* Just set normal values.
        String[] operator = RILResponseUtil.Operator.Dummy();
        mSimulator.setOperator(operator, null);

        // *Don't care* Just set normal values.
        String[] subs = RILResponseUtil.CDMASubscription.REAL_SIM();
        mSimulator.setCDMASubscription(subs, null);

        // *Don't care* Just set normal values.
        DataCallState ret = RILResponseUtil.SetupDataCall.SUCCESS();
        mSimulator.setSetupDataCall(ret, null);

        mSimulator.changeRadioState(RadioState.RADIO_ON);
        mHandler.waitForMessageAndState(EVENT_SERVICE_STATE_CHANGED, ServiceState.STATE_IN_SERVICE);

        assertEquals(ServiceState.STATE_IN_SERVICE, mPhone.getServiceState().getState());
    }
}
