package com.android.internal.telephony;

import android.content.Context;
import android.media.AudioManager;
import android.os.AsyncResult;
import android.telephony.Rlog;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.sip.SipPhone;
import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.cdma.CdmaMmiCode;
import com.android.internal.telephony.CallManager;
import android.telephony.ServiceState;

import android.telephony.PhoneNumberUtils;

import java.util.ArrayList;
/// [SS related]. @{
import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.cdma.CdmaMmiCode;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
/// @}

public final class CallManagerHelper {

    private static final String LOG_TAG ="CallManager";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    CallManager mCallManager;

    CallManagerHelper (CallManager callmgr) {
        mCallManager = callmgr;
    }

    public AudioManager getAudioManager() {
        
        Context context;
        Phone defaultPhone = mCallManager.getDefaultPhone();
        if(defaultPhone == null) 
            return null;
        else
            context = defaultPhone.getContext();
        
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager;
    }

    /**
     * Check if the current service state is power off.
     * @return true for power off, else return false.
     */
    public boolean isServiceStatePowerOff(ArrayList<Phone> phones) {
        boolean bIsPowerOff = true;
    
        for (Phone phone : phones) {
            bIsPowerOff = (phone.getServiceState().getState() == ServiceState.STATE_POWER_OFF);
            if (!bIsPowerOff) {
                break;
            }
        }
    
        Rlog.d(LOG_TAG, "[isServiceStatePowerOff]bIsPowerOff = " + bIsPowerOff);
        return bIsPowerOff;
    }   

    /// [SS related]. @{
    
    // check if the dial string is CRSS string.
    // @param dialString dial string which may contain CRSS string.    
    public boolean isInCallMmiCommands(String dialString) {
        boolean result = false;
        char ch = dialString.charAt(0);

        switch (ch) {
            case '0':
            case '3':
            case '4':
            case '5':
                if (dialString.length() == 1) {
                    result = true;
                }
                break;

            case '1':
            case '2':
                if (dialString.length() == 1 || dialString.length() == 2) {
                    result = true;
                }
                break;

            default:
                break;
        }

        return result;
    } 

    public boolean isUssdNumber(Phone phone, String dialString) {

        boolean isUssdNumber = false;
        
        if(phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
            UiccCardApplication cardApp = UiccController.getInstance(phone.getSimId()).getUiccCardApplication(UiccController.APP_FAM_3GPP);
            Rlog.d(LOG_TAG, "[UiccCardApplication]cardApp = " + cardApp);

            String newDialString = PhoneNumberUtils.stripSeparators(dialString);
            String networkPortion = PhoneNumberUtils.extractNetworkPortionAlt(newDialString);
            GsmMmiCode gsmMmiCode = GsmMmiCode.newFromDialString(networkPortion, (GSMPhone)phone, cardApp);
            
            if (gsmMmiCode == null || gsmMmiCode.isTemporaryModeCLIR()) {
                isUssdNumber = false;
            }
            else {
                isUssdNumber = true;
            }                
        } else if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            UiccCardApplication cardApp = UiccController.getInstance(phone.getSimId()).getUiccCardApplication(UiccController.APP_FAM_3GPP2);
            CdmaMmiCode cdmaMmiCode = CdmaMmiCode.newFromDialString(dialString, (CDMAPhone)phone, cardApp);
            if (cdmaMmiCode != null) {
                isUssdNumber = cdmaMmiCode.isUssdRequest();
            }
        }     

        return isUssdNumber;
    }    
    /// @}    

}


