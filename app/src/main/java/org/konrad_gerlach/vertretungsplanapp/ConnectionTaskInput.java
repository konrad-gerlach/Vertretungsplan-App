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
 * Created by User on 22.09.2017.
 */
import java.util.*;
import java.io.File;
import android.content.Context;
public class ConnectionTaskInput {

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    private String mode ="";
    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public File getSaveFile() {
        return saveFile;
    }

    public void setSaveFile(File saveFile) {
        this.saveFile = saveFile;
    }

    private File saveFile ;
    private Context context=null;
    private ArrayList<VPLWeek> weekArray =new ArrayList<VPLWeek>();
    private showVPLEventInitiator initiator = new showVPLEventInitiator();
    private UpdateErrorInitiator errorInitiator = new UpdateErrorInitiator();
    private boolean show = false;
    public ConnectionTaskInput(Context context,String mode,File saveFile)
    {
        this.context=context;
        this.mode=mode;
        this.saveFile=saveFile;
    }
    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }






    public UpdateErrorInitiator getErrorInitiator() {
        return errorInitiator;
    }
    public void setErrorInitiator(UpdateErrorInitiator errorInitiator) {
        this.errorInitiator = errorInitiator;
    }
    public ArrayList<VPLWeek> getWeekArray() {
        return weekArray;
    }

    public void setWeekArray(ArrayList<VPLWeek> weekArray) {
        this.weekArray = weekArray;
    }
    public showVPLEventInitiator getInitiator ()
    {
        return initiator;
    }
    public void setInitiator(showVPLEventInitiator newInitiator)
    {
        initiator=newInitiator;
    }
}
