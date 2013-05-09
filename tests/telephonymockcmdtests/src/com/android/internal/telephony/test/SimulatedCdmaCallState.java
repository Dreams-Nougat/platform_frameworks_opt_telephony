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

import android.telephony.PhoneNumberUtils;

import com.android.internal.telephony.ATParseEx;
import com.android.internal.telephony.DriverCall;

import java.util.ArrayList;
import java.util.List;

class CdmaCallInfo {
    enum State {
        ACTIVE(0),
        HOLDING(1),
        DIALING(2),    // MO call only
//        ALERTING(3),   // Not in CDMA
        INCOMING(4),   // MT call only
        WAITING(5);    // MT call only

        State (int value) {this.value = value;}

        private final int value;
        public int value() {return value;};
    };

    boolean isMT;
    State state;
    boolean isMpty;
    String number;
    int TOA;

    CdmaCallInfo(boolean isMT, State state, boolean isMpty, String number) {
        this.isMT = isMT;
        this.state = state;
        this.isMpty = isMpty;
        this.number = number;

        if (number.length() > 0 && number.charAt(0) == '+') {
            TOA = PhoneNumberUtils.TOA_International;
        } else {
            TOA = PhoneNumberUtils.TOA_Unknown;
        }
    }

    static CdmaCallInfo createOutgoingCall(String number) {
        return new CdmaCallInfo (false, State.DIALING, false, number);
    }

    static CdmaCallInfo createIncomingCall(String number) {
        return new CdmaCallInfo (true, State.INCOMING, false, number);
    }

    String toCLCCLine(int index) {
        return
            "+CLCC: "
            + index + "," + (isMT ? "1" : "0") +","
            + state.value() + ",0," + (isMpty ? "1" : "0")
            + ",\"" + number + "\"," + TOA;
    }

    DriverCall toDriverCall(int index) {
        DriverCall ret;

        ret = new DriverCall();

        ret.index = index;
        ret.isMT = isMT;

        try {
            ret.state = DriverCall.stateFromCLCC(state.value());
        } catch (ATParseEx ex) {
            throw new RuntimeException("should never happen", ex);
        }

        ret.isMpty = isMpty;
        ret.number = number;
        ret.TOA = TOA;
        ret.isVoice = true;
        ret.als = 0;

        return ret;
    }

    boolean isActiveOrHeld() {
        return state == State.ACTIVE || state == State.HOLDING;
    }

    boolean isConnecting() {
        return state == State.DIALING; //|| state == State.ALERTING;
    }

    boolean isRinging() {
        return state == State.INCOMING || state == State.WAITING;
    }

}

class SimulatedCdmaCallState {

    private CdmaCallInfo[] calls = new CdmaCallInfo[7];

    public void onDial(String address) {

        String phNum = PhoneNumberUtils.extractNetworkPortion(address);

        if (phNum.length() == 0) {
            // TODO
            throw new RuntimeException("invalid ph num");
        }

        // TODO need to check existing lines?

        calls[0] = CdmaCallInfo.createOutgoingCall(phNum);

    }

    public boolean onAnswer() {
        for (int i = 0 ; i < calls.length ; i++) {
            CdmaCallInfo call = calls[i];

            if (call != null
                && (call.state == CdmaCallInfo.State.INCOMING
                    || call.state == CdmaCallInfo.State.WAITING)
            ) {
                return switchActiveAndHeldOrWaiting();
            }
        }

        return false;
    }

    public boolean switchActiveAndHeldOrWaiting() {
        boolean hasHeld = false;

        // first, are there held calls?
        for (int i = 0 ; i < calls.length ; i++) {
            CdmaCallInfo c = calls[i];

            if (c != null && c.state == CdmaCallInfo.State.HOLDING) {
                hasHeld = true;
                break;
            }
        }

        // Now, switch
        for (int i = 0 ; i < calls.length ; i++) {
            CdmaCallInfo c = calls[i];

            if (c != null) {
                if (c.state == CdmaCallInfo.State.ACTIVE) {
                    c.state = CdmaCallInfo.State.HOLDING;
                } else if (c.state == CdmaCallInfo.State.HOLDING) {
                    c.state = CdmaCallInfo.State.ACTIVE;
                } else if (!hasHeld && c.isRinging())  {
                    c.state = CdmaCallInfo.State.ACTIVE;
                }
            }
        }

        return true;
    }

    /**
     * Start the simulated phone ringing
     * true if succeeded, false if failed
     */
    public void triggerRing(String number) {
        synchronized (this) {
            int empty = -1;
            boolean isCallWaiting = false;

            // ensure there aren't already calls INCOMING or WAITING
            for (int i = 0 ; i < calls.length ; i++) {
                CdmaCallInfo call = calls[i];

                if (call == null && empty < 0) {
                    empty = i;
                } else if (call != null && call.isRinging()) {
                    throw new RuntimeException("triggerRing failed; phone already ringing");
                } else if (call != null) {
                    isCallWaiting = true;
                }
            }

            if (empty < 0 ) {
                throw new RuntimeException("triggerRing failed; all full");
            }

            calls[empty] = CdmaCallInfo.createIncomingCall(
                PhoneNumberUtils.extractNetworkPortion(number));

            if (isCallWaiting) {
                calls[empty].state = CdmaCallInfo.State.WAITING;
            }
        }
    }

    /** If a call is DIALING , progress it to the next state */
    public synchronized void progressConnectingCallState() {
        for (int i = 0 ; i < calls.length ; i++) {
            CdmaCallInfo call = calls[i];

            if (call != null && call.state == CdmaCallInfo.State.DIALING) {
                call.state = CdmaCallInfo.State.ACTIVE;
                break;
            }
        }
    }

    public boolean releaseActiveAcceptHeldOrWaiting() {
        boolean foundActive = false;

        for (int i = 0 ; i < calls.length ; i++) {
            CdmaCallInfo c = calls[i];

            if (c != null && c.state == CdmaCallInfo.State.ACTIVE) {
                calls[i] = null;
                foundActive = true;
            }
        }

        return true;
    }


    public List<DriverCall> getDriverCalls() {
        ArrayList<DriverCall> ret = new ArrayList<DriverCall>(calls.length);

        for (int i = 0 ; i < calls.length ; i++) {
            CdmaCallInfo c = calls[i];

            if (c != null) {
                DriverCall dc;

                dc = c.toDriverCall(i + 1);
                ret.add(dc);
            }
        }

        return ret;
    }

}
