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
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;

@TargetApi(21)
public class NotificationJobService extends JobService {
    Context context;
    Thread thread;
    @Override
    public boolean onStartJob(JobParameters params)
    {
        context=this.getApplicationContext();
        //Main.log("Info","started job",NotificationJobService.class);
        Intent intent1 = new Intent(context, NotificationService.class);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                NotificationCore core = new NotificationCore();
                core.run(context);
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //context.startService(intent1);
        return true;
    }
    //TODO is this compliant?
    @Override
    public boolean onStopJob(JobParameters params) {
        if(thread!=null) {
            try {

                Main.log("Warning","job stopped",NotificationJobService.class);
                thread.join();
            } catch (InterruptedException e) {
                Main.log("Error",e.toString(),NotificationJobService.class);
            }
        }
        return false;
    }
}
