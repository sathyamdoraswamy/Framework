package com.frameworkserver;

/**
 *
 * @author sathyam
 */
import java.io.*;

class Bundle implements java.io.Serializable {

    int userId;
    int transactionId;
    int bundleType;
    int noOfBundles;
    int bundleNumber;
    int bundleSize;
    byte[] data;
    int DATA = 1, ACK = 2, RETRANS = 3, START = 4, STOP = 5;

    // This function is used to parse a bundle from a set of bytes
    public void parse(byte[] buffer) {
        byte userIdarr[] = new byte[4];
        byte transactionIdarr[] = new byte[4];
        byte bundleTypearr[] = new byte[4];
        byte noOfBundlesarr[] = new byte[4];
        byte bundleNumberarr[] = new byte[4];
        byte bundleSizearr[] = new byte[4];
        for (int lk = 0; lk < 4; lk++) {
            userIdarr[lk] = buffer[lk];
            transactionIdarr[lk] = buffer[lk + 4];
            bundleTypearr[lk] = buffer[lk + 8];
            noOfBundlesarr[lk] = buffer[lk + 12];
            bundleNumberarr[lk] = buffer[lk + 16];
            bundleSizearr[lk] = buffer[lk + 20];
        }
        userId = byteArrayToInt(userIdarr);
        transactionId = byteArrayToInt(transactionIdarr);
        bundleType = byteArrayToInt(bundleTypearr);
        noOfBundles = byteArrayToInt(noOfBundlesarr);
        bundleNumber = byteArrayToInt(bundleNumberarr);
        bundleSize = byteArrayToInt(bundleSizearr);
        data = new byte[bundleSize];
        for (int j = 0; j < bundleSize; j++) {
            data[j] = buffer[j + 24];
        }
    }

    // This function is used to check whether the current bundle is an
    // acknowledgement to a bundle whose parameters are specified by the arguments
    public boolean isAcknowledgement(int transactionid, int noofbundles,
            int bundleno) {
        if (transactionid == transactionId && noofbundles == noOfBundles
                && bundleno == bundleNumber && bundleType == ACK) {
            return true;
        } else {
            return false;
        }
    }

    // This function is used to convert an integer to an array of bytes
    public static final byte[] intToByteArray(int value) {
        return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16),
                    (byte) (value >>> 8), (byte) value};
    }

    // This function is used to get an integer stored in an array of bytes
    public static final int byteArrayToInt(byte[] b) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }

    // This function is used to get the current bundle in bytes
    public byte[] getBytes() {
        byte userIdarr[] = new byte[4];
        byte transactionIdarr[] = new byte[4];
        byte bundleTypearr[] = new byte[4];
        byte noOfBundlesarr[] = new byte[4];
        byte bundleNumberarr[] = new byte[4];
        byte bundleSizearr[] = new byte[4];
        byte[] buffer = new byte[bundleSize + 24];
        userIdarr = intToByteArray(userId);
        transactionIdarr = intToByteArray(transactionId);
        bundleTypearr = intToByteArray(bundleType);
        noOfBundlesarr = intToByteArray(noOfBundles);
        bundleNumberarr = intToByteArray(bundleNumber);
        bundleSizearr = intToByteArray(bundleSize);
        for (int lk = 0; lk < 4; lk++) {
            buffer[lk] = userIdarr[lk];
            buffer[lk + 4] = transactionIdarr[lk];
            buffer[lk + 8] = bundleTypearr[lk];
            buffer[lk + 12] = noOfBundlesarr[lk];
            buffer[lk + 16] = bundleNumberarr[lk];
            buffer[lk + 20] = bundleSizearr[lk];
        }
        for (int j = 0; j < bundleSize; j++) {
            buffer[j + 24] = data[j];
        }
        return buffer;
    }

    // This function is used to make this bundle an acknowledgement of the given
    // bundle
    public void createACK(Bundle b) {
        userId = b.userId;
        transactionId = b.transactionId;
        bundleType = ACK;
        noOfBundles = b.noOfBundles;
        bundleNumber = b.bundleNumber;
        bundleSize = 0;
        data = null;
    }
}