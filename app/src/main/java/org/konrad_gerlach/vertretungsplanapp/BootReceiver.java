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
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

/**
 * Created by User on 04.05.2018.
 */

public class BootReceiver extends BroadcastReceiver {
    //TODO check if additional support for HTC devices is required quickboot.poweron etc.
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            SharedPreferences settingsFile = context.getSharedPreferences(
                    context.getString(R.string.settings_file_key),Context.MODE_PRIVATE);
            Main.writeLog=settingsFile.getBoolean(Main.WRITE_LOG_KEY, Main.writeLog);
            Main.log("Info","boot receiver activated",BootReceiver.class);
            //uses alarmmanager to schedule notification service on api less than 21
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                scheduleAlarm(context);
            }
            //creates a notification job if api level is greater than or equal to 21
            else {
                BootReceiver.scheduleJob(context);
            }
        }
        else {
            Main.log("Info", "Attempted Spoofing", this.getClass());
        }
    }
    public static void scheduleAlarm(Context context)
    {
        Calendar now = Calendar.getInstance();
        //multiple repeating alarms will be used in the morning to try to display the VPL as close to its time of publication as possible
        //regular alarm at 7:25 starting the next day
        Calendar morning1 = Calendar.getInstance();
        morning1.add(Calendar.DAY_OF_YEAR,1);
        morning1.set(Calendar.HOUR_OF_DAY,7);
        morning1.set(Calendar.MINUTE,25);
        //regular alarm at 7:45 starting the next day
        Calendar morning2=Calendar.getInstance();
        morning2.add(Calendar.DAY_OF_YEAR,1);
        morning2.set(Calendar.HOUR_OF_DAY,7);
        morning2.set(Calendar.MINUTE,45);
        //regular alarm at 7:50 starting the next day
        Calendar morning3 = Calendar.getInstance();
        morning3.add(Calendar.DAY_OF_YEAR,1);
        morning3.set(Calendar.HOUR_OF_DAY,7);
        morning3.set(Calendar.MINUTE,50);
        //regular alarm at 7:55 starting the next day
        Calendar morning4 = Calendar.getInstance();
        morning4.add(Calendar.DAY_OF_YEAR,1);
        morning4.set(Calendar.HOUR_OF_DAY,7);
        morning4.set(Calendar.MINUTE,55);

        //there will also be an hourly alarm to check for updates throughout the day
        Calendar cal = Calendar.getInstance();

        Intent intent1 = new Intent(context, NotificationService.class);
        PendingIntent pintent = PendingIntent.getService(context, 0, intent1, 0);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) {
            //offsets alarms by random interval up to 2 minutes to avoid crashing server with requests
            //TODO add jitter separately to main alarm
            alarm.setRepeating(AlarmManager.RTC,morning1.getTimeInMillis()+Math.round(Math.random()*120000),AlarmManager.INTERVAL_DAY,pintent);
            alarm.setRepeating(AlarmManager.RTC,morning2.getTimeInMillis()+Math.round(Math.random()*120000),AlarmManager.INTERVAL_DAY,pintent);
            alarm.setRepeating(AlarmManager.RTC,morning3.getTimeInMillis()+Math.round(Math.random()*120000),AlarmManager.INTERVAL_DAY,pintent);
            alarm.setRepeating(AlarmManager.RTC,morning4.getTimeInMillis()+Math.round(Math.random()*120000),AlarmManager.INTERVAL_DAY,pintent);
            alarm.setRepeating(AlarmManager.RTC, cal.getTimeInMillis() +Math.round(Math.random()*120000)+ 100, AlarmManager.INTERVAL_HOUR, pintent);
        } else {
            Main.log("Error", "AlarmManager==null", BootReceiver.class);
        }
    }

    //TODO add random delay to network access
    @TargetApi(21)
    public static void scheduleJob(Context context)
    {
        SharedPreferences settingsFile = context.getSharedPreferences(
                context.getString(R.string.settings_file_key),Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settingsFile.edit();
        int jobId = settingsFile.getInt(Main.JOB_ID,0);
        JobInfo.Builder builder = new JobInfo.Builder(jobId,new ComponentName(context,NotificationJobService.class))
                .setPeriodic(3600000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(false);

        JobScheduler jobScheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }
}
