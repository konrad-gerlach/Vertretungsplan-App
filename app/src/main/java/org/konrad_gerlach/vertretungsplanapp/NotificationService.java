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

/**
 * Created by User on 30.04.2018.
 */

import android.app.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import android.content.SharedPreferences;
import android.os.IBinder;
import android.content.Intent;

import java.io.File;
import java.util.*;
import java.text.*;

import androidx.core.app.*;

import org.konrad_gerlach.vertretungsplanapp.security.Cryptography;

public class NotificationService extends Service{

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        runService();
        return START_STICKY;
    }
    public void runService()
    {
        new NotificationCore().run(this);
    }




}