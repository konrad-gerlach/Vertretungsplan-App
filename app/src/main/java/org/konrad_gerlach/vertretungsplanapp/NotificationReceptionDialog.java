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
import android.os.*;
import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
/**
 * Created by User on 02.08.2018.
 */

public class NotificationReceptionDialog extends Dialog implements View.OnClickListener {
    public VPLLogin c;
    public Dialog d;
    public Button yes, no;
    public NotificationReceptionDialog(VPLLogin a)
    {
        super(a);
        c=a;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.receive_notifications_dialog);
        yes=(Button) findViewById(R.id.ConfirmButton1);
        no=(Button) findViewById(R.id.DenyButton1);
        yes.setOnClickListener(this);
        no.setOnClickListener(this);

    }
    @Override
    public void onClick(View v)
    {
        Main main = new Main();
        switch (v.getId()) {
            case R.id.ConfirmButton1:
                EditText className =(EditText) findViewById(R.id.klassenname_init);
                Main.receiveNotifications = true;
                Main.className=className.getText().toString();
                c.notificationConfirm(className.getText().toString());
                main.createChannel(this.getContext());
                Main.log("Info","Confirmed Notification Reception",this.getClass());
                break;
            case R.id.DenyButton1:
                Main.log("Info","Denied Notification Reception",this.getClass());
                break;
        }
        dismiss();


    }

}
