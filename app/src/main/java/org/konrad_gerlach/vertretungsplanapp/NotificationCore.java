package org.konrad_gerlach.vertretungsplanapp;/*
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


import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.konrad_gerlach.vertretungsplanapp.security.Cryptography;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationCore {
    boolean firstNotification=true;
    private int summaryId=0;
    private int notificationId =1;
    private String className ="";
    private Calendar displayForDate;
    //altered notifications text depending on it being displayed for the current or the next day ("Heute"<-->"Morgen")
    private boolean designatedForNextDay=false;
    public void run(Context context)
    {
        designatedForNextDay=false;
        Calendar now;
        //the day for which to display notifications; if the current time is later than 18:00, then it will display notifications for the next day
        displayForDate= Calendar.getInstance();

        if((now=Calendar.getInstance()).get(Calendar.HOUR_OF_DAY)>=18)
        {
            designatedForNextDay=true;
            displayForDate.add(Calendar.DAY_OF_YEAR,1);
        }

        boolean credentialsReadSuccess = false;
        boolean receiveNotifications=false;

        File saveFile = new File(context.getFilesDir(), Main.VPL_SAVE_FILE_NAME);
        Main.LogFile = new File(context.getExternalFilesDir(null), Main.LOG_FILE_NAME);
        //gets the Information from the prefs file
        SharedPreferences settingsFile = context.getSharedPreferences(
                context.getString(R.string.settings_file_key),Context.MODE_PRIVATE);
        receiveNotifications=settingsFile.getBoolean(Main.RECEIVE_NOTIFICATIONS_KEY, Main.receiveNotifications);
        className=settingsFile.getString(Main.CLASS_NAME_KEY, Main.className);
        Main.writeLog=settingsFile.getBoolean(Main.WRITE_LOG_KEY, Main.writeLog);

        Main.log("Info","running notification service",this.getClass());
        //loads username and password, if saved
        String credentialsUnsplit = Cryptography.decryptCredentials(context.getApplicationContext());
        if(!credentialsUnsplit.equals(""))
        {
            String[] credentials = credentialsUnsplit.split("\\|\\|");
            Main.websiteAddress=credentials[0];
            Main.username=credentials[1];
            Main.password=credentials[2];
            credentialsReadSuccess=true;
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (!Main.websiteAddress.equals("")&&!Main.username.equals("") && !Main.password.equals("")&& credentialsReadSuccess&&receiveNotifications&&notificationManager.areNotificationsEnabled()) {
            Main.log("Info","creating vplconnectiontask",NotificationCore.class);
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(Main.username, Main.password.toCharArray());
                }
            });
            ConnectionTaskInput Input = new ConnectionTaskInput(context, "online", saveFile);
            showVPLEventInitiator initiator = new showVPLEventInitiator();
            showVPLEventListener listener = new showVPLEventListener(null,this, false,context);
            initiator.addEventListener(listener);
            Input.setInitiator(initiator);
            VPLConnectionTask runningVPLConnectionTask = new VPLConnectionTask();
            runningVPLConnectionTask.execute(Input, null, Input);
        }
    }

    public void goOn(Context context)
    {
        Main.log("Info","going on",NotificationCore.class);
        File abbreviationsFile = new File(context.getFilesDir(), Main.ABBREVIATIONS_FILE_NAME);
        try {
            Main.loadTeachersAbbreviations(abbreviationsFile,context);
        } catch (IOException e) {
            Main.log("Error","load abbreviations failed "+e.toString(),NotificationCore.class);
        }

        //gets the notifications already shown for the displayForDate day to avoid duplicates
        File shownNotificationsFile = new File(context.getFilesDir(),Main.SHOWN_NOTIFICATIONS_FILE_NAME);
        ArrayList<String> shownNotifications = loadShownNotifications(shownNotificationsFile,context);

        Calendar calendar = Calendar.getInstance();
        //calendar.add(Calendar.DAY_OF_YEAR,+1);
        VPLDisplayer displayer = new VPLDisplayer();
        ArrayList<VPLWeek> weeks= displayer.readFromSaveFile(false,new File(context.getFilesDir(),"vplSaveFile.txt"),context.getApplicationContext());

        if(weeks!=null) {
            for (VPLWeek week : weeks){
                for (VPLDay day : week.getDays()) {
                    //tries to find out the correct year to construct the date to compare to today
                    DateFormat format = new SimpleDateFormat("dd MM  EEEE yyyy", Locale.GERMANY);
                    String daysDate1 = day.getDate();
                    daysDate1 = daysDate1.replace(".", " ");
                    //TODO if save file is exactly 1 year old, false notification may occur
                    daysDate1 = daysDate1.concat(" " + calendar.get(Calendar.YEAR));
                    boolean correctDay = false;
                    Calendar cal1 =Calendar.getInstance();
                    try {
                        Date daysDate = format.parse(daysDate1);
                        cal1.setTime(daysDate);
                        cal1.getTime();
                        //recalculates relevant fields
                        displayForDate.getTimeInMillis();
                        cal1.getTimeInMillis();
                        //checks if it is the same day of year
                        correctDay = displayForDate.get(Calendar.DAY_OF_YEAR) == cal1.get(Calendar.DAY_OF_YEAR);
                    }
                    //checks for errors within the program
                    catch (ParseException e) {
                        correctDay=false;
                        Main.log("Error", "Failed to parse Dates", this.getClass());
                    }
                    if (correctDay) {
                        //TODO add notification summary for api <24

                        Main.log("Info","messageDesignatedForNextDay "+designatedForNextDay+" displayForDate "+displayForDate.get(Calendar.DAY_OF_YEAR)+" "+format.format(displayForDate.getTime())+" "+cal1.get(Calendar.DAY_OF_YEAR)+" "+format.format(cal1.getTime()),this.getClass());
                        if (!day.getMessage().equals("")) {
                            //TODO message of the day is not displayed properly when notification is extended
                            NotificationCompat.Builder mBuilder  = new NotificationCompat.Builder(context,Main.MAIN_CHANNEL_ID);

                            String contentTitle = designatedForNextDay ? "Nachricht des morgigen Tages":"Nachricht des Tages";
                            String[] splitMessage =day.getMessage().split("\\s");
                            String messageWithNewLine ="";
                            for(int i=0;i<splitMessage.length;i++)
                            {
                                if(i%7==0&&i!=0)
                                {
                                    messageWithNewLine=messageWithNewLine.concat(System.getProperty("line.separator"));
                                }
                                messageWithNewLine+=splitMessage[i]+" ";
                            }
                            mBuilder.setSmallIcon(R.drawable.notification_icon)
                                    .setContentTitle(contentTitle)
                                    .setStyle(new NotificationCompat.BigTextStyle().bigText(messageWithNewLine))
                                    .setTimeoutAfter(14400000);
                                    //.setContentText(day.getMessage())
                                    //timeoutAfter only added in SDK Version 26, min SDK Version is 15

                                    //.setGroup(Main.NOTIFICATION_GROUP)
                                    //.setSortKey("a")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                                mBuilder.setGroup(Main.NOTIFICATION_GROUP);
                            }if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                            {
                                mBuilder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
                            }else{
                                mBuilder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL);
                            }

                            //ensures that notification has not already been displayed for this displayForDate day
                            boolean notificationPreviouslyDisplayed=false;
                            for(String previousNotification:shownNotifications)
                            {
                                if(previousNotification.trim().equals((day.getMessage()).trim()))
                                    notificationPreviouslyDisplayed=true;

                            }

                            if(!notificationPreviouslyDisplayed) {
                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                                createSummary(context);
                                // notificationId is a unique int for each notification that you must define
                                notificationManager.notify("VPLMessage", notificationId, mBuilder.build());
                                shownNotifications.add(day.getMessage());
                                notificationId++;
                            }
                        }
                        for (VPLLine line : day.getLines()) {
                            if (line.getGroup().contains(className) || line.getGroup().equals("") || line.getGroup().equals(" ")) {
                                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, Main.MAIN_CHANNEL_ID);
                                String substitutionTypeShort=line.getSubstitute();
                                String substitutionTypeLong=line.getSubstitute();
                                if(line.getSubstitute().trim().equals("+"))
                                {
                                    substitutionTypeShort="Entfall "+line.getLessons()+" Std. " + line.getReplacementSubject();
                                    substitutionTypeLong="eigenverantworliches Arbeiten";
                                }
                                else
                                {
                                    substitutionTypeShort="Vertr. bei "+line.getSubstitute()+" "+line.getLessons()+" Std. " + line.getReplacementSubject();
                                    substitutionTypeLong="Vertretung bei "+VPLDisplayer.lookUpTeacherAbbr(line.getSubstitute());
                                }
                                String contentTitle=substitutionTypeShort+(designatedForNextDay ? " morgen ":" heute ");
                                mBuilder.setSmallIcon(R.drawable.notification_icon)
                                        .setContentTitle(contentTitle)
                                        .setStyle(new NotificationCompat.BigTextStyle()
                                                .bigText(substitutionTypeLong+" in der "+line.getLessons()+" Std. " + line.getReplacementSubject()+" in "+line.getRoom()+System.getProperty("line.separator")+(line.getComment().equals("")||line.getComment()==null?"":"Kommentar:"+line.getComment())))
                                        .setTimeoutAfter(14400000);
                                        //.setGroup(Main.NOTIFICATION_GROUP)
                                        //.setSortKey("b")

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                                    mBuilder.setGroup(Main.NOTIFICATION_GROUP);
                                }if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                {
                                    mBuilder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
                                }else{
                                    mBuilder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL);
                                }
                                //ensures that notification has not already been displayed for this displayForDate day
                                boolean notificationPreviouslyDisplayed=false;
                                for(String previousNotification:shownNotifications)
                                {
                                    if(previousNotification.trim().equals((substitutionTypeShort+line.getLessons()+" Std. " + line.getReplacementSubject()+" in "+line.getRoom()+" "+line.getComment()).trim()))
                                        notificationPreviouslyDisplayed=true;

                                }

                                if(!notificationPreviouslyDisplayed) {
                                    createSummary(context);
                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                                    // notificationId is a unique int for each notification that you must define
                                    notificationManager.notify("VPLLine", notificationId, mBuilder.build());
                                    shownNotifications.add(substitutionTypeShort+line.getLessons()+" Std. " + line.getReplacementSubject()+" in "+line.getRoom()+" "+line.getComment().trim());
                                    notificationId++;
                                }
                            }
                        }

                    }
                }

            }

        }

        writeShownNotifications(shownNotifications,shownNotificationsFile,context);


    }
    private void createSummary(Context context)
    {
        if(firstNotification&&Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, Main.MAIN_CHANNEL_ID);
             mBuilder.setSmallIcon(R.drawable.notification_icon)
                     .setContentTitle("Vetretungsplan")
                     .setGroup(Main.NOTIFICATION_GROUP)
                     .setGroupSummary(true)
                .setStyle(new NotificationCompat.InboxStyle().addLine("Fehler"))
                .setTimeoutAfter(14400000);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify("VPLSummary", summaryId, mBuilder.build());
        }

        firstNotification=false;
    }
    private ArrayList<String> loadShownNotifications(File shownNotificationFile,Context context)
    {
        SharedPreferences settingsFile = context.getSharedPreferences(
                context.getString(R.string.settings_file_key),Context.MODE_PRIVATE);
        notificationId=settingsFile.getInt(Main.NOTIFICATION_ID,0);
        ArrayList<String> shownNotifications = new ArrayList<String>();
        try {

            if(!shownNotificationFile.createNewFile())
            {
                java.io.FileReader R = new java.io.FileReader(shownNotificationFile);
                java.io.BufferedReader BR = new java.io.BufferedReader(R);
                String shownNotificationsString = BR.readLine();
                if(shownNotificationsString!=null)
                    shownNotificationsString=Cryptography.decrypt(Main.SN_INIT_VECTOR,shownNotificationsString,context.getApplicationContext(),Main.SN_KEY,Main.SN_ENCRYPT_SUCCESS_KEY,Main.SN_KEY_23_AES,Main.SN_KEY_23_RSA);

                Scanner lineScanner = new Scanner(shownNotificationsString);
                lineScanner.useDelimiter("<#>");
                boolean correctDate=false;
                //checks if notifications were displayed for same displayForDate day
                if(lineScanner.hasNext()){
                    String displayForDateString=DateFormat.getDateInstance(DateFormat.SHORT).format(displayForDate.getTime());
                    if(displayForDateString.trim().equals(lineScanner.next().trim()))
                        correctDate=true;
                }
                //loads previously displayed notifications into memory to compare to notifications about to be sent
                if(correctDate) {
                    while (lineScanner.hasNext()) {
                        shownNotifications.add(lineScanner.next());
                    }
                }
                lineScanner.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return shownNotifications;
    }
    private void writeShownNotifications(ArrayList<String> shownNotifications,File shownNotificationFile,Context context)
    {
        SharedPreferences settingsFile = context.getSharedPreferences(
                context.getString(R.string.settings_file_key),Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settingsFile.edit();
        editor.putInt(Main.NOTIFICATION_ID,notificationId);
        editor.commit();
        //saves the updated shownNotifications to the file
        String shownNotificationsToSave=DateFormat.getDateInstance(DateFormat.SHORT).format(displayForDate.getTime())+"<#>";
        for(String shownNotification:shownNotifications)
        {
            shownNotificationsToSave+=shownNotification+"<#>";
        }
        shownNotificationsToSave=Cryptography.encrypt(Main.SN_INIT_VECTOR,shownNotificationsToSave,context.getApplicationContext(),Main.SN_KEY,Main.SN_ENCRYPT_SUCCESS_KEY,Main.SN_KEY_23_AES,Main.SN_KEY_23_RSA);
        try {
            PrintWriter writer = new PrintWriter(shownNotificationFile);
            writer.write(shownNotificationsToSave);
            writer.close();
        } catch (FileNotFoundException e) {
            Main.log("Error",e.toString(),this.getClass());
        }
    }
}
