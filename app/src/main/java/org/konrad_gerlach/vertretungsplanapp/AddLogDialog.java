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
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class AddLogDialog extends Dialog implements View.OnClickListener {
    public Activity c;
    public Dialog d;
    public Button yes, no;
    public AddLogDialog(Activity a)
    {
        super(a);
        c=a;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.add_log_dialog);
        yes=(Button) findViewById(R.id.ConfirmButton2);
        no=(Button) findViewById(R.id.DenyButton2);
        yes.setOnClickListener(this);
        no.setOnClickListener(this);

    }
    @Override
    public void onClick(View v)
    {
        Main main = new Main();
        switch (v.getId()) {
            case R.id.ConfirmButton2:
                launchEmail(true);
                Main.log("Info","Confirmed Add Log",this.getClass());
                break;
            case R.id.DenyButton2:
                launchEmail(false);
                Main.log("Info","Denied Add Log",this.getClass());
                break;
        }
        dismiss();


    }
    public void launchEmail(boolean addLog)
    {


        Main mainInstance = new Main();

        Intent intent=new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        String[]to={c.getString(R.string.contactEmail)};
        intent.putExtra(Intent.EXTRA_EMAIL, to);
        if(addLog) {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mainInstance.LogFile));
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, "VPL Feedback/BugReport");
        intent.putExtra(Intent.EXTRA_TEXT, c.getString(R.string.defaultEmailText));

        Intent chooser=intent.createChooser(intent, "E-mail Programm auswählen");
        c.startActivity(chooser);
    }



}
