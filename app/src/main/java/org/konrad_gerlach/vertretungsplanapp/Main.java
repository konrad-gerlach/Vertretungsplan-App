
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

package org.konrad_gerlach.vertretungsplanapp;


import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.net.*;
import java.io.*;
import java.math.BigInteger;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import android.os.*;
import java.util.*;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;

import org.konrad_gerlach.vertretungsplanapp.security.Cryptography;
//TODO show notification reception dialog after first logging in
public class Main extends AppCompatActivity {
    //constants
    final public static String VPL_SAVE_FILE_NAME = "vplSaveFile.txt";
    final public static String LOG_FILE_NAME = "VPLLog.txt";
    final public static String ABBREVIATIONS_FILE_NAME = "abbreviations.txt";
    final public static String SHOWN_NOTIFICATIONS_FILE_NAME="shownNotifications.txt";
    final public static String MAIN_CHANNEL_ID = "org.konrad_gerlach.vertretungsplanapp.main_channel";
    final public static String NOTIFICATION_GROUP="org.konrad_gerlach.vertretungsplanapp.vpl_notifications";
    //shared preferences keys
    final public static String HAS_SAVES_KEY ="hasSaves";
    final public static String EULA_ACCEPTED_KEY ="EULAaccepted";
    final public static String MAIN_APP_LAUNCHED_BEFORE_KEY ="launchedBefore";
    final public static String ACTIVITY_LAUNCHED_BEFORE_KEY ="launchedActivityBefore";
    final public static String RECEIVE_NOTIFICATIONS_KEY ="receiveNotifications";
    final public static String THEME_KEY="theme";
    final public static String CLASS_NAME_KEY="className";
    final public static String WRITE_LOG_KEY="writeLog";

    //credentials Encryption
    //the key alias used to find and store encrypted credentials within shared preferences
    final public static String CREDENTIALS_LOCATION = "credentials_Key";
    //the key aliases used to encrypt and decrypt credentials
    final public static String CREDENTIALS_KEY ="credentialsKey";
    final public static String CREDENTIALS_INIT_VECTOR ="Credentials_IV_AES";
    final public static String CREDENTIALS_ENCRYPT_SUCCESS_KEY="Credentials_Encrypt_Success_Key";
    //key aliases for encryption in api level 23 and less
    final public static String CREDENTIALS_KEY_23_RSA="credentialsKeyRSA";
    final public static String CREDENTIALS_KEY_23_AES="credentialsKeyAES";

    //teachers abbreviations Encryption
    final public static String TA_KEY = "TA_Key";
    final public static String TA_INIT_VECTOR ="TA_IV_AES";
    final public static String TA_ENCRYPT_SUCCESS_KEY="TA_Encrypt_Success_Key";
    final public static String TA_KEY_23_RSA="TAKeyRSA";
    final public static String TA_KEY_23_AES="TAKeyAES";

    //saveFile Encryption
    final public static String SF_KEY = "SF_Key";
    final public static String SF_INIT_VECTOR ="SF_IV_AES";
    final public static String SF_ENCRYPT_SUCCESS_KEY="SF_Encrypt_Success_Key";
    final public static String SF_KEY_23_RSA="SFKeyRSA";
    final public static String SF_KEY_23_AES="SFKeyAES";

    //shownNotifications encryption
    final public static String SN_KEY = "SN_Key";
    final public static String SN_INIT_VECTOR ="SN_IV_AES";
    final public static String SN_ENCRYPT_SUCCESS_KEY="SN_Encrypt_Success_Key";
    final public static String SN_KEY_23_RSA="SNKeyRSA";
    final public static String SN_KEY_23_AES="SNKeyAES";

    //place to store current notification id within shared prefs
    final public static String NOTIFICATION_ID="notification_id";
    final public static String JOB_ID ="job_id";

