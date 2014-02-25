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

import android.telephony.Rlog;
import java.util.BitSet;
import android.telephony.ServiceState;

/**
 * {@hide}
 *
 * hardware configuration information reported by the ril layer and for
 * use by the telephone framework.
 *
 * the harward configuration is managerd by the TelephonyDevController.
 *
 * the telephony hardware resources are:
 *    - modem: physical entity providing acces technology.
 *    - sim: physicaly entity providing a slot interface.
 */
public class HardwareConfig {
   static final String LOG_TAG = "HardwareConfig";

   /**
    * hardware configuration kind.
    */
   public static final int DEV_HARDWARE_TYPE_MODEM = 0;
   public static final int DEV_HARDWARE_TYPE_SIM   = 1;
   /**
    * ril attachment model.  if single, there is a one-to-one
    * relationship between a modem hardware and a ril daemon.
    * if multiple, there is a one-to-many relatioship between a
    * modem hardware and several ril simultaneous ril daemons.
    */
   public static final int DEV_MODEM_RIL_MODEL_SINGLE   = 0;
   public static final int DEV_MODEM_RIL_MODEL_MULTIPLE = 1;
   /**
    * hardware state of the resource.  if enabled, it can
    * be used by the framework and active calls can be handled
    * on it.  if standby, it can be used by the framework but
    * only for non call related (eg reading the address book).
    * if disabled, it cannot be used and attempt to use it leads
    * to undetermined results.
    */
   public static final int DEV_HARDWARE_STATE_ENABLED  = 0;
   public static final int DEV_HARDWARE_STATE_STANDBY  = 1;
   public static final int DEV_HARDWARE_STATE_DISABLED = 2;

   /**
    * common hardware configuration.
    */
   public int type;     /* see DEV_HARDWARE_TYPE_ */
   public String uuid;  /* unique identifier for this hardware. */
   public int state;    /* see DEV_HARDWARE_STATE_ */

   /**
    * specific hardware configuration based on the type.
    */

   /**
    * DEV_HARDWARE_TYPE_MODEM.
    */
   public int rilModel; /* see DEV_MODEM_RIL_MODEL_ */
   public BitSet rat;   /* supported rat on this modem (BitSet of android.telephony.ServiceState). */
   public int maxActiveVoiceCall; /* maximum number of concurent active voice calls. */
   public int maxActiveDataCall;  /* maximum number of concurent active data calls. */
   public int maxStandby;         /* maximum number of concurent standby connections. */
   /**
    * DEV_HARDWARE_TYPE_SIM.
    */
   public String modemUuid; /* unique association to a modem for a sim. */

   /**
    * default constructor.
    */
   public HardwareConfig(int type) {
      type = type;
   }

   public void assignModem(String id, int state, int bs,
      int maxV, int maxD, int maxS) {
      if (type == DEV_HARDWARE_TYPE_MODEM) {
         uuid = id;
         state = state;
         rat = new BitSet();
         // TODO
         /* dodgy but no better elegant way just yet... */
         //for (int i = 0 ; i < 32 ; i++) {
         //   if ((1 << i) & bs) {
         //      rat.set(i);
         //   }
         //}
         maxActiveVoiceCall = maxV;
         maxActiveDataCall = maxD;
         maxStandby = maxS;
      }
   }

   public void assignSim(String id, int state, String link) {
      if (type == DEV_HARDWARE_TYPE_SIM) {
         uuid = id;
         modemUuid = link;
         state = state;
      }
   }

   public String toString() {
      StringBuilder builder = new StringBuilder();
      if (type == DEV_HARDWARE_TYPE_MODEM) {
         builder.append("Modem ");
         builder.append("{ uuid=" + uuid);
         builder.append(", state=" + state);
         builder.append(", rilModel=" + rilModel);
         builder.append(", rat=" + rat.toString());
         builder.append(", maxActiveVoiceCall=" + maxActiveVoiceCall);
         builder.append(", maxActiveDataCall=" + maxActiveDataCall);
         builder.append(", maxStandby=" + maxStandby);
         builder.append(" }");
      } else if (type == DEV_HARDWARE_TYPE_SIM) {
         builder.append("Sim ");
         builder.append("{ uuid=" + uuid);
         builder.append("{ modemUuid=" + modemUuid);
         builder.append(", state=" + state);
         builder.append(" }");
      } else {
         builder.append("Invalid Configration");
      }
      return builder.toString();
   }
};
