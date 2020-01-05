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
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.net.Authenticator;
import java.net.PasswordAuthentication;


import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import org.konrad_gerlach.vertretungsplanapp.security.Cryptography;
import org.konrad_gerlach.vertretungsplanapp.security.Validator;

public class VPLLogin extends AppCompatActivity {
    Main mainInstance = new Main();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(mainInstance.ThemeNametoThemeInt(mainInstance.theme,"ID"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Main.log("Info","Launched",this.getClass());
        androidx.appcompat.widget.Toolbar cToolbar = (androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(cToolbar);

        CheckBox credentialsCheckBox = (CheckBox) findViewById(R.id.remember_login);
        credentialsCheckBox.setChecked(Main.saveCredentials);

        //saves that it is the first time this activity has been launched or changes value of launchedBefore to true
        SharedPreferences sharedPref = this.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if(!sharedPref.getBoolean(Main.ACTIVITY_LAUNCHED_BEFORE_KEY,false)){
            // means activity was launched for the first time
            //store value in SharedPreferences as true
            editor.putBoolean(Main.ACTIVITY_LAUNCHED_BEFORE_KEY, true);
            editor.commit();
            NotificationReceptionDialog dialog = new NotificationReceptionDialog(VPLLogin.this);
            dialog.show();
            Main.log("Info","Showing Notification Reception Dialog",this.getClass());
        }


        Button login_button = (Button) findViewById(R.id.login_button);
        if(this.getIntent().getExtras().get("Action").equals("Login")) {
            login_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText websiteAddress = (EditText) findViewById(R.id.website_address);
                    EditText usernameField = (EditText) findViewById(R.id.username_edit_text);
                    EditText passwordField = (EditText) findViewById(R.id.password_edit_text);
                    if(Validator.checkString(websiteAddress.getText().toString())&&Validator.checkString(usernameField.getText().toString())&&Validator.checkString(passwordField.getText().toString())&&passwordField.getText().length()<=20&&usernameField.getText().length()<=20&&websiteAddress.getText().length()<=40) {
                        Main.websiteAddress=websiteAddress.getText().toString();
                        Main.username = usernameField.getText().toString();
                        Main.password = passwordField.getText().toString();
                        Button login_button = (Button) findViewById(R.id.login_button);
                        login_button.setEnabled(false);
                        login_button.setText("Logging in...");
                        CheckBox credentialsCheckBox = (CheckBox) findViewById(R.id.remember_login);

                        if (credentialsCheckBox.isChecked()) {
                            Main.saveCredentials = true;
                        } else {
                            Main.saveCredentials = false;
                        }
                        //clears credentials from shared preferences
                        if (!Main.saveCredentials) {
                            SharedPreferences sharedPref =getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(Main.CREDENTIALS_LOCATION,"");
                            editor.putString(Main.CREDENTIALS_INIT_VECTOR,"");
                            editor.commit();
                        }
                        Authenticator.setDefault(new Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(Main.username, Main.password.toCharArray());
                            }
                        });

                        launchVPLDisplayer();
                    }
                    else
                    {
                        Toast toast = Toast.makeText(getApplicationContext(),getString(R.string.unsupportedCharacterErrorToast),Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
            });
        }
        else
        {
            TextView header =(TextView) findViewById(R.id.Heading_text_view);
            header.setText("Speichern der Login Daten");
            login_button.setText("Speichern");
            CheckBox saveCredentialsSaveBox =(CheckBox) findViewById(R.id.remember_login);
            saveCredentialsSaveBox.setVisibility(View.GONE);
            login_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText websiteAddress = (EditText) findViewById(R.id.website_address);
                    EditText usernameField = (EditText) findViewById(R.id.username_edit_text);
                    EditText passwordField = (EditText) findViewById(R.id.password_edit_text);
                    if(Validator.checkString(websiteAddress.getText().toString())&&Validator.checkString(usernameField.getText().toString())&&Validator.checkString(passwordField.getText().toString())&&passwordField.getText().length()<=20&&usernameField.getText().length()<=20) {
                        Main.websiteAddress=websiteAddress.getText().toString();
                        Main.username = usernameField.getText().toString();
                        Main.password = passwordField.getText().toString();
                        TextView title = (TextView) findViewById(R.id.Heading_text_view);
                        title.setText("Anmeldedaten speichern");
                        Cryptography.encryptCredentials(Main.websiteAddress+"||"+Main.username+"||"+Main.password,getApplicationContext());


                        finish();
                    }
                    else
                    {
                        Toast toast = Toast.makeText(getApplicationContext(),getString(R.string.unsupportedCharacterErrorToast),Toast.LENGTH_LONG);
                        toast.show();
                    }

                }
            });
        }



    }
    public void notificationConfirm(String className) {
        //updates settings save
        SharedPreferences settingsFile = getApplicationContext().getSharedPreferences(
                getString(R.string.settings_file_key),MODE_PRIVATE);
        SharedPreferences.Editor editor = settingsFile.edit();
        editor.putBoolean(Main.RECEIVE_NOTIFICATIONS_KEY, Main.receiveNotifications);
        editor.putString(Main.CLASS_NAME_KEY,className);
        editor.commit();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater cMenuInflater = getMenuInflater();
        cMenuInflater.inflate(R.menu.toolbar_menu2,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId()==R.id.Feedback_Item2)
        {
            launchEmail();
        }
        else if(item.getItemId()==R.id.Impressum2)
        {
            launchImpressum();
        }
        else if(item.getItemId()==R.id.EULA2)
        {
            launchEULA();
        }
        else if(item.getItemId()==R.id.Settings_Item2)
        {
            launchSettings();
        }
        return super.onOptionsItemSelected(item);
    }
    private void launchSettings()
    {
        Intent intent = new Intent(this, Settings.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    public void launchVPLDisplayer()
    {

        Intent intent = new Intent(this, VPLDisplayer.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("Action","update");
        startActivity(intent);
        finish();
    }
    public void launchEmail()
    {
        if(Main.EULAAccepted) {
            AddLogDialog dialog = new AddLogDialog(VPLLogin.this);
            dialog.show();
        }else {
            Toast toast = Toast.makeText(this.getApplicationContext(),"Sie haben entweder den Allgemeinen Geschäftsbedingungen (AGB) der App oder der Datenschutzerklärung der App nicht zugestimmt oder Sie sind nicht mindestens 13 Jahre alt.",Toast.LENGTH_LONG);
            toast.show();
            Main.log("Error","User did not accept EULA",VPLLogin.class);
        }
    }
    private void launchEULA()
    {
        Intent intent = new Intent(this, EULA.class);
        intent.putExtra("Agreeable",false);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    private void launchImpressum()
    {
        Intent intent = new Intent(this, Legal_Notice.class);
        intent.putExtra("Agreeable",false);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }




}
