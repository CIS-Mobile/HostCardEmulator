package com.cismobile.hostcardemulator;

import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;

public final class HostCardEmulatorService extends HostApduService {
   private static final String TAG = "Host Card Emulator";

   private static final String STATUS_SUCCESS = "9000";
   private static final String STATUS_FAILED = "6F00";
   private static final String STATUS_NO_APP = "6A82";
   private static final String CLA_NOT_SUPPORTED = "6E00";
   private static final String INS_NOT_SUPPORTED = "6D00";
   private static final String AID = "A0000002471001";
   private static final String SELECT_INS = "A4";
   private static final String GET_DATA_INS = "CB";
   private static final String DEFAULT_CLA = "00";
   private static final int MIN_APDU_LENGTH = 8;

   private static final byte[] SELECT_APDU = Utils.BuildSelectApdu(AID);

   public void launchScreen() {
      Intent intent = new Intent(this, MainActivity.class);
      startActivity(intent);
   }

   /**
    * Called if the connection to the NFC card is lost, in order to let the application know the
    * cause for the disconnection (either a lost link, or another AID being selected by the
    * reader).
    *
    * @param reason Either DEACTIVATION_LINK_LOSS or DEACTIVATION_DESELECTED
    */
   @Override
   public void onDeactivated(int reason) {
      Log.d(TAG, "Deactivated: " + reason);
   }

   /**
    * This method will be called when a command APDU has been received from a remote device. A
    * response APDU can be provided directly by returning a byte-array in this method. In general
    * response APDUs must be sent as quickly as possible, given the fact that the user is likely
    * holding his device over an NFC reader when this method is called.
    *
    * <p class="note">If there are multiple services that have registered for the same AIDs in
    * their meta-data entry, you will only get called if the user has explicitly selected your
    * service, either as a default or just for the next tap.
    *
    * <p class="note">This method is running on the main thread of your application. If you
    * cannot return a response APDU immediately, return null and use the {@link
    * #sendResponseApdu(byte[])} method later.
    *
    * @param commandApdu The APDU that received from the remote device
    * @param extras A bundle containing extra data. May be null.
    * @return a byte-array containing the response APDU, or null if no response APDU can be sent
    * at this point.
    */
   @Override
   public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
      byte[] returnBytes = new byte[65538];

      launchScreen();
      if (commandApdu == null) {
         // The APDU can't be empty
         Log.d(TAG, "APDU is empty");
         return Utils.HexStringToByteArray(STATUS_FAILED);
      }

      String hexCommandApdu = Utils.ByteArrayToHexString(commandApdu);
      if (hexCommandApdu.length() < MIN_APDU_LENGTH) {
         Log.d(TAG, "APDU is malformed - too short");
         return Utils.HexStringToByteArray(STATUS_FAILED);
      }

      if (!(hexCommandApdu.substring(0, 2)).equals(DEFAULT_CLA)) {
         Log.d(TAG, "APDU is malformed - CLA is not supported: " + hexCommandApdu.substring(0, 2));
         return Utils.HexStringToByteArray(CLA_NOT_SUPPORTED);
      }

      if (!(hexCommandApdu.substring(2, 4)).equals(SELECT_INS) &&
            !(hexCommandApdu.substring(2, 4)).equals(GET_DATA_INS)) {
         Log.d(TAG, "APDU is malformed - INS is not supported: " + hexCommandApdu.substring(2, 4));
         return Utils.HexStringToByteArray(INS_NOT_SUPPORTED);
      }

      if ((hexCommandApdu.substring(2, 4)).equals(SELECT_INS)) {
         if (!Arrays.equals(SELECT_APDU, commandApdu)) {
            Log.d(TAG, "APDU does not match expected command: " + hexCommandApdu);
            Log.d(TAG, "No matching AID found");
            return Utils.HexStringToByteArray(STATUS_NO_APP);
         }
         String dataToSend = "test";
         byte[] dataBytes = dataToSend.getBytes();
         returnBytes =  Utils.ConcatArrays(dataBytes, Utils.HexStringToByteArray(STATUS_SUCCESS));
      }

      if ((hexCommandApdu.substring(2, 4)).equals(GET_DATA_INS)) {
         String dataToSend = "test";
         byte[] dataBytes = dataToSend.getBytes();
         returnBytes =  Utils.ConcatArrays(dataBytes, Utils.HexStringToByteArray(STATUS_SUCCESS));
      }
      return returnBytes;
   }
}

