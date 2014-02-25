/*
 * Copyright (C) 2014 The Android Open Source Project
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

import com.android.internal.telephony.RIL;
import android.telephony.TelephonyManager;

import android.os.AsyncResult;
import android.content.Context;
import android.telephony.Rlog;
import java.util.BitSet;
import java.util.List;
import java.util.ArrayList;
import android.text.TextUtils;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;

/**
 * TelephonyDevController - provides a unified view of the
 * telephony hardware resources on a device.
 *
 * the telephony hardware resources are:
 *    - modem: physical entity providing acces technology.
 *    - sim: physicaly entity providing a slot interface.
 */
public class TelephonyDevController extends Handler {
   private static final String LOG_TAG = "TelephonyDevController";
   private static final boolean DBG = true;
   private static final boolean DBG_DATA = true;
   private static final Object mLock = new Object();
   private Context mContext;
   private CommandsInterface mCi;

   private static final int EVENT_HARDWARE_CONFIG_CHANGED = 1;

   /** hardware state of the modem resource.  if enabled, it can
    * be used by the framework, if disbaled it can't and attempt
    * to use it leads to undertermined results.
    */
   public static final int DEV_MODEM_STATE_ENABLED  = 0;
   public static final int DEV_MODEM_STATE_DISABLED = 1;
   /** ril attachment model.  if single, there is a one-to-one
    * relationship between a modem hwardware and a ril daemon.
    * if multiple, there is a one-to-many relatioship between a
    * modem hardware and several ril simultaneous ril daemons.
    */
   public static final int DEV_MODEM_RIL_MODEL_SINGLE   = 0;
   public static final int DEV_MODEM_RIL_MODEL_MULTIPLE = 1;
   /** radio access technology (rat) supported by a modem
    * hardware.  this is to allow the framework to get a proper
    * hint of the capabilities of the modem to make educated
    * decision when selecting modem for a functionality.
    *
    * note: those are indexes in a BitSet.  use BitSet API to
    * get the proper information.
    *
    * see also RIL_RadioTechnology in include/telephony/ril.h
    */
   public static final int DEV_MODEM_RADIO_TECH_GPRS   = 1;
   public static final int DEV_MODEM_RADIO_TECH_EDGE   = 2;
   public static final int DEV_MODEM_RADIO_TECH_UMTS   = 3;
   public static final int DEV_MODEM_RADIO_TECH_IS95A  = 4;
   public static final int DEV_MODEM_RADIO_TECH_IS95B  = 5;
   public static final int DEV_MODEM_RADIO_TECH_1xRTT  = 6;
   public static final int DEV_MODEM_RADIO_TECH_EVDO_0 = 7;
   public static final int DEV_MODEM_RADIO_TECH_EVDO_A = 8;
   public static final int DEV_MODEM_RADIO_TECH_HSDPA  = 9;
   public static final int DEV_MODEM_RADIO_TECH_HSUPA  = 10;
   public static final int DEV_MODEM_RADIO_TECH_HSPA   = 11;
   public static final int DEV_MODEM_RADIO_TECH_EVDO_B = 12;
   public static final int DEV_MODEM_RADIO_TECH_EHRPD  = 13;
   public static final int DEV_MODEM_RADIO_TECH_LTE    = 14;
   public static final int DEV_MODEM_RADIO_TECH_HSPAP  = 15;
   public static final int DEV_MODEM_RADIO_TECH_GSM    = 16;

   /** hardware state of the sim resource.  if enabled, it can
    * be used by the framework and active calls can be handled
    * on it.  if standby, it can be used by the framework but
    * only for non call related (eg reading the address book).
    * if disabled, it cannot be used and attempt to use it leads
    * to undetermined results.
    */
   public static final int DEV_SIM_STATE_ENABLED  = 0;
   public static final int DEV_SIM_STATE_STANDBY  = 1;
   public static final int DEV_SIM_STATE_DISABLED = 2;

   /** hardware - modem device.
    */
   public class TelephonyDevModem {
      private TelephonyDevModem() {
         uuid = null;
         rat = new BitSet();
      }

      private TelephonyDevModem(String uuid, int state, int rilModel, BitSet bs,
         int activeVoice, int activeData, int standby) {
         uuid = uuid;
         state = state;
         rilModel = rilModel;
         rat = new BitSet();
         rat.and(bs);
         maxActiveVoiceCall = activeVoice;
         maxActiveDataCall = activeData;
         maxStandby = standby;
      }

