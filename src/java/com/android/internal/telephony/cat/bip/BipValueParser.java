/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2016. All rights reserved.
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

package com.android.internal.telephony.cat;

import java.net.UnknownHostException;

/**
 * Class that implements BIP value parser.
 *
 * {@hide}
 */
abstract class BipValueParser {

    static BearerDesc retrieveBearerDesc(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        BearerDesc bearerDesc = null;
        GPRSBearerDesc gprsbearerDesc = null;
        EUTranBearerDesc euTranbearerDesc = null;
        DefaultBearerDesc defaultbearerDesc = null;
        try {
            int bearerType = rawValue[valueIndex++] & 0xff;
            CatLog.d("CAT", "retrieveBearerDesc: bearerType:" + bearerType + ", length: " + length);
            if (BipUtils.BEARER_TYPE_GPRS == bearerType) {
                gprsbearerDesc = new GPRSBearerDesc();
                gprsbearerDesc.precedence = rawValue[valueIndex++] & 0xff;
                gprsbearerDesc.delay = rawValue[valueIndex++] & 0xff;
                gprsbearerDesc.reliability = rawValue[valueIndex++] & 0xff;
                gprsbearerDesc.peak = rawValue[valueIndex++] & 0xff;
                gprsbearerDesc.mean = rawValue[valueIndex++] & 0xff;
                gprsbearerDesc.pdpType = rawValue[valueIndex++] & 0xff;
                return gprsbearerDesc;
            } else if (BipUtils.BEARER_TYPE_EUTRAN == bearerType) {
                euTranbearerDesc = new EUTranBearerDesc();
                euTranbearerDesc.qci = rawValue[valueIndex++] & 0xff;
                euTranbearerDesc.maxBitRateU = rawValue[valueIndex++] & 0xff;
                euTranbearerDesc.maxBitRateD = rawValue[valueIndex++] & 0xff;
                euTranbearerDesc.guarBitRateU = rawValue[valueIndex++] & 0xff;
                euTranbearerDesc.guarBitRateD = rawValue[valueIndex++] & 0xff;
                euTranbearerDesc.maxBitRateUEx = rawValue[valueIndex++] & 0xff;
                euTranbearerDesc.maxBitRateDEx = rawValue[valueIndex++] & 0xff;
                euTranbearerDesc.guarBitRateUEx = rawValue[valueIndex++] & 0xff;
                euTranbearerDesc.guarBitRateDEx = rawValue[valueIndex++] & 0xff;
                euTranbearerDesc.pdnType = rawValue[valueIndex++] & 0xff;
                return euTranbearerDesc;
            } else if (BipUtils.BEARER_TYPE_DEFAULT == bearerType) {
                defaultbearerDesc = new DefaultBearerDesc();
                return defaultbearerDesc;
            } else if (BipUtils.BEARER_TYPE_CSD == bearerType) {
                CatLog.d("CAT", "retrieveBearerDesc: unsupport CSD");
                throw new ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
            } else {
                CatLog.d("CAT", "retrieveBearerDesc: un-understood bearer type");
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveBearerDesc: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static int retrieveBufferSize(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int size = 0;

        try {
            size = ((rawValue[valueIndex] & 0xff) << 8) + (rawValue[valueIndex + 1] & 0xff);
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveBufferSize: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return size;
    }

        static String retrieveNetworkAccessName(ComprehensionTlv ctlv) throws ResultException {
            byte[] rawValue = ctlv.getRawValue();
            int valueIndex = ctlv.getValueIndex();
            String networkAccessName = null;

            try {
            // int len = ctlv.getLength() - ctlv.getValueIndex() + 1;
                int totalLen = ctlv.getLength();
                String stkNetworkAccessName = new String(rawValue, valueIndex, totalLen);
                String stkNetworkIdentifier = null;
                String stkOperatorIdentifier = null;

                if (stkNetworkAccessName != null && totalLen > 0) {
                //Get network identifier
                    int len = rawValue[valueIndex++];
                    if (totalLen > len) {
                          stkNetworkIdentifier = new String(rawValue, valueIndex, len);
                          valueIndex += len;
                    }
                    CatLog.d("CAT", "totalLen:" + totalLen + ";" + valueIndex + ";" + len);
                    //Get operator identififer
                    String tmpString = null;
                    while (totalLen > (len + 1)) {
                        totalLen -= (len + 1);
                        len = rawValue[valueIndex++];
                        CatLog.d("CAT", "next len: " + len);
                        if (totalLen > len) {
                            tmpString = new String(rawValue, valueIndex, len);
                            if (stkOperatorIdentifier == null) {
                                stkOperatorIdentifier = tmpString;
                            } else {
                                stkOperatorIdentifier = stkOperatorIdentifier + "." + tmpString;
                            }
                            tmpString = null;
                        }
                        valueIndex += len;
                        CatLog.d("CAT", "totalLen:" + totalLen + ";" + valueIndex + ";" + len);
                    }

                    if (stkNetworkIdentifier != null && stkOperatorIdentifier != null) {
                        networkAccessName = stkNetworkIdentifier + "." + stkOperatorIdentifier;
                    } else if (stkNetworkIdentifier != null) {
                        networkAccessName = stkNetworkIdentifier;
                    }
                    CatLog.d("CAT", "nw:" + stkNetworkIdentifier + ";" + stkOperatorIdentifier);
                }
            } catch (IndexOutOfBoundsException e) {
                CatLog.d("CAT", "retrieveNetworkAccessName: out of bounds");
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }

            return networkAccessName;
        }

    static TransportProtocol retrieveTransportProtocol(ComprehensionTlv ctlv)
            throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int protocolType = 0;
        int portNumber = 0;

        try {
            protocolType = rawValue[valueIndex++];
            portNumber = ((rawValue[valueIndex] & 0xff) << 8) + (rawValue[valueIndex + 1] & 0xff);
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveTransportProtocol: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return new TransportProtocol(protocolType, portNumber);
    }

    static OtherAddress retrieveOtherAddress(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int addressType = 0;
        OtherAddress otherAddress = null;

        try {
            addressType = rawValue[valueIndex++];
            if (BipUtils.ADDRESS_TYPE_IPV4 == addressType) {
                otherAddress = new OtherAddress(addressType, rawValue, valueIndex);
            } else if (BipUtils.ADDRESS_TYPE_IPV6 == addressType) {
                return null;
                // throw new
                // ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
            } else {
                return null;
                // throw new
                // ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveOtherAddress: out of bounds");
            return null;
            // throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        } catch (UnknownHostException e2) {
            CatLog.d("CAT", "retrieveOtherAddress: unknown host");
            return null;
            // throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return otherAddress;
    }

    static int retrieveChannelDataLength(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = 0;

        CatLog.d("CAT", "valueIndex:" + valueIndex);

        try {
            length = rawValue[valueIndex] & 0xFF;
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveTransportProtocol: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return length;
    }

    static byte[] retrieveChannelData(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        byte[] channelData = null;

        try {
            channelData = new byte[ctlv.getLength()];
            System.arraycopy(rawValue, valueIndex, channelData, 0, channelData.length);
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveChannelData: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return channelData;
    }

    static byte[] retrieveNextActionIndicator(ComprehensionTlv ctlv) throws ResultException {
        byte[] nai;

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();

        nai = new byte[length];
        try {
            for (int index = 0; index < length; ) {
                nai[index++] = rawValue[valueIndex++];
            }
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return nai;
    }
}
