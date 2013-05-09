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

package android.telephony.test.util;

import android.content.Context;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.util.Log;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.DummyCommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.uicc.UiccController;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class TelephonyTestUtil {

    private static final String LOG_TAG = "TelephonyTestUtil";

    private static final String FIELD_INSTANCE = "mInstance";

    private static PhoneNotifier notifier = new TestNotifier();
    private static PhoneBase mPhone = null;
    private static PhoneBase mGSMPhone = null;

    public static Object getField(Object clazz, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(clazz);
    }

    public static Object getStaticField(Class<?> clazz, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(clazz);
    }

    public static Object getSuperField(Object clazz, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(clazz);
    }

    public static void setField(Object clazz, String fieldName, Object object)
            throws NoSuchFieldException, IllegalAccessException {
        Field fState = clazz.getClass().getDeclaredField(fieldName);
        fState.setAccessible(true);
        fState.set(clazz, object);
    }

    public static void setStaticField(Class<?> clazz, String fieldName, Object object)
            throws NoSuchFieldException, IllegalAccessException {
        Field fState = clazz.getDeclaredField(fieldName);
        fState.setAccessible(true);
        fState.set(clazz, object);
    }

    public static void setSuperField(Object clazz, String fieldName, Object object)
            throws NoSuchFieldException, IllegalAccessException {
        Field fState = clazz.getClass().getSuperclass().getDeclaredField(fieldName);
        fState.setAccessible(true);
        fState.set(clazz, object);
    }

    public static Object invokeMethod(Object clazz, String methodName)
            throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        Method m = clazz.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        return m.invoke(clazz);
    }

    public static Object invokeStaticMethod(Class<?> clazz, String methodName)
            throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        Method m = clazz.getDeclaredMethod(methodName);
        m.setAccessible(true);
        return m.invoke(clazz);
    }

    public static Object invokeSuperMethod(Object clazz, String methodName)
            throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        Method m = clazz.getClass().getSuperclass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        return m.invoke(clazz);
    }

    public static Object invokeMethod(Object clazz, String methodName, Class<?>[] parameterTypes,
            Object[] args)
            throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        Method m = clazz.getClass().getDeclaredMethod(methodName, parameterTypes);
        m.setAccessible(true);
        return m.invoke(clazz, args);
    }

    public static Object invokeSuperMethod(Object clazz, String methodName,
            Class<?>[] parameterTypes, Object[] args)
            throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        Method m = clazz.getClass().getSuperclass().getDeclaredMethod(methodName, parameterTypes);
        m.setAccessible(true);
        return m.invoke(clazz, args);
    }


    public static PhoneBase getCDMAPhone(Context context) {
        if(mPhone != null) {
            return mPhone;
        }

        CommandsInterface cm = getRil(context);
        try {
            if (TelephonyTestUtil.getStaticField(UiccController.class, FIELD_INSTANCE) != null) {
                setStaticField(UiccController.class, FIELD_INSTANCE, null);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception occurred at UiccController.mInstance" + e);
            return null;
        }
        UiccController.make(context, cm);
        PhoneBase phone = new CDMAPhone(context, cm, notifier);

        mPhone = phone;
        return mPhone;
    }

    public static PhoneBase getGSMPhone(Context context) {
        if (mGSMPhone != null) {
            return mGSMPhone;
        }

        CommandsInterface cm = getRil(context);
        try {
            if (TelephonyTestUtil.getStaticField(UiccController.class, FIELD_INSTANCE) != null) {
                setStaticField(UiccController.class, FIELD_INSTANCE, null);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception occurred at UiccController.mInstance" + e);
            return null;
        }
        UiccController.make(context, cm);
        PhoneBase phone = new GSMPhone(context, cm, notifier);
        mGSMPhone = phone;
        return mGSMPhone;
    }

    /*
     * If 2nd connection request is sent to rild, the system crash occurs.
     * Since Telephony FW already created the connection to rild,
     * a new connection to rild cannot be requested from the Phone's test code.
     * Therefore the test code has to use Stub instead of RIL.
     */
    private static CommandsInterface getRil(Context context) {
        return (new DummyCommandsInterface());
    }

    private static class TestNotifier implements PhoneNotifier {
        public void notifyPhoneState(Phone sender) {
        }

        public void notifyServiceState(Phone sender) {
        }

        public void notifyCellLocation(Phone sender) {
        }

        public void notifySignalStrength(Phone sender) {
        }

        public void notifyMessageWaitingChanged(Phone sender) {
        }

        public void notifyCallForwardingChanged(Phone sender) {
        }

        public void notifyDataConnection(Phone sender, String reason, String apnType,
                PhoneConstants.DataState state) {
        }

        public void notifyDataConnectionFailed(Phone sender, String reason, String apnType) {
        }

        public void notifyDataActivity(Phone sender) {
        }

        public void notifyOtaspChanged(Phone sender, int otaspMode) {
        }

        public void notifyCellInfo(Phone sender, List<CellInfo> cellInfo) {
        }
    }
}
