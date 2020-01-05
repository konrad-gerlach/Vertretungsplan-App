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
import android.os.Message;

/**
 * Created by User on 11.10.2017.
 */
public class showVPLEventListener implements showVPLInterface {
    private VPLDisplayer instanceOfVPLDisplayer;
    private boolean show ;
    private Context context;
    private NotificationCore notificationCore;
    @Override
    public void VPLIsToBeShown()
    {
        if(show)
        {
            //returns to login screen if any errors occur
            //TODO use handler
            if(instanceOfVPLDisplayer.readFromSaveFile(true, Main.saveFile,instanceOfVPLDisplayer)==null)
            {
                Message message = VPLDisplayer.errorHandler.obtainMessage(-2,instanceOfVPLDisplayer.getApplicationContext());
                message.sendToTarget();
                instanceOfVPLDisplayer.launchLogin();
                instanceOfVPLDisplayer.finish();
            }
            else {
                instanceOfVPLDisplayer.updateSharedPref();
            }
        }
        else
        {
            notificationCore.goOn(context);
        }
    }
    public showVPLEventListener(VPLDisplayer givenInstanceOfVPLDisplayer, NotificationCore notificationCore, boolean show, Context context)
    {

            instanceOfVPLDisplayer = givenInstanceOfVPLDisplayer;
            this.show=show;
            this.notificationCore =notificationCore;
            this.context=context;
    }
}
