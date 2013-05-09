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

import android.os.Handler;

/**
 * Register for notifications from Telephony FW.
 */
public interface NotificationRegister {

    public void registerForPhoneState(Handler h, int what, Object obj);

    public void registerForServiceState(Handler h, int what, Object obj);

    public void registerForCellLocation(Handler h, int what, Object obj);

    public void registerForSignalStrength(Handler h, int what, Object obj);

    public void registerForMessageWaitingChanged(Handler h, int what, Object obj);

    public void registerForCallForwardingChanged(Handler h, int what, Object obj);

    public void registerForDataConnection(Handler h, int what, Object obj);

    public void unregisterForServiceState(Handler h);

    public void registerForDataConnectionFailed(Handler h, int what, Object obj);

    public void registerForDataActivity(Handler h, int what, Object obj);

}
