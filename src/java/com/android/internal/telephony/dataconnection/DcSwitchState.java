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

import com.android.internal.util.AsyncChannel;
import com.android.internal.util.Protocol;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneProxy;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.text.TextUtils;

import android.util.Log;

import java.util.HashSet;
import java.util.Iterator;

public class DcSwitchState extends StateMachine {
    protected static final boolean DBG = true;
    protected static final boolean VDBG = false;
    private static final String LOG_TAG = "PHONE";

    // ***** Event codes for driving the state machine
    protected static final int BASE = Protocol.BASE_DATA_CONNECTION_TRACKER + 0x00001000;
    protected static final int EVENT_CONNECT = BASE + 0;
    protected static final int EVENT_DISCONNECT = BASE + 1;
    protected static final int EVENT_CLEANUP_ALL = BASE + 2;
    protected static final int EVENT_CONNECTED = BASE + 3;
    protected static final int EVENT_DETACH_DONE = BASE + 4;
    protected static final int EVENT_TO_IDLE_DIRECTLY = BASE + 5;
    protected static final int EVENT_TO_ACTING_DIRECTLY = BASE + 6;

    protected int mId;
    private Phone mPhone;
    protected AsyncChannel mAc;
    protected RegistrantList mIdleRegistrants = new RegistrantList();
    protected HashSet<String> mApnTypes = new HashSet<String>();

    private DcSwitchIdleState     mIdleState = new DcSwitchIdleState();
    private DcSwitchActingState   mActingState = new DcSwitchActingState();
    private DcSwitchActedState    mActedState = new DcSwitchActedState();
    private DcSwitchDeactingState mDeactingState = new DcSwitchDeactingState();
    private DcSwitchDefaultState  mDefaultState = new DcSwitchDefaultState();

    protected DcSwitchState(Phone phone, String name, int id) {
        super(name);
        if (DBG) log("DcSwitchState constructor E");
        mPhone = phone;
        mId = id;

        addState(mDefaultState);
        addState(mIdleState, mDefaultState);
        addState(mActingState, mDefaultState);
        addState(mActedState, mDefaultState);
        addState(mDeactingState, mDefaultState);
        setInitialState(mIdleState);

        if (DBG) log("DcSwitchState constructor X");
    }

    private int setupConnection(String type) {
        mApnTypes.add(type);
        return mPhone.enableApnType(type);
    }

    private int teardownConnection(String type) {
        mApnTypes.remove(type);
        if (mApnTypes.isEmpty()) {
            log("No APN is using, then clean up all");
            // Since last type is removed from mApnTypes and will not be disabled in requestDataIdle()
            mPhone.disableApnType(type);
            requestDataIdle();
            transitionTo(mDeactingState);
            return PhoneConstants.APN_REQUEST_STARTED;
        } else {
            return mPhone.disableApnType(type);
        }
    }

    private void requestDataIdle() {
        if (DBG) log("requestDataIdle is triggered");
        Iterator<String> itrType = mApnTypes.iterator();
        while (itrType.hasNext()) {
            mPhone.disableApnType(itrType.next());
        }
        mApnTypes.clear();
        ((PhoneBase)((PhoneProxy)mPhone).getActivePhone()).mCi.requestDataIdle(obtainMessage(EVENT_DETACH_DONE));
    }

    public void notifyDataConnection(String state, String reason,
            String apnName, String apnType, boolean unavailable, int sim_id) {
        if (sim_id == mId && 
                TextUtils.equals(state, PhoneConstants.DataState.CONNECTED.toString())) {
            sendMessage(obtainMessage(EVENT_CONNECTED));
        }
    }

    public void cleanupAllConnection() {
        sendMessage(obtainMessage(EVENT_CLEANUP_ALL));
    }

    public void registerForIdle(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mIdleRegistrants.add(r);
    }

    public void unregisterForIdle(Handler h) {
        mIdleRegistrants.remove(h);
    }

    public void transitToIdleState() {
        sendMessage(obtainMessage(EVENT_TO_IDLE_DIRECTLY));
    }
    public void transitToActingState() {
        sendMessage(obtainMessage(EVENT_TO_ACTING_DIRECTLY));
    }

    private class DcSwitchIdleState extends State {
        @Override
        public void enter() {
            mIdleRegistrants.notifyRegistrants();
        }

