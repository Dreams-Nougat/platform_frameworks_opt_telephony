/* Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.android.internal.telephony;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.Rlog;

import com.android.internal.R;

import java.util.concurrent.ConcurrentHashMap;

public class SmsSecurityService extends ISmsSecurityService.Stub {

    public interface SmsSecurityServiceCallback {
        void onAuthorizationResult(boolean authorized);
    }

    public static final String SERVICE_NAME = "sms-sec";

    public static final int ERROR_CODE_BLOCKED = 191286;

    private static final String LOG_TAG = SmsSecurityService.class.getSimpleName();

    private final ConcurrentHashMap<IBinder, PendingRequestRecord> mPendingRequests;

    private final Context mContext;

    private volatile SecurityAgentRecord mAgentRecord;

    private final long mTimeoutMs;

    public SmsSecurityService(final Context context) {
        mPendingRequests = new ConcurrentHashMap<>();
        mTimeoutMs = context.getResources().getInteger(R.integer.config_sms_authorization_timeout_ms);
        mContext = context;
    }

    @Override
    public boolean register(final ISmsSecurityAgent agent) throws RemoteException {
        mContext.enforceCallingOrSelfPermission(permission.AUTHORIZE_OUTGOING_SMS, LOG_TAG);
        boolean registered = false;
        synchronized (this) {
            if (mAgentRecord != null && !mAgentRecord.mAgent.asBinder().equals(agent.asBinder())) {
                unregister(mAgentRecord.mAgent);
                mAgentRecord = null;
            }

            if (mAgentRecord == null) {
                mAgentRecord = new SecurityAgentRecord(agent, this);
                registered = true;
            }
        }
        return registered;
    }

    @Override
    public boolean unregister(final ISmsSecurityAgent agent) throws RemoteException {
        mContext.enforceCallingOrSelfPermission(permission.AUTHORIZE_OUTGOING_SMS, LOG_TAG);
        return doUnregisterSafe(agent);
    }

    @Override
    public boolean sendResponse(final SmsAuthorizationRequest request, final boolean authorized)
            throws RemoteException {
        mContext.enforceCallingOrSelfPermission(permission.AUTHORIZE_OUTGOING_SMS, LOG_TAG);
        final PendingRequestRecord record = mPendingRequests.remove(request.getToken());
        if (record != null) {
            record.invokeCallback(authorized);
        }
        return record != null;
    }

    public boolean requestAuthorization(final PackageInfo packageInfo,
            final String destinationAddress,
            final String message,
            final SmsSecurityServiceCallback callback,
            final Handler callbackHandler) {
        boolean requested = false;

        final SecurityAgentRecord record = mAgentRecord;
        if (record != null) {
            final IBinder token = new Binder();
            final SmsAuthorizationRequest request = new SmsAuthorizationRequest(this,
                    token, packageInfo.packageName, destinationAddress, message);

            final PendingRequestRecord requestRecord = new PendingRequestRecord(this, request,
                    callback, callbackHandler);
            mPendingRequests.put(token, requestRecord);

            requestRecord.scheduleTimeout(mTimeoutMs);
            try {
                record.mAgent.onAuthorize(request);
                requested = true;
            } catch (RemoteException e) {
                Rlog.e(LOG_TAG, "Unable to request SMS authentication.", e);
                mPendingRequests.remove(token);
                requestRecord.cancelTimeout();
            }
        }
        return requested;
    }

    protected void onRequestTimeout(final SmsAuthorizationRequest request) {
        final PendingRequestRecord record = mPendingRequests.remove(request.getToken());
        if (record != null) {
            record.invokeTimeout();
        }
    }

    private boolean doUnregisterSafe(final ISmsSecurityAgent agent) {
        boolean unregistered = false;
        synchronized (this) {
            if (mAgentRecord != null && mAgentRecord.mAgent.asBinder().equals(agent.asBinder())) {
                mAgentRecord.mAgent.asBinder().unlinkToDeath(mAgentRecord, 0);
                mAgentRecord = null;
                unregistered = true;
            }
        }
        return unregistered;
    }

    private static final class SecurityAgentRecord implements DeathRecipient {

        private final ISmsSecurityAgent mAgent;
        private final SmsSecurityService mService;

        public SecurityAgentRecord(final ISmsSecurityAgent agent,
                final SmsSecurityService monitor) throws RemoteException {
            mAgent = agent;
            mService = monitor;
            mAgent.asBinder().linkToDeath(this, 0);
        }

        @Override
        public void binderDied() {
            mService.doUnregisterSafe(mAgent);
        }
    }

    private static final class PendingRequestRecord {

        private final SmsSecurityServiceCallback mCallback;
        private final Handler mHandler;
        private final Runnable mTimeoutCallback;

        public PendingRequestRecord(final SmsSecurityService service,
                final SmsAuthorizationRequest request,
                final SmsSecurityServiceCallback callback,
                final Handler callbackHandler) {
            mCallback = callback;
            mHandler = callbackHandler;
            mTimeoutCallback = new Runnable() {
                @Override
                public void run() {
                    service.onRequestTimeout(request);
                }
            };
        }

        public void invokeCallback(boolean authorized) {
            cancelTimeout();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onAuthorizationResult(authorized);
                }
            });
        }

        public void invokeTimeout() {
            mCallback.onAuthorizationResult(true);
        }

        public void scheduleTimeout(final long delayMillis) {
            mHandler.postDelayed(mTimeoutCallback, delayMillis);
        }

        public void cancelTimeout() {
            mHandler.removeCallbacks(mTimeoutCallback);
        }
    }
}
