/*
 * Copyright 2019 Konrad Gerlach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.konrad_gerlach.vertretungsplanapp.security;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import androidx.annotation.NonNull;
import android.util.Base64;

import org.konrad_gerlach.vertretungsplanapp.Main;
import org.konrad_gerlach.vertretungsplanapp.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;


public class Cryptography {
    private static final String AESMode="AES/GCM/NoPadding";
    private static final String oldAESMode="AES/ECB/PKCS7Padding";
    private static final String RSAMode="RSA/ECB/PKCS1Padding";
    private static final String AndroidKeyStore = "AndroidKeyStore";
    /**
     * decrypts the message previously stored in shared preferences
     * @param context
     * @return returns the decrypted message or empty string ("") if any error occured or no message was found
     */
    public static String decryptCredentials(Context context)
    {

            //loads the encrypted credentials and the init vector
            SharedPreferences sharedPref =context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            String encryptedCredStr =sharedPref.getString(Main.CREDENTIALS_LOCATION,"");
            //faulty values or nonexistant saves lead to returning "" --> error value
            if(encryptedCredStr.equals(""))
            {
                return "";
            }
            return decrypt(Main.CREDENTIALS_INIT_VECTOR,encryptedCredStr,context,Main.CREDENTIALS_KEY,Main.CREDENTIALS_ENCRYPT_SUCCESS_KEY,Main.CREDENTIALS_KEY_23_AES,Main.CREDENTIALS_KEY_23_RSA);
    }
    @TargetApi(23)
    public static String newDecrypt(String IVKey,String encryptedMessage,Context context,String keyAlias,String encryptSuccessKey)
    {   try {

            SharedPreferences sharedPref =context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            String initVStr = sharedPref.getString(IVKey, "");
            Boolean encryptSuccess = sharedPref.getBoolean(encryptSuccessKey,false);
            if (initVStr.equals("")||!encryptSuccess||encryptedMessage.equals("")) {
                return "";
            }
            //decodes the string representations of the byte arrays containing the encrypted credentials and the init vector
            byte[] byteEncryptedMessage = Base64.decode(encryptedMessage, Base64.NO_WRAP);
            byte[] initVector = Base64.decode(initVStr, Base64.NO_WRAP);
            //loads the key
            KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);
            if(!keyStore.containsAlias(keyAlias)) {
                Main.log("Warning","Did not find key in KeyStore",Cryptography.class);
                return "";
            }
            final KeyStore.SecretKeyEntry secretKEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(keyAlias, null);
            final SecretKey secretK = secretKEntry.getSecretKey();
            //decrypts the credentials
            final Cipher chiffre = Cipher.getInstance(AESMode);
            final GCMParameterSpec spec = new GCMParameterSpec(128, initVector);
            chiffre.init(Cipher.DECRYPT_MODE, secretK, spec);
            final byte[] decodedCredentials = chiffre.doFinal(byteEncryptedMessage);
            return new String(decodedCredentials, "UTF-8");
        }
        catch(java.io.IOException | java.security.NoSuchAlgorithmException|java.security.cert.CertificateException|java.security.KeyStoreException|java.security.UnrecoverableEntryException|javax.crypto.NoSuchPaddingException|java.security.InvalidKeyException| java.security.InvalidAlgorithmParameterException|javax.crypto.IllegalBlockSizeException| javax.crypto.BadPaddingException e) {
            Main.log("Error", e.toString(), Cryptography.class);
            return "";
        }
    }
    public static String encrypt(String IVKey, @NonNull String message, @NonNull Context context,String keyAlias,String encryptSuccessKey,String oldAESKeyAlias,String RSAKeyAlias)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            return newEncrypt(IVKey,message,context,keyAlias,encryptSuccessKey);
        }
        else{
            return oldEncrypt(context,message,oldAESKeyAlias,RSAKeyAlias,encryptSuccessKey,IVKey);
        }
    }
    public static String decrypt(String IVKey, @NonNull String encryptedMessage, @NonNull Context context,String keyAlias,String encryptSuccessKey,String oldAESKeyAlias,String RSAKeyAlias)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            return newDecrypt(IVKey,encryptedMessage,context,keyAlias,encryptSuccessKey);
        }
        else{
            return oldDecrypt(context,encryptedMessage,oldAESKeyAlias,RSAKeyAlias,encryptSuccessKey,IVKey);
        }
    }
    /**
     * encrypts credentials and stores them in shared preferences
     * @param message the credentials to be stored in shared preferences, also theoretically works with any String
     * @param context used to access shared preferences
     * @throws javax.crypto.NoSuchPaddingException
     * @throws java.security.InvalidKeyException
     * @throws java.io.UnsupportedEncodingException
     * @throws javax.crypto.IllegalBlockSizeException
     * @throws javax.crypto.BadPaddingException
     * @throws java.security.NoSuchAlgorithmException
     */
    public static void encryptCredentials( String message, Context context)
    {
        String encrypted = encrypt(Main.CREDENTIALS_INIT_VECTOR,message,context,Main.CREDENTIALS_KEY,Main.CREDENTIALS_ENCRYPT_SUCCESS_KEY,Main.CREDENTIALS_KEY_23_AES,Main.CREDENTIALS_KEY_23_RSA);
        SharedPreferences sharedPref =context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Main.CREDENTIALS_LOCATION,encrypted);
        editor.commit();
    }
    @TargetApi(23)
    public static String newEncrypt(String IVKey, @NonNull String message, @NonNull Context context,String keyAlias,String encryptSuccessKey)
    {
        try {
            final Cipher chiffre = Cipher.getInstance(AESMode);
            final SecretKey secretK = createKey(keyAlias);
            chiffre.init(Cipher.ENCRYPT_MODE, secretK);
            byte[] initVector = chiffre.getIV();
            //TODO adjust charset if neccessary
            byte[] encrypted = chiffre.doFinal(message.getBytes("UTF-8"));
            SharedPreferences sharedPref =context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            //saves the init vector to shared Preferences
            //converts the byte array to a string
            String IV = Base64.encodeToString(initVector,Base64.NO_WRAP);
            editor.putString(IVKey,IV);
            editor.putBoolean(encryptSuccessKey,true);
            //saves the encrypted credentials to shared Preferences
            editor.commit();
            return Base64.encodeToString(encrypted,Base64.NO_WRAP);
        }
        //in case of an error it flags encryption as unsuccessful and returns the message to be saved in plain text
        catch(java.security.NoSuchProviderException|java.security.NoSuchAlgorithmException|java.security.InvalidAlgorithmParameterException| javax.crypto.NoSuchPaddingException| java.security.InvalidKeyException|java.io.UnsupportedEncodingException|javax.crypto.IllegalBlockSizeException| javax.crypto.BadPaddingException e)
        {
            Main.log("Error",e.toString(),Cryptography.class);
            SharedPreferences sharedPref =context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(encryptSuccessKey,false);
            editor.commit();
            return "";
        }
    }

    /**
     * creates a SecretKey, which is stored in AndroidKeyStore
     * @return the created SecretKey
     * @throws java.security.NoSuchProviderException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.InvalidAlgorithmParameterException
     */
    @TargetApi(23)
    public static SecretKey createKey(String keyAlias)
            throws java.security.NoSuchProviderException, java.security.NoSuchAlgorithmException,java.security.InvalidAlgorithmParameterException

    {
        KeyGenerator keyGenerator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore);
            final KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build();
            keyGenerator.init(keyGenParameterSpec);

        }
        //TODO untested
        else{
            keyGenerator= KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore);
            keyGenerator.init(128);
        }
        return keyGenerator.generateKey();
    }
    //generates RSA key pairs, which are stored in the android key store
    public static void createRSAKey(Context context, String keyAlias, BigInteger serialNumber)
    {try {
        KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);
        if (!keyStore.containsAlias(keyAlias)) {
            // Generate a key pair for encryption
            Calendar startDate = Calendar.getInstance();
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.YEAR, 30);
            KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                    .setSubject(new X500Principal("CN=" + keyAlias))
                    .setSerialNumber(serialNumber)
                    .setStartDate(startDate.getTime())
                    .setEndDate(expirationDate.getTime())
                    .setAlias(keyAlias)
                    .build();
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, AndroidKeyStore);

            keyPairGenerator.initialize(spec);
            keyPairGenerator.generateKeyPair();
        }
    }catch( java.io.IOException|java.security.KeyStoreException|java.security.cert.CertificateException|java.security.NoSuchAlgorithmException|java.security.NoSuchProviderException|java.security.InvalidAlgorithmParameterException e)
        {
            Main.log("Error",e.toString(),Cryptography.class);
        }
    }
    //encrypts a message using a public key located in the androidKeyStore
    private static byte[] RSAEncrypt(byte[] message, String keyAlias) throws Exception{
        KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);
        if(keyStore.containsAlias(keyAlias)) {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(keyAlias, null);
            Cipher chiffre = Cipher.getInstance(RSAMode, "AndroidOpenSSL");
            chiffre.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, chiffre);
            cipherOutputStream.write(message);
            cipherOutputStream.close();

            byte[] vals = outputStream.toByteArray();
            return vals;
        }
        else{
            Main.log("Error","failed to find key in keyStore",Cryptography.class);
            return new byte[0];
        }
    }
    private static  byte[] RSADecrypt(byte[] encryptedMessage, String keyAlias) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(keyAlias, null);
        Cipher chiffre = Cipher.getInstance(RSAMode, "AndroidOpenSSL");
        chiffre.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
        CipherInputStream cipherInputStream = new CipherInputStream(new ByteArrayInputStream(encryptedMessage), chiffre);

        //TODO untested
        byte[] buffer = new byte[20];

        ByteArrayOutputStream byteArrayGen = new ByteArrayOutputStream();
        int bytesRead;
        while((bytesRead=cipherInputStream.read(buffer))>=0)
        {
            byteArrayGen.write(buffer,0,bytesRead);
        }
        return byteArrayGen.toByteArray();
    }
    private static void generateAESKey(Context context,String AESkeyAlias, String RSAkeyAlias)
    throws java.lang.Exception
    {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String enryptedKey = sharedPref.getString(AESkeyAlias, null);
        //generates a new key, if no key has previously been generated
        if (enryptedKey == null) {
            byte[] key = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(key);
            byte[] encryptedAESKey = RSAEncrypt(key,RSAkeyAlias);
            enryptedKey = Base64.encodeToString(encryptedAESKey, Base64.NO_WRAP);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(AESkeyAlias, enryptedKey);
            editor.commit();
        }
    }
    private static Key getAESKey(Context context, String AESkeyALias, String RSAkeyAlias) throws Exception{
        generateAESKey(context,AESkeyALias,RSAkeyAlias);
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String encryptedKey = sharedPref.getString(AESkeyALias, null);
        byte[] byteEncryptedKey = Base64.decode(encryptedKey, Base64.NO_WRAP);
        byte[] secretK = RSADecrypt(byteEncryptedKey,RSAkeyAlias);
        return new SecretKeySpec(secretK, "AES");
    }
    private static String oldEncrypt(Context context, String message, String AESkeyALias, String RSAkeyAlias,String encryptSuccessKey,String IVKey) {
        try {
            byte[] byteMessage =message.getBytes("UTF-8");
            Cipher chiffre = Cipher.getInstance(oldAESMode, "BC");
            Key secretK= getAESKey(context, AESkeyALias, RSAkeyAlias);
            chiffre.init(Cipher.ENCRYPT_MODE, secretK);
            //byte[] initVector = chiffre.getIV();
            byte[] encodedBytes = chiffre.doFinal(byteMessage);
            SharedPreferences sharedPref =context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            //saves the init vector to shared Preferences
            //converts the byte array to a string
            //String IV = Base64.encodeToString(initVector,Base64.NO_WRAP);
            //editor.putString(IVKey,IV);
            editor.putBoolean(encryptSuccessKey,true);
            //saves the encrypted credentials to shared Preferences
            editor.commit();
            String encryptedMessage = Base64.encodeToString(encodedBytes, Base64.NO_WRAP);
            return encryptedMessage;
        }
        catch(javax.crypto.BadPaddingException|javax.crypto.IllegalBlockSizeException e)
        {
            Main.log("Error",e.toString(),Cryptography.class);
        }
        catch(java.lang.Exception e)
        {
            Main.log("Error",e.toString(),Cryptography.class);
        }
        return "";
    }
    private static String oldDecrypt(Context context, String encryptedMessage, String AESkeyALias, String RSAkeyAlias,String encryptSuccessKey,String IVKey) {
        try{
            SharedPreferences sharedPref =context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            //String initVStr = sharedPref.getString(IVKey, "");
            Boolean encryptSuccess = sharedPref.getBoolean(encryptSuccessKey,false);
            //initVStr.equals("")||
            if (!encryptSuccess||encryptedMessage.equals("")) {
                return "";
            }
            //decodes the string representations of the byte arrays containing the encrypted credentials and the init vector
            byte[] byteEncryptedMessage =Base64.decode(encryptedMessage, Base64.NO_WRAP);
            //byte[] initVector = Base64.decode(initVStr, Base64.NO_WRAP);

            Cipher chiffre = Cipher.getInstance(oldAESMode);
            Key secretK= getAESKey(context, AESkeyALias, RSAkeyAlias);

            //final GCMParameterSpec spec = new GCMParameterSpec(128, initVector);
            //chiffre.init(Cipher.DECRYPT_MODE, secretK,spec);
            chiffre.init(Cipher.DECRYPT_MODE, secretK);
            final byte[] decodedBytes = chiffre.doFinal(byteEncryptedMessage);
            String decodedMessage = new String(decodedBytes,"UTF-8");
            return decodedMessage;
        }
        catch(javax.crypto.BadPaddingException|javax.crypto.IllegalBlockSizeException e)
        {
            Main.log("Error",e.toString(),Cryptography.class);
        }
        catch(java.lang.Exception e)
        {
            Main.log("Error",e.toString(),Cryptography.class);
        }
        return "";
    }
}