      public String uuid;  /* unique identifier for this modem. */
      public int state;    /* see DEV_MODEM_STATE_ */
      public int rilModel; /* see DEV_MODEM_RIL_MODEL_ */
      public BitSet rat;   /* supported rat on this modem. */

      public int maxActiveVoiceCall; /* maximum number of concurent active voice calls. */
      public int maxActiveDataCall;  /* maximum number of concurent active data calls. */
      public int maxStandby;         /* maximum number of concurent standby connections. */

      public String toString() {
         StringBuilder builder = new StringBuilder();
         builder.append("Modem ");
         builder.append("{ uuid=" + uuid);
         builder.append(", state=" + state);
         builder.append(", rilModel=" + rilModel);
         builder.append(", rat=" + rat.toString());
         builder.append(", maxActiveVoiceCall=" + maxActiveVoiceCall);
         builder.append(", maxActiveDataCall=" + maxActiveDataCall);
         builder.append(", maxStandby=" + maxStandby);
         builder.append(" }");
         return builder.toString();
      }
   }

   /** hardware - sim device.
    */
   public class TelephonyDevSim {
      private TelephonyDevSim() {
         uuid = null;
         modemUuid = null;
      }

      private TelephonyDevSim(String uuid, String modemUuid, int state) {
         uuid = uuid;
         modemUuid = modemUuid;
         state = state;
      }

      public String uuid;      /* unique identifier for this sim. */
      public String modemUuid; /* unique association to a modem for this sim. */
      public int state;        /* see DEV_SIM_STATE_ */

      public String toString() {
         StringBuilder builder = new StringBuilder();
         builder.append("Sim ");
         builder.append("{ uuid=" + uuid);
         builder.append("{ modemUuid=" + modemUuid);
         builder.append(", state=" + state);
         builder.append(" }");
         return builder.toString();
      }
   }

   private static TelephonyDevController sTelephonyDevController;
   private ArrayList<TelephonyDevModem> mModems = new ArrayList<TelephonyDevModem>();
   private ArrayList<TelephonyDevSim> mSims = new ArrayList<TelephonyDevSim>();

   public static TelephonyDevController create(Context context, CommandsInterface cmdsIf) {
      synchronized (mLock) {
         if (sTelephonyDevController != null) {
            throw new RuntimeException("TelephonyDevController already created!?!");
         }
         sTelephonyDevController = new TelephonyDevController(context, cmdsIf);
         return sTelephonyDevController;
      }
   }

   public static TelephonyDevController getInstance() {
      synchronized (mLock) {
         if (sTelephonyDevController == null) {
            throw new RuntimeException("TelephonyDevController not yet created!?!");
         }
         return sTelephonyDevController;
      }
   }

   private void addCanned() {
      BitSet rat = new BitSet();
      rat.set(DEV_MODEM_RADIO_TECH_HSPAP);
      rat.set(DEV_MODEM_RADIO_TECH_GSM);

      mModems.add(new TelephonyDevModem("cannedModem",
         DEV_MODEM_STATE_ENABLED,
         DEV_MODEM_RIL_MODEL_SINGLE,
         rat, 1 /*voice*/, 1 /*data*/, 1 /*standby*/));

      mSims.add(new TelephonyDevSim("cannedSIM",
         "cannedModem",
         DEV_SIM_STATE_ENABLED));
   }

   private TelephonyDevController(Context context, CommandsInterface cmdsIf) {
      if (DBG) Rlog.d(LOG_TAG, "contructor");

      mContext = context;
      mCi = cmdsIf;

      /* debug - add canned modem+sim.
       */
      if (DBG_DATA) {
         addCanned();
      }

      /* create initial database and content if needed, or populate
       * from existig one.
       */


      /* register with ril for async device configuration change. */
      mCi.registerForHardwareConfigChanged(this, EVENT_HARDWARE_CONFIG_CHANGED, null);

      mModems.trimToSize();
      mSims.trimToSize();
   }