        @Override
        public boolean processMessage(Message msg) {
            boolean retVal;

            switch (msg.what) {
                case DcSwitchAsyncChannel.REQ_CONNECT:
                case EVENT_CONNECT: {
                    String type = (String)msg.obj;
                    if (DBG) {
                        log("DcSwitchIdleState: REQ_CONNECT/EVENT_CONNECT(" + msg.what + ") type=" + type);
                    }
                    int result = setupConnection(type);
                    if (msg.what == DcSwitchAsyncChannel.REQ_CONNECT) {
                        mAc.replyToMessage(msg, DcSwitchAsyncChannel.RSP_CONNECT, result);
                    }
                    transitionTo(mActingState);
                    retVal = HANDLED;
                    break;
                }
                case DcSwitchAsyncChannel.REQ_DISCONNECT: {
                    String type = (String)msg.obj;
                    if (DBG) {
                        log("DcSwitchIdleState: DcSwitchAsyncChannel.REQ_DISCONNECT type=" + type);
                    }
                    mAc.replyToMessage(msg, DcSwitchAsyncChannel.RSP_DISCONNECT, PhoneConstants.APN_ALREADY_INACTIVE);
                    retVal = HANDLED;
                    break;
                }
                case EVENT_CLEANUP_ALL: {
                    if (DBG) {
                        log("DcSwitchIdleState: EVENT_CLEANUP_ALL" );
                    }
                    requestDataIdle();
                    retVal = HANDLED;
                    break;
                }
                case EVENT_CONNECTED: {
                    if (DBG) {
                        log("DcSwitchIdleState: Receive invalid event EVENT_CONNECTED!");
                    }
                    retVal = HANDLED;
                    break;
                }
                default:
                    if (VDBG) {
                        log("DcSwitchIdleState: nothandled msg.what=0x" +
                                Integer.toHexString(msg.what));
                    }
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }
    }

    private class DcSwitchActingState extends State {
        @Override
        public boolean processMessage(Message msg) {
            boolean retVal;

            switch (msg.what) {
                case DcSwitchAsyncChannel.REQ_CONNECT:
                case EVENT_CONNECT: {
                    String type = (String)msg.obj;
                    if (DBG) {
                        log("DcSwitchActingState: REQ_CONNECT/EVENT_CONNECT(" + msg.what + ") type=" + type);
                    }
                    int result = setupConnection(type);
                    if (msg.what == DcSwitchAsyncChannel.REQ_CONNECT) {
                        mAc.replyToMessage(msg, DcSwitchAsyncChannel.RSP_CONNECT, result);
                    }
                    retVal = HANDLED;
                    break;
                }
                case DcSwitchAsyncChannel.REQ_DISCONNECT: {
                    String type = (String)msg.obj;
                    if (DBG) {
                        log("DcSwitchActingState: DcSwitchAsyncChannel.REQ_DISCONNECT type=" + type);
                    }
                    int result = teardownConnection(type);
                    mAc.replyToMessage(msg, DcSwitchAsyncChannel.RSP_DISCONNECT, result);
                    retVal = HANDLED;
                    break;
                }
                case EVENT_CONNECTED: {
                    if (DBG) {
                        log("DcSwitchActingState: EVENT_CONNECTED");
                    }
                    transitionTo(mActedState);
                    retVal = HANDLED;
                    break;
                }
                case EVENT_CLEANUP_ALL: {
                    if (DBG) {
                        log("DcSwitchActingState: EVENT_CLEANUP_ALL" );
                    }
                    requestDataIdle();
                    transitionTo(mDeactingState);
                    retVal = HANDLED;
                    break;
                }
                default:
                    if (VDBG) {
                        log("DcSwitchActingState: nothandled msg.what=0x" +
                                Integer.toHexString(msg.what));
                    }
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }
    }

