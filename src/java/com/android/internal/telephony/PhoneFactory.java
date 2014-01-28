/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.net.LocalServerSocket;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.cdma.CDMALTEPhone;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.SimInfoUpdater;
import com.android.internal.telephony.sip.SipPhone;
import com.android.internal.telephony.sip.SipPhoneFactory;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.SubscriptionController;

/**
 * {@hide}
 */
public class PhoneFactory {
    static final String LOG_TAG = "PhoneFactory";
    static final int SOCKET_OPEN_RETRY_MILLIS = 2 * 1000;
    static final int SOCKET_OPEN_MAX_RETRY = 3;

    //***** Class Variables
    
    static private Phone sProxyPhone[] = null;
    static private CommandsInterface[] sCommandsInterface = null;
    static private PhoneProxyManager sPhoneProxyManager = null;
    static private SimInfoUpdater sSimInfoUpdater = null;

    static private boolean sMadeDefaults = false;
    static private PhoneNotifier[] sPhoneNotifier;
    static private Looper sLooper;
    static private Context sContext;

    //***** Class Methods

    public static void makeDefaultPhones(Context context) {
        makeDefaultPhone(context);
    }

    /**
     * FIXME replace this with some other way of making these
     * instances
     */
    public static void makeDefaultPhone(Context context) {
        synchronized(Phone.class) {
            if (!sMadeDefaults) {
                sLooper = Looper.myLooper();
                sContext = context;

                if (sLooper == null) {
                    throw new RuntimeException(
                        "PhoneFactory.makeDefaultPhone must be called from Looper thread");
                }

                int retryCount = 0;
                for(;;) {
                    boolean hasException = false;
                    retryCount ++;

                    try {
                        // use UNIX domain socket to
                        // prevent subsequent initialization
                        new LocalServerSocket("com.android.internal.telephony");
                    } catch (java.io.IOException ex) {
                        hasException = true;
                    }

                    if ( !hasException ) {
                        break;
                    } else if (retryCount > SOCKET_OPEN_MAX_RETRY) {
                        throw new RuntimeException("PhoneFactory probably already running");
                    } else {
                        try {
                            Thread.sleep(SOCKET_OPEN_RETRY_MILLIS);
                        } catch (InterruptedException er) {
                        }
                    }
                }

                // Get preferred network mode
                int preferredNetworkMode = RILConstants.PREFERRED_NETWORK_MODE;
                if (TelephonyManager.getLteOnCdmaModeStatic() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    preferredNetworkMode = Phone.NT_MODE_GLOBAL;
                }

                int simCount = TelephonyManager.from(context).getSimCount();
                sPhoneNotifier = new DefaultPhoneNotifier[simCount];
                sCommandsInterface = new RIL[simCount];
                sProxyPhone = new PhoneProxy[simCount];

                Rlog.i(LOG_TAG, "simCount: " + Integer.toString(simCount));


                for (int i = 0; i < simCount; i++) {
                    sPhoneNotifier[i] = new DefaultPhoneNotifier(i);

                    String networkModeSettings = 
                        ((i == 0) ? Settings.Global.PREFERRED_NETWORK_MODE 
                        : (Settings.Global.PREFERRED_NETWORK_MODE + "_" + Integer.toString(i)));
                    int networkMode = Settings.Global.getInt(context.getContentResolver(),
                            networkModeSettings, preferredNetworkMode);
                    Rlog.i(LOG_TAG, "Network Mode set to " + networkModeSettings 
                        + ":" + Integer.toString(networkMode));

                    int cdmaSubscription = CdmaSubscriptionSourceManager.getDefault(context);
                    Rlog.i(LOG_TAG, "Cdma Subscription set to " + cdmaSubscription);

                    //reads the system properties and makes commandsinterface
                    sCommandsInterface[i] = new RIL(context, networkMode, cdmaSubscription, i);

                    // Instantiate UiccController so that all other classes can just call getInstance()
                    UiccController.make(context, sCommandsInterface[i], i);

                    int phoneType = TelephonyManager.getPhoneType(networkMode);
                    if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                        Rlog.i(LOG_TAG, "Creating GSMPhone, simId:" + Integer.toString(i));
                        sProxyPhone[i] = new PhoneProxy(new GSMPhone(context,
                                sCommandsInterface[i], sPhoneNotifier[i], i));
                    } else if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                        switch (TelephonyManager.getLteOnCdmaModeStatic()) {
                            case PhoneConstants.LTE_ON_CDMA_TRUE:
                                Rlog.i(LOG_TAG, "Creating CDMALTEPhone, simId:" + Integer.toString(i));
                                sProxyPhone[i] = new PhoneProxy(new CDMALTEPhone(context,
                                    sCommandsInterface[i], sPhoneNotifier[i], i));
                                break;
                            case PhoneConstants.LTE_ON_CDMA_FALSE:
                            default:
                                Rlog.i(LOG_TAG, "Creating CDMAPhone, simId:" + Integer.toString(i));
                                sProxyPhone[i] = new PhoneProxy(new CDMAPhone(context,
                                        sCommandsInterface[i], sPhoneNotifier[i], i));
                                break;
                        }
                    }
                }

                sPhoneProxyManager = PhoneProxyManager.getDefault();
                sPhoneProxyManager.setPhoneProxys(sProxyPhone);
                Rlog.i(LOG_TAG, "Set PhoneProxys to PhoneProxyManager, simCount: " + Integer.toString(simCount));

                SubscriptionController.init(PhoneFactory.getPhoneProxyManager().getPhoneProxy(TelephonyManager.getDefaultSim()));

                Rlog.i(LOG_TAG, "Creating SimInfoUpdater");
                sSimInfoUpdater = new SimInfoUpdater();

                // Ensure that we have a default SMS app. Requesting the app with
                // updateIfNeeded set to true is enough to configure a default SMS app.
                ComponentName componentName =
                        SmsApplication.getDefaultSmsApplication(context, true /* updateIfNeeded */);
                String packageName = "NONE";
                if (componentName != null) {
                    packageName = componentName.getPackageName();
                }
                Rlog.i(LOG_TAG, "defaultSmsApplication: " + packageName);

                // Set up monitor to watch for changes to SMS packages
                SmsApplication.initSmsPackageMonitor(context);

                sMadeDefaults = true;
            }
        }
    }

    public static Phone getDefaultPhone() {
        if (sLooper != Looper.myLooper()) {
            throw new RuntimeException(
                "PhoneFactory.getDefaultPhone must be called from Looper thread");
        }

        if (!sMadeDefaults) {
            throw new IllegalStateException("Default phones haven't been made yet!");
        }

        int defaultSim = TelephonyManager.getDefault().getDefaultSim();
        return sProxyPhone[defaultSim];
    }

    public static Phone getCdmaPhone() {
        Phone phone;
        int defaultSim = TelephonyManager.getDefault().getDefaultSim();
        synchronized(PhoneProxy.lockForRadioTechnologyChange) {
            switch (TelephonyManager.getLteOnCdmaModeStatic()) {
                case PhoneConstants.LTE_ON_CDMA_TRUE: {
                    phone = new CDMALTEPhone(sContext, sCommandsInterface[defaultSim], sPhoneNotifier[defaultSim]);
                    break;
                }
                case PhoneConstants.LTE_ON_CDMA_FALSE:
                case PhoneConstants.LTE_ON_CDMA_UNKNOWN:
                default: {
                    phone = new CDMAPhone(sContext, sCommandsInterface[defaultSim], sPhoneNotifier[defaultSim]);
                    break;
                }
            }
        }
        return phone;
    }

    public static Phone getGsmPhone() {
        synchronized(PhoneProxy.lockForRadioTechnologyChange) {
            int defaultSim = TelephonyManager.getDefault().getDefaultSim();
            Phone phone = new GSMPhone(sContext, sCommandsInterface[defaultSim], sPhoneNotifier[defaultSim]);
            return phone;
        }
    }

    /**
     * Makes a {@link SipPhone} object.
     * @param sipUri the local SIP URI the phone runs on
     * @return the {@code SipPhone} object or null if the SIP URI is not valid
     */
    public static SipPhone makeSipPhone(String sipUri) {
        int defaultSim = TelephonyManager.getDefault().getDefaultSim();
        return SipPhoneFactory.makePhone(sipUri, sContext, sPhoneNotifier[defaultSim]);
    }

    public static PhoneProxyManager getPhoneProxyManager() {
        Rlog.i(LOG_TAG, "getPhoneProxyManager");
        return ((sPhoneProxyManager == null) ? PhoneProxyManager.getDefault():sPhoneProxyManager);
    }
}
