/*
 * Copyright (C) 2012-2013 Motorola Mobility LLC All Rights Reserved.
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

import android.telephony.SignalStrength;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Xml;
import android.util.Log;
import android.util.Config;
import com.android.internal.util.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

/**
 * SignalLevelManager loads the SignalLevel file definitions and manages it.
 */
public class SignalLevelManager {
    private static final String TAG = "RIL_SignalLevelManager";
    private static final boolean DBG = true; //Config.DEBUG;

    private static final int RADIOTECH_NUM = 5; // Support all tech signal level

    private static final int RADIOTECH_CDMA = 0;
    private static final int RADIOTECH_EVDO = 1;
    private static final int RADIOTECH_LTE = 2;
    private static final int RADIOTECH_GSM = 3;
    private static final int RADIOTECH_UMTS = 4;

    public static final int SL_FROM_XML          = 0;
    public static final int SL_FROM_FILE_SYSTEM  = 1;

    private int mSLFileSource = SL_FROM_XML;
    private SignalLevelFile mSLFile;
    private boolean isSLFileLoaded = false;
    private int mNumberOfBars = 0;
    private static Context mContext;
    private static SignalLevelManager mInstance;

    // for sprint, which do not use ecio as level map
    private static boolean mUseEVDOecio = false;

    class SignalLevelInfo {

        public int mSignalBars;
        public int[] mAsuLevel = null;
        public int[] mSigStrLevel = null;
        public int[] mEcSnrLevel = null;

        public SignalLevelInfo (int signalBars, String asuLevel,
                String sigStrLevel, String ecSnrLevel) {
            this.mSignalBars = signalBars;
            if (asuLevel != null && asuLevel.length() > 0)
            this.mAsuLevel = SignalLevelManager.parseSignalLevelString(
                    signalBars, asuLevel);
            if (sigStrLevel != null && sigStrLevel.length() > 0)
            this.mSigStrLevel = SignalLevelManager.parseSignalLevelString(
                    signalBars, sigStrLevel);
            if (ecSnrLevel != null && ecSnrLevel.length() > 0)
            this.mEcSnrLevel = SignalLevelManager.parseSignalLevelString(
                    signalBars, ecSnrLevel);
        }
    }

    class SignalLevelFile {

        public int [] mNumberOfSignalEntries;
        public HashMap<Integer, SignalLevelInfo> [] mSignalInfoTable;

        public SignalLevelFile() {
            this.mNumberOfSignalEntries = new int[RADIOTECH_NUM];
            this.mSignalInfoTable = new HashMap[RADIOTECH_NUM];
            for (int i = 0; i < RADIOTECH_NUM; i++) {
                this.mNumberOfSignalEntries[i] = 0;
                this.mSignalInfoTable[i] = new HashMap<Integer, SignalLevelInfo>();
            }
        }
    }

    private SignalLevelManager(Context c) {
        mContext = c;
        if (isSLFileLoaded == false) {
            mSLFile = new SignalLevelFile();
            loadSignalLevelFile();
            isSLFileLoaded = true;
        }
    }

    public static SignalLevelManager getInstance(Context c) {
        if (mInstance == null && c != null) {
            mInstance = new SignalLevelManager(c);
        } else if (c != null){
            mContext  = c;
        }
        return mInstance;
    }

    public static SignalLevelManager getInstance() {
        return mInstance;
    }

    /**
     * Parse XML signal level strings to signal level threshold
     */
    public static int[] parseSignalLevelString(int signalBars, String signalLevelString) {
        int[] signalLevel = new int[signalBars];
        String[] splitLevel = signalLevelString.split(",");
        if (splitLevel.length < signalBars) {
            Log.e(TAG, "Error Parsing SignalLevelFile: signalBars: " +
                    signalBars+ " has " + splitLevel.length  + " element.");
            return null;
        } else {
            for (int i = 0; i < signalBars; i++) {
                signalLevel[i] = Integer.parseInt(splitLevel[i]);
            }
        }
        return signalLevel;
    }

    /**
     * Parse RadioType string to enums
     */
    private int parseRadioType(String RadioTypeString) {
        int radioType = 0;
        if ("GSM".equals(RadioTypeString)) {
            radioType = RADIOTECH_GSM;
        } else if ("UMTS".equals(RadioTypeString)) {
            radioType = RADIOTECH_UMTS;
        } else if ("CDMA".equals(RadioTypeString)) {
            radioType = RADIOTECH_CDMA;
        } else if ("EVDO".equals(RadioTypeString)) {
            radioType = RADIOTECH_EVDO;
        }else if ("LTE".equals(RadioTypeString)) {
            radioType = RADIOTECH_LTE;
        }
        return radioType;
    }