   /** handle callbacks from RIL.
    */
   public void handleMessage(Message msg) {
      AsyncResult ar;
      switch (msg.what) {
         case EVENT_HARDWARE_CONFIG_CHANGED:
            if (DBG) Rlog.d(LOG_TAG, "received EVENT_HARDWARE_CONFIG_CHANGED");
               ar = (AsyncResult) msg.obj;
               handleGetHardwareConfigChanged(ar);
            break;
         default:
            Rlog.e(LOG_TAG, " Unknown Event " + msg.what);
      }
   }

   /** hardware configuration changed.
    */
   private void handleGetHardwareConfigChanged(AsyncResult ar) {
      if ((ar.exception == null) && (ar.result != null)) {
         // TODO
      }
   }

   /** get total number of registered modem.
    */
   public int getModemCount() {
      synchronized (mLock) {
         return mModems.size();
      }
   }

   /** get modem at index 'index'.
    */
   public TelephonyDevModem getModem(int index) {
      synchronized (mLock) {
         if (mModems.isEmpty()) {
            Rlog.e(LOG_TAG, "no registered modem device?!?");
            return null;
         }

         if (index > getModemCount()) {
            Rlog.e(LOG_TAG, "out-of-bounds access for modem device " + index + " max: " + getModemCount());
            return null;
         }

         if (DBG) Rlog.d(LOG_TAG, "getModem " + index);
         return mModems.get(index);
      }
   }

   /** get total number of registered sims.
    */
   public int getSimCount() {
      synchronized (mLock) {
         return mSims.size();
      }
   }

   /** get sim at index 'index'.
    */
   public TelephonyDevSim getSim(int index) {
      synchronized (mLock) {
         if (mSims.isEmpty()) {
            Rlog.e(LOG_TAG, "no registered sim device?!?");
            return null;
         }

         if (index > getSimCount()) {
            Rlog.e(LOG_TAG, "out-of-bounds access for sim device " + index + " max: " + getSimCount());
            return null;
         }

         if (DBG) Rlog.d(LOG_TAG, "getSim " + index);
            return mSims.get(index);
      }
   }

   /** get modem associated with sim index 'simIndex'.
    */
   public TelephonyDevModem getModemForSim(int simIndex) {
      synchronized (mLock) {
         if (mModems.isEmpty() || mSims.isEmpty()) {
            Rlog.e(LOG_TAG, "no registered modem/sim device?!?");
            return null;
         }

         if (simIndex > getSimCount()) {
            Rlog.e(LOG_TAG, "out-of-bounds access for sim device " + simIndex + " max: " + getSimCount());
            return null;
         }

         if (DBG) Rlog.d(LOG_TAG, "getModemForSim " + simIndex);

         TelephonyDevSim sim = getSim(simIndex);
         for (TelephonyDevModem modem: mModems) {
            if (modem.uuid.equals(sim.modemUuid)) {
               return modem;
            }
         }

         return null;
      }
   }

   /** get all sim's associated with modem at index 'modemIndex'.
    */
   public ArrayList<TelephonyDevSim> getAllSimsForModem(int modemIndex) {
      synchronized (mLock) {
         if (mSims.isEmpty()) {
            Rlog.e(LOG_TAG, "no registered sim device?!?");
            return null;
         }

         if (modemIndex > getModemCount()) {
            Rlog.e(LOG_TAG, "out-of-bounds access for modem device " + modemIndex + " max: " + getModemCount());
            return null;
         }

         if (DBG) Rlog.d(LOG_TAG, "getAllSimsForModem " + modemIndex);

         ArrayList<TelephonyDevSim> result = new ArrayList<TelephonyDevSim>();
         TelephonyDevModem modem = getModem(modemIndex);
         for (TelephonyDevSim sim: mSims) {
            if (sim.modemUuid.equals(modem.uuid)) {
               result.add(sim);
            }
         }
         return result;
      }
   }

   /** get all modem's registered.
    */
   public ArrayList<TelephonyDevModem> getAllModems() {
      synchronized (mLock) {
         if (mModems.isEmpty()) {
            Rlog.e(LOG_TAG, "no registered modem device?!?");
            return null;
         }

         return mModems;
      }
   }

   /** get all sim's registered.
    */
   public ArrayList<TelephonyDevSim> getAllSims() {
      synchronized (mLock) {
         if (mSims.isEmpty()) {
            Rlog.e(LOG_TAG, "no registered sim device?!?");
            return null;
         }

         return mSims;
      }
   }
}