    private class DcSwitchActedState extends State {
        @Override
        public boolean processMessage(Message msg) {
            boolean retVal;

            switch (msg.what) {
                case DcSwitchAsyncChannel.REQ_CONNECT:
                case EVENT_CONNECT: {
                    String type = (String)msg.obj;
                    if (DBG) {
                        log("DcSwitchActedState: REQ_CONNECT/EVENT_CONNECT(" + msg.what + ") type=" + type);
                    }
                    int result = setupConnection(type);
                    if (msg.what == DcSwitchAsyncChannel.REQ_CONNECT) {
                        mAc.replyToMessage(msg, DcSwitchAsyncChannel.RSP_CONNECT, result);
                    }
                    retVal = HANDLED;
                    break;
                }
                case DcSwitchAsyncChannel.REQ_DISCONNECT: {
                    String type = (String)msg.obj;
                    if (DBG) {
                        log("DcSwitchActedState: DcSwitchAsyncChannel.REQ_DISCONNECT type=" + type);
                    }
                    int result = teardownConnection(type);
                    mAc.replyToMessage(msg, DcSwitchAsyncChannel.RSP_DISCONNECT, result);
                    retVal = HANDLED;
                    break;
                }
                case EVENT_CONNECTED: {
                    if (DBG) {
                        log("DcSwitchActedState: EVENT_CONNECTED");
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_CLEANUP_ALL: {
                    if (DBG) {
                        log("DcSwitchActedState: EVENT_CLEANUP_ALL" );
                    }
                    requestDataIdle();
                    transitionTo(mDeactingState);
                    retVal = HANDLED;
                    break;
                }
                default:
                    if (VDBG) {
                        log("DcSwitchActingState: nothandled msg.what=0x" +
                                Integer.toHexString(msg.what));
                    }
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }
    }

    private class DcSwitchDeactingState extends State {
        @Override
        public boolean processMessage(Message msg) {
            boolean retVal;

            switch (msg.what) {
                case DcSwitchAsyncChannel.REQ_CONNECT:
                case EVENT_CONNECT: {
                    String type = (String)msg.obj;
                    if (DBG) {
                        log("DcSwitchDeactingState: REQ_CONNECT/EVENT_CONNECT(" + msg.what + ") type=" + type + ", request is defered.");
                    }
                    deferMessage(obtainMessage(EVENT_CONNECT, type));
                    if (msg.what == DcSwitchAsyncChannel.REQ_CONNECT) {
                        mAc.replyToMessage(msg, DcSwitchAsyncChannel.RSP_CONNECT, PhoneConstants.APN_REQUEST_STARTED);
                    }
                    retVal = HANDLED;
                    break;
                }
                case DcSwitchAsyncChannel.REQ_DISCONNECT: {
                    String type = (String)msg.obj;
                    if (DBG) {
                        log("DcSwitchDeactingState: DcSwitchAsyncChannel.REQ_DISCONNECT type=" + type);
                    }
                    mAc.replyToMessage(msg, DcSwitchAsyncChannel.RSP_DISCONNECT, PhoneConstants.APN_ALREADY_INACTIVE);
                    retVal = HANDLED;
                    break;
                }
                case EVENT_DETACH_DONE: {
                    if (DBG) {
                        log("DcSwitchDeactingState: EVENT_DETACH_DONE");
                    }
                    transitionTo(mIdleState);
                    retVal = HANDLED;
                    break;
                }
                case EVENT_CONNECTED: {
                    if (DBG) {
                        log("DcSwitchDeactingState: Receive invalid event EVENT_CONNECTED!");
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_CLEANUP_ALL: {
                    if (DBG) {
                        log("DcSwitchDeactingState: EVENT_CLEANUP_ALL, already deacting." );
                    }
                    retVal = HANDLED;
                    break;
                }				
                default:
                    if (VDBG) {
                        log("DcSwitchDeactingState: nothandled msg.what=0x" +
                                Integer.toHexString(msg.what));
                    }
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }
    }


    private class DcSwitchDefaultState extends State {
        @Override
        public boolean processMessage(Message msg) {
            AsyncResult ar;

            switch (msg.what) {
                case AsyncChannel.CMD_CHANNEL_FULL_CONNECTION: {
                    if (mAc != null) {
                        if (VDBG) log("Disconnecting to previous connection mAc=" + mAc);
                        mAc.replyToMessage(msg, AsyncChannel.CMD_CHANNEL_FULLY_CONNECTED,
                                AsyncChannel.STATUS_FULL_CONNECTION_REFUSED_ALREADY_CONNECTED);
                    } else {
                        mAc = new AsyncChannel();
                        mAc.connected(null, getHandler(), msg.replyTo);
                        if (VDBG) log("DcDefaultState: FULL_CONNECTION reply connected");
                        mAc.replyToMessage(msg, AsyncChannel.CMD_CHANNEL_FULLY_CONNECTED,
                                AsyncChannel.STATUS_SUCCESSFUL, mId, "hi");
                    }
                    break;
                }
                case AsyncChannel.CMD_CHANNEL_DISCONNECT: {
                    if (VDBG) log("CMD_CHANNEL_DISCONNECT");
                    mAc.disconnect();
                    break;
                }
                case AsyncChannel.CMD_CHANNEL_DISCONNECTED: {
                    if (VDBG) log("CMD_CHANNEL_DISCONNECTED");
                    mAc = null;
                    break;
                }
                case DcSwitchAsyncChannel.REQ_IS_IDLE_STATE: {
                    boolean val = getCurrentState() == mIdleState;
                    if (VDBG) log("REQ_IS_IDLE_STATE  isIdle=" + val);
                    mAc.replyToMessage(msg, DcSwitchAsyncChannel.RSP_IS_IDLE_STATE, val ? 1 : 0);
                    break;
                }
                case DcSwitchAsyncChannel.REQ_IS_IDLE_OR_DEACTING_STATE: {
                    boolean val = (getCurrentState() == mIdleState || getCurrentState() == mDeactingState);
                    if (VDBG) log("REQ_IS_IDLE_OR_DEACTING_STATE  isIdleDeacting=" + val);
                    mAc.replyToMessage(msg, DcSwitchAsyncChannel.RSP_IS_IDLE_OR_DEACTING_STATE, val ? 1 : 0);
                    break;
                }
                case EVENT_TO_ACTING_DIRECTLY: {
                    log("Just transit to Acting state");
                    transitionTo(mActingState);
                    break;
                }
                case EVENT_TO_IDLE_DIRECTLY: {
                    log("Just transit to Idle state");
                    Iterator<String> itrType = mApnTypes.iterator();
                    while (itrType.hasNext()) {
                        mPhone.disableApnType(itrType.next());
                    }
                    mApnTypes.clear();
                    transitionTo(mIdleState);
                }
                default:
                    if (DBG) {
                        log("DcSwitchDefaultState: shouldn't happen but ignore msg.what=0x" +
                                Integer.toHexString(msg.what));
                    }
                    break;
            }

            return HANDLED;
        }
    }

    protected void log(String s) {
        Log.d(LOG_TAG, "[" + getName() + "] " + s);
    }
}