    private void loadSignalLevelFile() {
        switch (mSLFileSource) {
        case SL_FROM_FILE_SYSTEM:
            loadSignalLevelFileFromFileSystem();
            break;

        case SL_FROM_XML:
        default:
            loadSignalLevelFileFromXml();
            break;
        }
    }

    /**
     * Load the SignalLevel file from a File System file
     *
     * In this case the a Phone Support Tool to update the SignalLevel file must be provided
     */
    private void loadSignalLevelFileFromFileSystem() {
        // NOT IMPLEMENTED, Chipset vendor/Operator specific
    }

    /**
     * Load the SignalLevel file from the application framework resources encoded in XML
     */
    private void loadSignalLevelFileFromXml() {
        FileInputStream stream = null;
        Resources r = mContext.getResources();
        XmlPullParser parser= r.getXml(com.android.internal.R.xml.signal_level);
        int numberOfSignalEntries = 0;
        int parsedSignalEntries = 0;
        String numberOfBarsString;

        try {
            XmlUtils.beginDocument(parser, "SignalLevelFile");

            numberOfBarsString = parser.getAttributeValue(null, "NumberOfBars");
            if ("auto".equalsIgnoreCase(numberOfBarsString)) {
                // Signal levels are "auto", but value unknown until radio becomes active.
                mNumberOfBars = 0;
            } else {
                try {
                    mNumberOfBars = Integer.parseInt(numberOfBarsString);
                } catch (NumberFormatException e) {
                    mNumberOfBars = 0;
                }
            }

            numberOfSignalEntries = Integer.parseInt(
                    parser.getAttributeValue(null, "NumberOfSignalEntries"));

            while (true) {
                XmlUtils.nextElement(parser);
                String infoName = parser.getName();
                if (infoName == null) {
                    if (parsedSignalEntries != numberOfSignalEntries)
                        Log.e(TAG, "Error Parsing SignalLevel File: SignalLevelInfo "
                                + numberOfSignalEntries + " defined, "
                                + parsedSignalEntries);
                    break;
                } else if (infoName.equals("SignalLevelInfo")) {
                    int radioTech = parseRadioType(parser.getAttributeValue(null, "RadioType"));
                    int slInfoBars = Integer.parseInt
                            (parser.getAttributeValue(null, "SignalBars"));
                    if (mNumberOfBars == 0 || mNumberOfBars == slInfoBars) {
                        //Parse singal level for each network type
                        if (radioTech == RADIOTECH_GSM) {
                            String asuLevel = parser.getAttributeValue(null, "AsuLevel");
                            mSLFile.mSignalInfoTable[RADIOTECH_GSM].put(slInfoBars,
                                    new SignalLevelInfo(slInfoBars, asuLevel, null, null));
                            mSLFile.mNumberOfSignalEntries[RADIOTECH_GSM]++;
                        } else if (radioTech == RADIOTECH_UMTS) {
                            String asuLevel = parser.getAttributeValue(null, "AsuLevel");
                            String rscpLevel = parser.getAttributeValue(null, "RscpLevel");
                            String ecnoLevel = parser.getAttributeValue(null, "EcnoLevel");
                            mSLFile.mSignalInfoTable[RADIOTECH_UMTS].put(slInfoBars,
                                    new SignalLevelInfo(slInfoBars, asuLevel, rscpLevel, ecnoLevel));
                            mSLFile.mNumberOfSignalEntries[RADIOTECH_UMTS]++;
                        } else if (radioTech == RADIOTECH_CDMA) {
                            String dbmLevel = parser.getAttributeValue(null, "DbmLevel");
                            String ecioLevel = parser.getAttributeValue(null, "EcioLevel");
                            mSLFile.mSignalInfoTable[RADIOTECH_CDMA].put(slInfoBars,
                                    new SignalLevelInfo(slInfoBars, null, dbmLevel, ecioLevel));
                            mSLFile.mNumberOfSignalEntries[RADIOTECH_CDMA]++;
                        } else if (radioTech == RADIOTECH_EVDO) {
                            String dbmLevel = parser.getAttributeValue(null, "DbmLevel");
                            String snrLevel = parser.getAttributeValue(null, "SnrLevel");

                            if (snrLevel != null) {
                                mSLFile.mSignalInfoTable[RADIOTECH_EVDO].put(slInfoBars,
                                        new SignalLevelInfo(slInfoBars, null, dbmLevel, snrLevel));
                                mSLFile.mNumberOfSignalEntries[RADIOTECH_EVDO]++;
                            } else {
                                String ecioLevel = parser.getAttributeValue(null, "EcioLevel");
                                if (ecioLevel != null) {
                                    mUseEVDOecio = true;
                                    mSLFile.mSignalInfoTable[RADIOTECH_EVDO].put(slInfoBars,
                                    new SignalLevelInfo(slInfoBars, null, dbmLevel, ecioLevel));
                                    mSLFile.mNumberOfSignalEntries[RADIOTECH_EVDO]++;
                                }
                            }
                        } else if (radioTech == RADIOTECH_LTE) {
                            String rssiLevel = parser.getAttributeValue(null, "RssiLevel");
                            String rsrpLevel = parser.getAttributeValue(null, "RsrpLevel");
                            String snrLevel = parser.getAttributeValue(null, "SnrLevel");
                            mSLFile.mSignalInfoTable[RADIOTECH_LTE].put(slInfoBars,
                                    new SignalLevelInfo(slInfoBars, rssiLevel, rsrpLevel, snrLevel));
                            mSLFile.mNumberOfSignalEntries[RADIOTECH_LTE]++;
                        }
                    }
                    parsedSignalEntries++;
                }
            }
            if (DBG) Log.d(TAG, "loadSignalLevelXml: SignalBar:" +
                    "GSM " + mSLFile.mNumberOfSignalEntries[RADIOTECH_GSM] +
                    " loaded. UMTS " + mSLFile.mNumberOfSignalEntries[RADIOTECH_UMTS] +
                    " loaded. CDMA " + mSLFile.mNumberOfSignalEntries[RADIOTECH_CDMA] +
                    " loaded. EVDO " + mSLFile.mNumberOfSignalEntries[RADIOTECH_EVDO] +
                    " loaded. LTE " + mSLFile.mNumberOfSignalEntries[RADIOTECH_LTE] +
                    " loaded. Total " + parsedSignalEntries + " found." +
                    " NumberOfBars set to "+ mNumberOfBars);
        } catch (Exception e) {
            Log.e(TAG, "Got exception while loading SignalLevel file.", e);
        } finally {
            if (parser instanceof XmlResourceParser) {
                ((XmlResourceParser)parser).close();
            }
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Map signal strength to signal level
     */
    private int getSignalLevelNum(int numBars, int sigStrength, int[] sigLevel) {
        int signalLevel = 0;

        if (sigStrength < sigLevel[0]) {
            signalLevel = 0;
        } else {
            for (int i = numBars - 1; i >= 0; i--) {
                if (sigStrength >= sigLevel[i]) {
                    signalLevel = i + 1;
                    break;
                }
            }
        }
        return signalLevel;
    }

    /**
     * Map GSM/UMTS parameters to signal level
     */
    public int get3GPPSignalLevel(int numBars, int radioTech, SignalStrength signalStrength) {
        int signalLevel = -1;
        SignalLevelInfo slInfo = null;
        int asu = signalStrength.getGsmSignalStrength();
        int umtsRscp = signalStrength.getUmtsRscp();
        int umtsEcno = signalStrength.getUmtsEcno();

        if (mSLFile.mSignalInfoTable[radioTech].containsKey(numBars)) {
            slInfo = mSLFile.mSignalInfoTable[radioTech].get(numBars);
        } else {
            if (radioTech == RADIOTECH_UMTS) {
                if (DBG) Log.d(TAG, "UMTS signal mapping num=" + numBars +
                    " rule not avaliable. try GSM.");
                return get3GPPSignalLevel(numBars, RADIOTECH_GSM, signalStrength);
            } else {
                Log.e(TAG, "GSM signal mapping num=" + numBars + " rule not avaliable.");
                return SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
            }
        }

        if (slInfo != null) {
            if (radioTech == RADIOTECH_UMTS) {
                int rscp_signalLevel = -1;
                int ecno_signalLevel = -1;
                if (slInfo.mSigStrLevel != null && umtsRscp <= -25) {
                    rscp_signalLevel = getSignalLevelNum(numBars, umtsRscp,
                            slInfo.mSigStrLevel);
                    signalLevel = rscp_signalLevel;
                }
                if (slInfo.mEcSnrLevel != null && umtsEcno < 0) {
                    ecno_signalLevel = getSignalLevelNum(numBars, umtsEcno,
                            slInfo.mEcSnrLevel);
                    if (signalLevel == -1) {
                        signalLevel = ecno_signalLevel;
                    } else {
                        signalLevel = (rscp_signalLevel < ecno_signalLevel) ?
                                rscp_signalLevel : ecno_signalLevel;
                    }
                }
                if (DBG) Log.d(TAG, "rscp_signalLevel=" + rscp_signalLevel +
                        ", ecno_signalLevel=" + ecno_signalLevel);
            }

            // GSM or no rscp and ecno is defined for UMTS
            if (signalLevel == -1) {
                // ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
                // asu = 99 is a special case, where the signal strength is unknown.
                if (asu == 99) {
                    signalLevel = 0;
                } else if (asu < 64) {
                    signalLevel = getSignalLevelNum(numBars, asu, slInfo.mAsuLevel);
                }
            }
        }
        if (DBG) Log.d(TAG, "GSM/UMTS Signal level: " + signalLevel);
        return (signalLevel == -1) ? SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN : signalLevel;
    }

    /**
     * Map CDMA/EVDO parameters to signal level
     */
    private int get3GPP2SignalLevel(int numBars, int radioTech, SignalStrength signalStrength) {
        int signalLevel = -1;
        SignalLevelInfo slInfo = null;
        int dbm;
        int ecSnr;
        boolean useEcio = false;
        if (radioTech == RADIOTECH_EVDO) {
            if (DBG) Log.d(TAG, "RADIO TECH EVDO, use ecio =" + mUseEVDOecio);
            dbm = signalStrength.getEvdoDbm();
            if (mUseEVDOecio == true) {
                ecSnr = signalStrength.getEvdoEcio();
                useEcio = true;
            } else {
                ecSnr = signalStrength.getEvdoSnr();
            }
        } else {
            dbm = signalStrength.getCdmaDbm();
            ecSnr = signalStrength.getCdmaEcio();
            useEcio = true;
        }

        if (mSLFile.mSignalInfoTable[radioTech].containsKey(numBars)) {
            slInfo = mSLFile.mSignalInfoTable[radioTech].get(numBars);
        } else {
            if (radioTech == RADIOTECH_EVDO) {
                if (DBG) Log.d(TAG, "EVDO signal mapping num=" + numBars +
                    " rule not avaliable. try CDMA.");
                //Sprint use CDMA signal also for EVDO network
                return get3GPP2SignalLevel(numBars, RADIOTECH_CDMA, signalStrength);
            } else {
                Log.e(TAG, "CDMA signal mapping num=" + numBars + " rule not avaliable.");
                return SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
            }
        }

        if (slInfo != null) {
            int dbm_signalLevel = -1;
            if (dbm < 0) {
                dbm_signalLevel = getSignalLevelNum(numBars, dbm, slInfo.mSigStrLevel);;
                signalLevel = dbm_signalLevel;
            }
            int ecSnr_signalLevel = -1;
            if ((useEcio && ecSnr < 0) || (!useEcio && ecSnr > 0)) {
                ecSnr_signalLevel = getSignalLevelNum(numBars, ecSnr, slInfo.mEcSnrLevel);
                if (signalLevel == -1) {
                    signalLevel = ecSnr_signalLevel;
                } else {
                    signalLevel = (dbm_signalLevel < ecSnr_signalLevel) ?
                            dbm_signalLevel : ecSnr_signalLevel;
                }
            }
            if (DBG) Log.d(TAG, "dbm_signalLevel=" + dbm_signalLevel +
                    ", ecSnr_signalLevel=" + ecSnr_signalLevel);
        }
        if (DBG) Log.d(TAG, "CDMA/EVDO Signal level: " + signalLevel);
        return signalLevel == -1 ? SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN : signalLevel;
    }

    /**
     * Map LTE parameters to signal level
     */
    private int getLTESignalLevel(int numBars, SignalStrength signalStrength) {
        int signalLevel = -1;
        SignalLevelInfo slInfo = null;
        int rsrp = signalStrength.getLteRsrp();
        int rssi = signalStrength.getLteSignalStrenght();
        int rssnr = signalStrength.getLteRssnr();

        if (mSLFile.mSignalInfoTable[RADIOTECH_LTE].containsKey(numBars)) {
            slInfo = mSLFile.mSignalInfoTable[RADIOTECH_LTE].get(numBars);
        } else {
            Log.e(TAG, "setLevels:get3GPP2SignalLevel: Bars=" + numBars + "rule not avaliable.");
            return 0;
        }

        if (slInfo != null) {
            int rsrp_signalLevel = -1;
            int rssi_signalLevel = -1;
            int rssnr_signalLevel = -1;
            if (slInfo.mSigStrLevel != null) {
                if (rsrp <= -44) { //RSRP(dbm) = -1/0x7FFFFFFF and RSRP > -44 invalid
                    rsrp_signalLevel = getSignalLevelNum(numBars, rsrp, slInfo.mSigStrLevel);
                }
                signalLevel = rsrp_signalLevel;
            }

            if (slInfo.mEcSnrLevel != null) {
                if (rssnr == SignalStrength.INVALID || rssnr > 300) {
                    // If SNR is invalid, then let RSRP drive the UI.
                    rssnr_signalLevel = numBars;
                } else {
                    rssnr_signalLevel = getSignalLevelNum(numBars, rssnr, slInfo.mEcSnrLevel);
                }
                if (DBG) Log.d(TAG, "rsrp_signalLevel=" + rsrp_signalLevel +
                        ", rssnr_signalLevel=" + rssnr_signalLevel);
                signalLevel = (rsrp_signalLevel < rssnr_signalLevel) ?
                        rsrp_signalLevel : rssnr_signalLevel;
            }

            // rsrp/rssnr are not defined, check rssi;
            if (signalLevel == -1 && slInfo.mAsuLevel != null
                    && rsrp != SignalStrength.INVALID) { // some ril report fake rssi for lte
                if (rssi == 99) {
                    rssi_signalLevel = 0;
                } else if (rssi < 64){
                    rssi_signalLevel = getSignalLevelNum(numBars, rssi, slInfo.mAsuLevel);
                }
                if (DBG) Log.d(TAG, "rssiSignalLevel=" + rssi_signalLevel);
                signalLevel = rssi_signalLevel;
            }
        }
        if (DBG) Log.d(TAG, "LTE Signal level: " + signalLevel);
        return (signalLevel == -1) ? SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN : signalLevel;
    }

    /**
     * Returns the GSM signal level parsed
     */
    private int getGsmSignalLevel(int numBars, SignalStrength signalStrength) {
        return get3GPPSignalLevel(numBars, RADIOTECH_GSM, signalStrength);
    }

    /**
     * Returns the UMTS signal level parsed
     */
    private int getUmtsSignalLevel(int numBars, SignalStrength signalStrength) {
        return get3GPPSignalLevel(numBars, RADIOTECH_UMTS, signalStrength);
    }

    /**
     * Returns the CDMA signal level parsed
     */
    private int getCdmaSignalLevel(int numBars, SignalStrength signalStrength) {
        return get3GPP2SignalLevel(numBars, RADIOTECH_CDMA, signalStrength);
    }

    /**
     * Returns the EVDO signal level parsed
     */
    private int getEvdoSignalLevel(int numBars, SignalStrength signalStrength) {
        return get3GPP2SignalLevel(numBars, RADIOTECH_EVDO, signalStrength);
    }

    /**
     * Returns the LTE signal level parsed
     */
    private int getLteSignalLevel(int numBars, SignalStrength signalStrength) {
        // calculate LTE signal bar with LTE SNR
        return getLTESignalLevel(numBars, signalStrength);
    }

    /**
     * Set signal level and asu signal level
     *
     * @hide
     */
    // Added set___MaxLevel(...) callbacks
    public void setLevels(SignalStrength signalStrength) {
        int numberOfBars = mNumberOfBars;
        if(numberOfBars == 0) {
            numberOfBars = 4;
            Log.w(TAG, "No NumberOfBars settings, set it as default 4.");
        }

        signalStrength.setMaxLevel(numberOfBars);

        // calculate all levels, SignalStrength will give ritht level to show
        signalStrength.setLteLevel(getLteSignalLevel(numberOfBars, signalStrength));
        signalStrength.setUmtsLevel(getUmtsSignalLevel(numberOfBars, signalStrength));
        signalStrength.setGsmLevel(getGsmSignalLevel(numberOfBars, signalStrength));
        signalStrength.setCdmaLevel(getCdmaSignalLevel(numberOfBars, signalStrength));
        signalStrength.setEvdoLevel(getEvdoSignalLevel(numberOfBars, signalStrength));

        if (DBG) Log.d(TAG, "setLevels: returned SignalStrength=" + signalStrength);
    }

    /**
     * Returns the number of signal bars set in config file
     */
    public int getNumberOfBars() {
        return mNumberOfBars;
    }
}
