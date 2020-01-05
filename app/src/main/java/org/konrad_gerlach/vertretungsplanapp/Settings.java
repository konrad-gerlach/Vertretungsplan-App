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
import android.view.View;
import android.widget.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Settings extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    Main mainInstance = new Main();
    String selectedTheme= Main.theme;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(mainInstance.ThemeNametoThemeInt(mainInstance.theme,"ID"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //creates the theme spinner
        Spinner themeSpinner =(Spinner) findViewById(R.id.theme_Spinner);
        ArrayAdapter<CharSequence> themeAdapter = new ArrayAdapter<CharSequence>(this,R.layout.custom_spinner_item);
        themeAdapter=themeAdapter.createFromResource(this,R.array.Theme_Array,R.layout.custom_spinner_item);
        themeSpinner.setAdapter(themeAdapter);
        themeSpinner.setSelection(mainInstance.ThemeNametoThemeInt(Main.theme,"ARRAY"));
        themeSpinner.setOnItemSelectedListener(this);
        //loads the class Name EditText
        EditText classNameEdit =(EditText) findViewById(R.id.klassenraum);
        classNameEdit.setText(mainInstance.className);
        //loads the saveCredentials checkBox
        Switch saveCredentialsCheckBox = (Switch) findViewById(R.id.saveCredentials_CheckBox);
        saveCredentialsCheckBox.setChecked(Main.saveCredentials);
        //loads the writeLog checkBox
        Switch writeLogCheckBox = (Switch) findViewById(R.id.write_log_check_box);
        writeLogCheckBox.setChecked(Main.writeLog);
        //loads the receiveNotifications checkBox
        Switch receiveNotificationsBox = (Switch) findViewById(R.id.receive_notifications_checkbox);
        receiveNotificationsBox.setChecked(Main.receiveNotifications);

        Button saveButton = (Button) findViewById(R.id.save_Button);

        //the eventlistener for the save Button
        saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Button saveButton1 = (Button) findViewById(R.id.save_Button);
                    saveButton1.setEnabled(false);
                changePrefsFile(false);
                finish();
            }
        });




    }
    public void changePrefsFile(boolean shouldChange)
    {

        Switch saveCredentialsCheckBox = (Switch) findViewById(R.id.saveCredentials_CheckBox);
        Switch receiveNotificationsBox = (Switch) findViewById(R.id.receive_notifications_checkbox);
        Switch writeLogCheckBox = (Switch) findViewById(R.id.write_log_check_box);
        EditText classNameEdit = (EditText) findViewById(R.id.klassenraum);
        //launches the Login GUI if saving credentials was just enabled
        if (Main.saveCredentials != saveCredentialsCheckBox.isChecked() && saveCredentialsCheckBox.isChecked()) {
            Main.saveCredentials = saveCredentialsCheckBox.isChecked();
            Main.log("Info", "Settings were changed1", this.getClass());
            launchLogin();
        }
        //clears any record of saved credentials if application is not supposed to save credentials
        else if (!saveCredentialsCheckBox.isChecked()) {
            Main.saveCredentials = saveCredentialsCheckBox.isChecked();
            Main.log("Info", "Settings were changed2", this.getClass());
            SharedPreferences sharedPref =getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(Main.CREDENTIALS_LOCATION,"");
            editor.putString(Main.CREDENTIALS_INIT_VECTOR,"");
            editor.putBoolean(Main.CREDENTIALS_ENCRYPT_SUCCESS_KEY,false);
            editor.commit();
        }
        //in case any other changes occurred
        if(writeLogCheckBox.isChecked()!= Main.writeLog||receiveNotificationsBox.isChecked()!= Main.receiveNotifications||!selectedTheme.equals(Main.theme)||classNameEdit.getText().toString()!= Main.className)
        {
            Main.theme = selectedTheme;
            Main.receiveNotifications=receiveNotificationsBox.isChecked();
            Main.writeLog=writeLogCheckBox.isChecked();
            Main.className = classNameEdit.getText().toString();
            Main.log("Info", "Settings were changed3", this.getClass());
            reWritePrefsFile();
        }





    }
    public void reWritePrefsFile()
    {
        Main.theme = selectedTheme;
        SharedPreferences settingsFile = this.getSharedPreferences(
                getString(R.string.settings_file_key), this.MODE_PRIVATE);
        SharedPreferences.Editor editor = settingsFile.edit();
        editor.putBoolean(Main.RECEIVE_NOTIFICATIONS_KEY, Main.receiveNotifications);
        editor.putString(Main.THEME_KEY, Main.theme);
        editor.putString(Main.CLASS_NAME_KEY, Main.className);
        editor.putBoolean(Main.WRITE_LOG_KEY, Main.writeLog);

        //clears records of shown notifications to ensure compliance
        if(!Main.receiveNotifications) {
            editor.putString(Main.SN_INIT_VECTOR,"");
            editor.putBoolean(Main.SN_ENCRYPT_SUCCESS_KEY,false);
            try {
                new PrintWriter(new File(this.getFilesDir()+Main.SHOWN_NOTIFICATIONS_FILE_NAME)).close();
            }catch (FileNotFoundException e) {
                Main.log("Error", "Failed to clear show notifications file "+e.toString(), this.getClass());
            }
        }
        editor.commit();
    }
    //launches the Login Activity with the Intent of saving the credentials not accessing the VPL
    public void launchLogin ()
    {
        Intent intent = new Intent(this, VPLLogin.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("Action","saveCredentialsInFile");
        startActivity(intent);
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
    {
        if(parent.getId()==R.id.theme_Spinner)
        {
            ArrayAdapter<CharSequence> themeAdapter = (ArrayAdapter<CharSequence>) parent.getAdapter();
            selectedTheme= themeAdapter.getItem(pos).toString();
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {}
}