    public static Boolean saveCredentials = false;
    public static String username = "";
    public static String password = "";
    public static String websiteAddress="";
    public static File saveFile;
    public static File LogFile;
    public static File credentialsSaveFile;
    public static File abbreviationsFile;
    public static String theme = "Rot";
    //ensures compliance
    public static Boolean writeLog=false;
    public static Boolean launchedBefore = false;
    public static Boolean EULAAccepted = false;
    public static Boolean hasSaves = false;
    public static String className = "";
    public static EULAAcceptedInitiator eulaAcceptedInitiator = new EULAAcceptedInitiator();
    boolean credentialsReadSuccess = false;
    public static boolean receiveNotifications = false;
    //private boolean encryptionKeyExists=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        eulaAcceptedInitiator.addEventListener(new EULAAcceptedListener(this));
        //initiates all the saveFiles (including the logFile
        LogFile = new File(this.getExternalFilesDir(null), LOG_FILE_NAME);
        saveFile = new File(this.getFilesDir(), VPL_SAVE_FILE_NAME);
        abbreviationsFile = new File(this.getFilesDir(), ABBREVIATIONS_FILE_NAME);

        //saves that it is the first time the app has been launched or changes value of launchedBefore to true
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if(sharedPref.getBoolean(Main.MAIN_APP_LAUNCHED_BEFORE_KEY,launchedBefore)){
            // means activity was not launched for the first time
            launchedBefore=true;
        }else{
            launchedBefore=false;
            // means activity was launched for the first time
            //store value in SharedPreferences as true
            editor.putBoolean(Main.MAIN_APP_LAUNCHED_BEFORE_KEY, true);
            editor.apply();
        }
        EULAAccepted=sharedPref.getBoolean(Main.EULA_ACCEPTED_KEY,EULAAccepted);
        hasSaves=sharedPref.getBoolean(Main.HAS_SAVES_KEY,hasSaves);
        //loads Username and Password from File
        try {
            //creates new files if they don't already exist to prevent filenotfound exceptions
            LogFile.createNewFile();
            saveFile.createNewFile();
            abbreviationsFile.createNewFile();


            //gets the Information from the shared prefs file (loads settings)
            SharedPreferences settingsFile = this.getSharedPreferences(
                    getString(R.string.settings_file_key), MODE_PRIVATE);
            receiveNotifications=settingsFile.getBoolean(Main.RECEIVE_NOTIFICATIONS_KEY,receiveNotifications);
            theme=settingsFile.getString(Main.THEME_KEY,theme);
            className=settingsFile.getString(Main.CLASS_NAME_KEY,className);
            writeLog=settingsFile.getBoolean(Main.WRITE_LOG_KEY,writeLog);

            //loads the credentials
            String credentialsUnsplit = Cryptography.decryptCredentials(this);
            if(!credentialsUnsplit.equals(""))
            {
                String[] credentials = credentialsUnsplit.split("\\|\\|");
                websiteAddress= credentials[0];
                username=credentials[1];
                password=credentials[2];
                credentialsReadSuccess=true;
                saveCredentials=true;
            }
            loadTeachersAbbreviations(abbreviationsFile,this);
        } catch (IOException e) {
            log("Error", "Error while interacting with a save File", this.getClass());
        }

        setTheme(ThemeNametoThemeInt(theme, "ID"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startchoice);
        if(launchedBefore)
        log("Info", "Launched App", this.getClass());
        //decides what to do next
        carryOn();


    }
    //loads the decrypted Teachers Abbreviations
    public static void loadTeachersAbbreviations(File abbreviationsFile,Context context)
            throws IOException
    {
        log("Info","Loading teachers abbreviations",Main.class);
        //loads the teachers abbreviations from the save File
        java.io.FileReader R4 = new java.io.FileReader(abbreviationsFile);
        java.io.BufferedReader BR4 = new java.io.BufferedReader(R4);
        String encryptedContent1="";
        String readLine1 ="";
        while((readLine1=BR4.readLine())!=null)
        {
            encryptedContent1=encryptedContent1.concat(readLine1);
        }
        String decryptedContent =Cryptography.decrypt(TA_INIT_VECTOR,encryptedContent1,context,TA_KEY,TA_ENCRYPT_SUCCESS_KEY,Main.TA_KEY_23_AES,Main.TA_KEY_23_RSA);
        if(!decryptedContent.equals("")) {
            Scanner abbrScanner = new Scanner(decryptedContent);
            abbrScanner.useDelimiter("<delimiter>");
            VPLDisplayer.abbreviations.clear();
            while (abbrScanner.hasNext()) {
                Teacher_Abbreviations_Storage constructable = new Teacher_Abbreviations_Storage();
                constructable.setName(abbrScanner.next());
                if (abbrScanner.hasNext()) {
                    constructable.setShortHand(abbrScanner.next());
                    VPLDisplayer.abbreviations.add(constructable);
                } else {
                    log("Error", "Error while interacting with abbr save File", Main.class);
                }
            }
        }
        BR4.close();
        R4.close();

    }
    public void EULAaccepted()
    {
        EULAAccepted=true;
        writeLog=true;
        //ensures logging only starts with the
        SharedPreferences settingsFile = this.getSharedPreferences(
                getString(R.string.settings_file_key), MODE_PRIVATE);
        SharedPreferences.Editor settingEdit = settingsFile.edit();
        settingEdit.putBoolean(Main.WRITE_LOG_KEY, Main.writeLog);
        settingEdit.commit();

        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(Main.EULA_ACCEPTED_KEY, true);
        editor.commit();
        //logs the sdk version of the device for debug/development purposes lateron but only if the EULA was accepted
        log("Info", "User accepted EULA", this.getClass());
        log("Info","Device SDK version"+Build.VERSION.SDK_INT,this.getClass());
        //creates the RSA keys used for encryption
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
        {
            Cryptography.createRSAKey(this,CREDENTIALS_KEY_23_RSA, BigInteger.valueOf(10000));
            Cryptography.createRSAKey(this,SN_KEY_23_RSA, BigInteger.valueOf(10001));
            Cryptography.createRSAKey(this,SF_KEY_23_RSA, BigInteger.valueOf(10002));
            Cryptography.createRSAKey(this,TA_KEY_23_RSA, BigInteger.valueOf(10003));
        }
        //uses alarmmanager to schedule notification service on api less than 21
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            BootReceiver.scheduleAlarm(this);
        }
        //creates a notification job if api level is greater than or equal to 21
        else {
            BootReceiver.scheduleJob(this);
        }
    }
    public void createChannel(Context context) {
        //creates and registers the notfication channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O&&!launchedBefore) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            CharSequence name = context.getString(R.string.channel_Name);
            String description = context.getString(R.string.channel_Desc);
            NotificationChannel channel = new NotificationChannel(MAIN_CHANNEL_ID, name,NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(description);
            // Register the channel with the system
            try {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
            catch(NullPointerException e)
            {
                log("Error","NullPointerException occured while attempting to create notification channel"+e,this.getClass());
            }
        }
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) Main.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        try{NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();}
        catch(NullPointerException e)
        {
            log("Error","NullPointerException occured while accessing network state"+e,this.getClass());
        }
        return false;
    }

    private void launchLogin() {
        Intent intent = new Intent(this, VPLLogin.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("Action", "Login");
        startActivity(intent);
    }

    private void launchVPLDisplayer(String extra) {
        Intent intent = new Intent(this, VPLDisplayer.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("Action", extra);
        startActivity(intent);
    }

    private void launchEULA() {
        Intent intent = new Intent(this, EULA.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }






    public void carryOn() {
        //different ways to access VPL
        if(!EULAAccepted) {
            launchEULA();
        }
        else
        {
            if (isNetworkAvailable() && password != "" && username != "" && credentialsReadSuccess && launchedBefore) {
                Authenticator.setDefault(new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password.toCharArray());
                    }
                });
                launchVPLDisplayer("update");
            } else if (isNetworkAvailable() || !launchedBefore||!hasSaves) {
                launchLogin();
            } else{
                launchVPLDisplayer("loadFromSaves");
            }
        }
        finish();
    }

    public int ThemeNametoThemeInt(String name, String returnType) {
        switch (name) {
            case "Blau":
                if (returnType.equals("ID")) {
                    return R.style.BlueTheme;
                } else if (returnType.equals("ARRAY")) {
                    return 0;
                }
                break;
            case "Grün":
                if (returnType.equals("ID")) {
                    return R.style.GreenTheme;
                } else if (returnType.equals("ARRAY")) {
                    return 1;
                }
                break;
            case "Rot":
                if (returnType.equals("ID")) {
                    return R.style.RedTheme;
                } else if (returnType.equals("ARRAY")) {
                    return 2;
                }
                break;
            case "Gelb":
                if (returnType.equals("ID")) {
                    return R.style.YellowTheme;
                } else if (returnType.equals("ARRAY")) {
                    return 3;

                }
                break;
            case "Grau":
                if (returnType.equals("ID")) {
                    return R.style.LightTheme;
                } else if (returnType.equals("ARRAY")) {
                    return 4;
                }
                break;
        }
        log("Error", "unable to turn Theme String into Integer", this.getClass());
        return 0;
    }
    /**
     * writes an entry to the VPLLog containing the date, the type of entry and the message and the class that called the error
     *
     * @param type    the type of message e.g("Error","Debug","Info" etc.)
     * @param message the message to enter to the log
     * @param caller  the class that called the log entry
     */
    public static void log(String type, String message, Class caller) {

        if(LogFile==null) {
            return;
            /*try {
                LogFile = new File(getApplicationContext().getExternalFilesDir(null), Main.LOG_FILE_NAME);
            }catch (NullPointerException e)
            {}*/
        }
        if(writeLog&&LogFile!=null) {

            Calendar now = Calendar.getInstance();
            String date = Integer.toString(now.get(Calendar.DAY_OF_MONTH)) + "/" + Integer.toString(now.get(Calendar.MONTH) + 1) + "/" + Integer.toString(now.get(Calendar.YEAR)) + "/" + Integer.toString(now.get(Calendar.HOUR_OF_DAY)) + "/" + Integer.toString(now.get(Calendar.MINUTE)) + "/" + Integer.toString(now.get(Calendar.SECOND)) + "/" + Integer.toString(now.get(Calendar.MILLISECOND)) + "    ";
            //clears log if size too large (>=5MB)
            if(LogFile.length()>=5000000)
            {
                try {
                    //clears credentialsSaveFile
                    PrintWriter pw1 = new PrintWriter(LogFile);
                    pw1.close();

                    Entry firstEntry = new Entry(date, Main.class.toString(),"Info","Reset Log; eula has been accepted:" + EULAAccepted);
                    writeLog(firstEntry);
                    if(EULAAccepted) {
                        Entry APIVersion = new Entry(date, Main.class.toString(), "Info", "Device SDK version" + Build.VERSION.SDK_INT);
                        writeLog(APIVersion);
                    }
                }
                catch (FileNotFoundException e) {}
            }
            Entry entry = new Entry(date, caller.toString(), type, message);
            writeLog(entry);
        }
    }
    public static void writeLog(Entry entry)
    {
        if (writeLog&&isExternalStorageWritable() && LogFile.getUsableSpace() >= 10000&&LogFile.canWrite()&&LogFile.length()<18000000) {
            try {
                FileWriter fileWriter = new FileWriter(LogFile, true);
                BufferedWriter logWriter = new BufferedWriter(fileWriter);

                //writes the entry
                //writes the date and time
                logWriter.write(entry.getDate());
                //writes the class that entered the entry
                logWriter.append(entry.getCaller() + "    ");
                //writes the type of entry
                logWriter.append(entry.getType() + "    ");
                //writes the message
                logWriter.append(entry.getMessage());
                logWriter.append("   " + System.getProperty("line.separator"));
                logWriter.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        }


    }
    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
