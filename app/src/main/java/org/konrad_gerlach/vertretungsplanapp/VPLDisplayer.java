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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView;

import org.konrad_gerlach.vertretungsplanapp.security.Cryptography;

import java.io.IOException;
import java.util.*;
import java.io.*;

public class VPLDisplayer extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    ArrayList<VPLWeek> weeks = new ArrayList<VPLWeek>();
    Integer selectedWeek = 0;
    Integer selectedDay = 0;
    Integer oldWeek = -1;
    Integer oldDay = -1;
    String selectedGroup = "";
    String oldGroup = "";
    String test = "HelloWorld";
    String lastUpdatedDay ="";
    String lastUpdatedTime ="";
    public static Boolean saveCredentials = false;
    public static String username = "";
    public static String password = "";
    public static ArrayList<Teacher_Abbreviations_Storage> abbreviations=new ArrayList<Teacher_Abbreviations_Storage>();
    Main mainInstance = new Main();
    public static Handler errorHandler;
    int receivedErrors=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(mainInstance.ThemeNametoThemeInt(mainInstance.theme,"ID"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpldisplayer);
        androidx.appcompat.widget.Toolbar cToolbar = (androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar1);
        setSupportActionBar(cToolbar);
        Main.LogFile = new File(this.getExternalFilesDir(null), Main.LOG_FILE_NAME);



        //called when the user uses a swipe to refresh screen, updates VPL information displayed
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Main.log("Info","Swipe refresh initiated",VPLDisplayer.class);
                        refreshVPL();
                    }
                }
        );




        Main.log("Info","Launched",this.getClass());
        //starts internet connection to
        if(this.getIntent().getExtras().get("Action").equals("update")&&isNetworkAvailable())
        {
            swipeRefreshLayout.setRefreshing(true);
            refreshVPL();
        }
        else
        {
            swipeRefreshLayout.setRefreshing(true);
            /*Context context = getApplicationContext();
            CharSequence text = "Läd Vertretungsplan ...";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context,text,duration);
            toast.show();*/

            //returns to login screen if any errors occur during loading
            if(readFromSaveFile(true, Main.saveFile,this)==null)
            {
                Message message =errorHandler.obtainMessage(-2,this);
                message.sendToTarget();
                launchLogin();
                finish();
            }
            else
            {
                updateSharedPref();
            }
        }


    }
    public void refreshVPL()
    {
        /*Context context = getApplicationContext();
        CharSequence text = "Aktualisierung läuft ...";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context,text,duration);
        toast.show();*/
        errorHandler =new Handler(Looper.getMainLooper())
        {
            @Override
            public void handleMessage(Message inputMessage) {
                if(receivedErrors<1) {
                    receivedErrors++;
                    String errorMessage;
                    switch (inputMessage.what) {
                        case 404:
                            errorMessage = "Es ist momentan kein Vertretungsplan online verfügbar.";
                            break;
                        case 401:
                            errorMessage = "Die Anmeldedaten sind inkorrekt. Bitte geben Sie diese erneut ein";
                            break;
                        case -1:
                            errorMessage = "Ein unbekannter Fehler ist bei der Verbindung zur Website erfolgt";
                            break;
                        case -2:
                            errorMessage="Vertretungsplan konnte nicht geladen werden";
                            break;
                        default:
                            errorMessage = "Ein unbekannter Fehler ist bei der Verbindung zur Website erfolgt";
                            break;
                    }
                    displayError(errorMessage, (Context) inputMessage.obj);
                }

            }
        };
        ConnectionTaskInput Input = new ConnectionTaskInput(this,"online", mainInstance.saveFile);
        showVPLEventInitiator initiator = new showVPLEventInitiator();
        showVPLEventListener listener = new showVPLEventListener(this,null,true,this.getApplicationContext());
        initiator.addEventListener(listener);
        UpdateErrorInitiator errorInitiator = new UpdateErrorInitiator();
        UpdateErrorListener errorListener = new UpdateErrorListener(this);
        errorInitiator.addEventListener(errorListener);
        Input.setErrorInitiator(errorInitiator);
        Input.setInitiator(initiator);
        VPLConnectionTask runningVPLConnectionTask = new VPLConnectionTask();
        runningVPLConnectionTask.execute(Input, null, new ConnectionTaskInput(this,"online", mainInstance.saveFile));

    }
    public void updateSharedPref()
    {
        //successfully loaded VPL therefore valid saves exist
        //updates sharedPreferences to include value for has Saves =true
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), this.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        if(!sharedPref.contains(Main.HAS_SAVES_KEY)) {
            editor.putBoolean(Main.HAS_SAVES_KEY, true);
            editor.commit();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater cMenuInflater = getMenuInflater();
        cMenuInflater.inflate(R.menu.toolbar_menu1,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.Settings_Item1)
        {
            launchSettings();
        }
        else if(item.getItemId()==R.id.Lehrerkürzel_Item)
        {
            launchAbbrEditor();
        }
        else if(item.getItemId()==R.id.Feedback_Item1)
        {
            launchEmail();
        }
        else if(item.getItemId()==R.id.Impressum1)
        {
            launchImpressum();
        }
        else if(item.getItemId()==R.id.EULA1)
        {
            launchEULA();
        }else if(item.getItemId()==R.id.menu_refresh)
        {
            Main.log("Info","manual refresh initiated",VPLDisplayer.class);
            //called when the user uses a swipe to refresh screen, updates VPL information displayed
            SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefresh);
            swipeRefreshLayout.setRefreshing(true);
            refreshVPL();
        }
        return super.onOptionsItemSelected(item);
    }
    /**
     * initializes the displaying of the VPL by loading all the relevant data from the respective save Files and displaying iz
     */
    public ArrayList<VPLWeek> readFromSaveFile(boolean display,File saveFile,Context context)
    {

        try {
            //loads data from the saveFile containing the VPL
            Main.log("Info","Reading VPL from save File",this.getClass());
            java.io.FileReader R1 = new java.io.FileReader(saveFile);
            java.io.BufferedReader BR1 = new java.io.BufferedReader(R1);
            String readString2 = "";
            String currentLine;
            while((currentLine=BR1.readLine())!=null)
            {
                readString2 = readString2.concat(currentLine);
            }
            //decrypts the files content
            readString2= Cryptography.decrypt(Main.SF_INIT_VECTOR,readString2,context,Main.SF_KEY,Main.SF_ENCRYPT_SUCCESS_KEY,Main.SF_KEY_23_AES,Main.SF_KEY_23_RSA);
            if(readString2=="")
            {
                Main.log("Error", "Save File Not Complete: maybe first time running app and no InternetConnection or error decrypting or encrypting file?", this.getClass());
                return null;
            }
            //loads the files content
            Scanner fileScanner = new Scanner(readString2);
            fileScanner.useDelimiter("<DateDelimiter>");


            if(fileScanner.hasNext())
            {
                lastUpdatedDay=fileScanner.next();

            }else{
                Main.log("Error", "Save File Not Complete: maybe first time running app and no InternetConnection? 1", this.getClass());

                return null;
            }
            if(fileScanner.hasNext())
            {
                lastUpdatedTime=fileScanner.next();
            }
            else{
                Main.log("Error", "Save File Not Complete: maybe first time running app and no InternetConnection? 2", this.getClass());
                return null;
            }
            fileScanner.useDelimiter("<newWeek>");
            //goes through all the lines of the file in order to get the information on one week
            String readString;
            while (fileScanner.hasNext()) {
                readString=fileScanner.next();
                //removes the bug, that the date will contain the delimiter <newWeek> and that there will be an additional week added containing only the delimiter <newWeek> in date by removing the first occurence of the delimiter <newWeek> , as it is no longer here
                readString=readString.replace("<newWeek>","");
                if(!readString.equals(""))
                {
                    Scanner scanner = new Scanner(readString);
                    VPLWeek week = new VPLWeek();
                    scanner.useDelimiter("<delimiter>");
                    if (scanner.hasNext()) {
                        week.setWeekType(scanner.next());
                    }
                    else{
                        Main.log("Error", "Save File Not Complete 1a", this.getClass());

                        return null;
                    }

                    if (scanner.hasNext()) {
                        week.setValidity(scanner.next());
                    }
                    else{
                        Main.log("Error", "Save File Not Complete 1b", this.getClass());

                        return null;
                    }
                    scanner.useDelimiter("<dayEnd>");
                    //gets the information for one day of the week
                    while (scanner.hasNext()) {
                        VPLDay day = new VPLDay();
                        Scanner dayScanner = new Scanner(scanner.next());
                        dayScanner.useDelimiter("<delimiter>");
                        //gets the day's date from the save File
                        if (dayScanner.hasNext()) {
                            day.setDate(dayScanner.next());
                        }
                        else{
                            Main.log("Error", "Save File Not Complete 2", this.getClass());

                            return null;
                        }
                        //gets the day's message from the save File
                        if (dayScanner.hasNext()) {
                            day.setMessage(dayScanner.next());
                        }
                        else{
                            Main.log("Error", "Save File Not Complete 2", this.getClass());

                            return null;
                        }
                        //gets whether or not there is a substitution plan for that day
                        if (dayScanner.hasNext()) {
                            day.setAvailable(Boolean.parseBoolean(dayScanner.next()));
                        }
                        else{

                            Main.log("Error", "Save File Not Complete 3", this.getClass());

                            return null;
                        }
                        //gets the individual lines
                        dayScanner.useDelimiter("<lineEnd>");
                        if (day.getAvailable()) {
                            while (dayScanner.hasNext()) {
                                VPLLine line = new VPLLine();
                                String lineText = dayScanner.next();
                                lineText=lineText.replace("<delimiter>","");
                                Scanner lineScanner = new Scanner(lineText);
                                lineScanner.useDelimiter("<lineDelimiter>");
                                if (lineScanner.hasNext()){
                                    String temp = lineScanner.next();
                                    if(!temp.equals("Empty"))
                                    {
                                        line.setGroup(temp);
                                    }else
                                    {line.setGroup("");}

                                }
                                else{

                                    Main.log("Error", "Save File Not Complete 4", this.getClass());

                                    return null;
                                }
                                if (lineScanner.hasNext()) {
                                    line.setDate(lineScanner.next());
                                }
                                else{

                                    Main.log("Error", "Save File Not Complete 5", this.getClass());

                                    return null;
                                }
                                if (lineScanner.hasNext()) {
                                    line.setLessons(lineScanner.next());
                                }
                                else{

                                    Main.log("Error", "Save File Not Complete 6", this.getClass());

                                    return null;
                                }
                                if (lineScanner.hasNext()) {
                                    line.setSubstitute(lineScanner.next());
                                }
                                else{

                                    Main.log("Error", "Save File Not Complete 7", this.getClass());

                                    return null;
                                }
                                if (lineScanner.hasNext())
                                {
                                    line.setReplacementSubject(lineScanner.next());
                                }
                                else{

                                    Main.log("Error", "Save File Not Complete 8", this.getClass());

                                    return null;
                                }
                                    if (lineScanner.hasNext()) {
                                        line.setRoom(lineScanner.next());
                                    }
                                    else{

                                        Main.log("Error", "Save File Not Complete 9", this.getClass());

                                        return null;
                                    }
                                    if (lineScanner.hasNext()) {
                                        line.setType(lineScanner.next());
                                    }
                                    else{

                                        Main.log("Error", "Save File Not Complete 10", this.getClass());

                                        return null;
                                    }
                                    if (lineScanner.hasNext()) {
                                        line.setSubject(lineScanner.next());
                                    }
                                    else{

                                        Main.log("Error", "Save File Not Complete 11", this.getClass());

                                        return null;
                                    }
                                    if (lineScanner.hasNext()) {
                                        line.setComment(lineScanner.next());
                                    }
                                    else{

                                        Main.log("Error", "Save File Not Complete 12", this.getClass());

                                        return null;
                                    }
                                    if (lineScanner.hasNext()) {
                                        line.setN_Druck(lineScanner.next());
                                    }
                                    else{

                                        Main.log("Error", "Save File Not Complete 13", this.getClass());

                                        return null;
                                    }

                                ArrayList<VPLLine> allLines = day.getLines();
                                allLines.add(line);
                                day.setLines(allLines);
                            }


                        }

                        ArrayList<VPLDay> allDays = week.getDays();
                        allDays.add(day);
                        week.setDays(allDays);
                    }

                    scanner.close();
                    weeks.add(week);
                }
            }
            fileScanner.close();
        }
        catch(IOException e)
        {

            Main.log("Error", "error occured while interaction with the saveFile: IOException " + e, this.getClass());
            return null;
        }
        if(display) {
            //shows the date the VPL was last updated
            TextView lastUpdatedTextView = (TextView) findViewById(R.id.LastUpdated_TextView);
            lastUpdatedTextView.setText("zuletzt aktualisiert am " + lastUpdatedDay + " um " + lastUpdatedTime);
            //creates the weekSpinner
            Spinner weekSpinner = (Spinner) findViewById(R.id.week_spinner);
            weekSpinner.setOnItemSelectedListener(this);
            ArrayList<CharSequence> weekNames = new ArrayList<CharSequence>();
            //adds the weeks
            weekNames.add(weeks.get(0).getDays().get(0).getDate() + "-" + weeks.get(0).getDays().get(4).getDate());
            if (weeks.size() > 1) {
                weekNames.add(weeks.get(1).getDays().get(0).getDate() + "-" + weeks.get(1).getDays().get(4).getDate());
            }
            ArrayAdapter<CharSequence> week_adapter = new ArrayAdapter<CharSequence>(this, R.layout.custom_spinner_item, weekNames);

            weekSpinner.setAdapter(week_adapter);
            weekSpinner.setVisibility(View.VISIBLE);
            //creates day selection spinner
            Spinner daySpinner = (Spinner) findViewById(R.id.day_spinner);
            daySpinner.setOnItemSelectedListener(this);
            ArrayList<CharSequence> dayNames = new ArrayList<CharSequence>();
            //adds the names of the days in the week to dayNames only if that day has an available substitution plan
            if (weeks.get(selectedWeek).getDays().get(0).getAvailable()) {
                dayNames.add("Montag");
            }
            if (weeks.get(selectedWeek).getDays().get(1).getAvailable()) {
                dayNames.add("Dienstag");
            }
            if (weeks.get(selectedWeek).getDays().get(2).getAvailable()) {
                dayNames.add("Mittwoch");
            }
            if (weeks.get(selectedWeek).getDays().get(3).getAvailable()) {
                dayNames.add("Donnerstag");
            }
            if (weeks.get(selectedWeek).getDays().get(4).getAvailable()) {
                dayNames.add("Freitag");
            }
            ArrayAdapter<CharSequence> day_adapter = new ArrayAdapter<CharSequence>(this, R.layout.custom_spinner_item, dayNames);
            daySpinner.setAdapter(day_adapter);
            daySpinner.setVisibility(View.VISIBLE);
            //creates the groupSpinner
            Spinner groupSpinner = (Spinner) findViewById(R.id.group_spinner);
            groupSpinner.setOnItemSelectedListener(this);
            ArrayList<CharSequence> groupNames = new ArrayList<CharSequence>();
            //adds the groups to the Spinner as items
            if (weeks.get(0).getDays().get(0).getAvailable()) {
                for (VPLLine line : weeks.get(0).getDays().get(0).getLines()) {
                    Boolean alreadyAdded = false;
                    String currentGroup = line.getGroup();
                    //checks if the group has already been added to the groupNames ArrayList
                    for (CharSequence group : groupNames) {
                        String stringGroup = group.toString();
                        if (stringGroup.equals(currentGroup)) {
                            alreadyAdded = true;
                        }
                    }
                    //only adds the group to the ArrayList groupNames if it has not already been added
                    if (!alreadyAdded) {
                        groupNames.add(currentGroup);
                    }

                }
            }
            ArrayAdapter<CharSequence> group_adapter = new ArrayAdapter<CharSequence>(this, R.layout.custom_spinner_item, groupNames);
            groupSpinner.setAdapter(group_adapter);
            groupSpinner.setVisibility(View.VISIBLE);
            selectedWeek = 0;
            selectedDay = 0;
            if (groupNames.size() > 0) {
                selectedGroup = groupNames.get(0).toString();
            }
            SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefresh);
            swipeRefreshLayout.setRefreshing(false);
            updateDisplay();
        }
        return weeks;
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
    {
        switch(parent.getId())
        {
            case R.id.week_spinner:
                selectedWeek = pos;
                break;
            case R.id.day_spinner:
                ArrayAdapter<CharSequence> dayAdapter = (ArrayAdapter<CharSequence>) parent.getAdapter();
                String day= dayAdapter.getItem(pos).toString();
                selectedDay=dayNametoDayInt(day);
                break;
            case R.id.group_spinner:
                ArrayAdapter<CharSequence> groupAdapter = (ArrayAdapter<CharSequence>) parent.getAdapter();
                selectedGroup= groupAdapter.getItem(pos).toString();
                break;
        }
        updateDisplay();
    }

    /**
     * converts a String with the name of a day into an Integer position representing that days position within the week starting with 0 for example "Montag"->0
     * @param dayname the name of the day as a String
     * @return the position of dayname within the week starting with 0
     */
    private Integer dayNametoDayInt(String dayname)
    {
        int Output=-1 ;
        switch(dayname)
        {
            case "Montag":Output=0;break;
            case "Dienstag":Output=1;break;
            case "Mittwoch":Output=2;break;
            case "Donnerstag":Output=3;break;
            case "Freitag":Output=4;break;
        }
        return Output;
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
    }
    private void updateDisplay()
    {
        VPLDay day =  weeks.get(selectedWeek).getDays().get(selectedDay);

        //updates the daySpinner if the selected week has been changed to show only the days with an available substitution plan
        if(!oldWeek.equals(selectedWeek))
        {
            Spinner daySpinner = (Spinner) findViewById(R.id.day_spinner);
            ArrayList<CharSequence> dayNames = new ArrayList<CharSequence>();
            //adds the names of the days in the week to dayNames only if that day has an available substitution plan
            if(weeks.get(selectedWeek).getDays().get(0).getAvailable()||(!weeks.get(selectedWeek).getDays().get(0).getMessage().equals("")&&!weeks.get(selectedWeek).getDays().get(0).getMessage().equals(" ")))
            {
                dayNames.add("Montag");
            }
            if(weeks.get(selectedWeek).getDays().get(1).getAvailable()||(!weeks.get(selectedWeek).getDays().get(1).getMessage().equals("")&&!weeks.get(selectedWeek).getDays().get(1).getMessage().equals(" ")))
            {
                dayNames.add("Dienstag");
            }
            if(weeks.get(selectedWeek).getDays().get(2).getAvailable()||(!weeks.get(selectedWeek).getDays().get(2).getMessage().equals("")&&!weeks.get(selectedWeek).getDays().get(2).getMessage().equals(" ")))
            {
                dayNames.add("Mittwoch");
            }
            if(weeks.get(selectedWeek).getDays().get(3).getAvailable()||(!weeks.get(selectedWeek).getDays().get(3).getMessage().equals("")&&!weeks.get(selectedWeek).getDays().get(3).getMessage().equals(" ")))
            {
                dayNames.add("Donnerstag");
            }
            if(weeks.get(selectedWeek).getDays().get(4).getAvailable()||(!weeks.get(selectedWeek).getDays().get(4).getMessage().equals("")&&!weeks.get(selectedWeek).getDays().get(4).getMessage().equals(" ")))
            {
                dayNames.add("Freitag");
            }
            ArrayAdapter<CharSequence> day_adapter = new ArrayAdapter<CharSequence>(this,R.layout.custom_spinner_item,dayNames);
            daySpinner.setAdapter(day_adapter);
            TextView validFrom = findViewById(R.id.validFrom);
            validFrom.setText(weeks.get(selectedWeek).getValidity());
            validFrom.setVisibility(View.VISIBLE);
        }
        //updates the groupSpinner only if a change has occured to either the selected week or the selected Day
        if(!oldWeek.equals(selectedWeek)||!oldDay.equals(selectedDay))
        {
            ArrayList<CharSequence> groupNames = new ArrayList<>();
            //only adds group names to groupNames if that day has a substitution plan, else groupNames remains empty
            if (day.getAvailable())
            {
                for (VPLLine line :day.getLines())
                {
                    Boolean alreadyAdded = false;
                    String currentGroup = line.getGroup();
                    //checks if the group has already been added to the groupNames ArrayList
                    for (CharSequence group : groupNames) {
                        String stringGroup = group.toString();
                        if (stringGroup.equals(currentGroup))
                        {
                            alreadyAdded = true;
                        }
                    }
                    //only adds the group to the ArrayList groupNames if it has not already been added
                    if (!alreadyAdded)
                    {
                        groupNames.add(currentGroup);
                    }

                }
            }
            ArrayAdapter<CharSequence> group_adapter = new ArrayAdapter<CharSequence>(this,R.layout.custom_spinner_item,groupNames);
            Spinner groupSpinner = (Spinner) findViewById(R.id.group_spinner);

            groupSpinner.setAdapter(group_adapter);
        }
        //displays the data in the table
        if(!oldWeek.equals(selectedWeek)||!oldDay.equals(selectedDay)||!oldGroup.equals(selectedGroup))
        {
            TextView dateView = (TextView) findViewById(R.id.date_TextView);
            dateView.setText("");
            dateView.setVisibility(View.GONE);
            TextView messageView = (TextView) findViewById(R.id.message_TextView);
            messageView.setText("");
            messageView.setVisibility(View.GONE);
            if(!day.getMessage().equals("")&&!day.getMessage().equals(" ")) {
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(day.getMessage());
            }
            TableLayout table = (TableLayout) findViewById(R.id.Table);
            TableRow names = (TableRow) table.getChildAt(0);
            table.removeAllViews();
            table.addView(names);

            if(day.getAvailable()) {
                table.setVisibility(View.VISIBLE);
                dateView.setVisibility(View.VISIBLE);

                dateView.setText(day.getDate());

                for (VPLLine currentLine : day.getLines()) {
                    if (currentLine.getGroup().equals(selectedGroup)) {
                        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(0, 0, 10, 10);
                        TableLayout.LayoutParams rowLayout = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        rowLayout.setMargins(5, 5, 5, 5);
                        TableRow row = new TableRow(this);
                        TextView group = new TextView(this);
                        group.setLayoutParams(layoutParams);
                        group.setText(currentLine.getGroup());
                        row.addView(group);
                        TextView date = new TextView(this);
                        date.setText(currentLine.getDate());
                        date.setLayoutParams(layoutParams);
                        row.addView(date);
                        TextView lesson = new TextView(this);
                        lesson.setText(currentLine.getLessons());
                        lesson.setLayoutParams(layoutParams);
                        row.addView(lesson);
                        TextView Substitute = new TextView(this);
                        Substitute.setText(lookUpTeacherAbbr(currentLine.getSubstitute()));

                        Substitute.setLayoutParams(layoutParams);
                        row.addView(Substitute);
                        TextView replacementSubject = new TextView(this);
                        replacementSubject.setText(currentLine.getReplacementSubject());
                        replacementSubject.setLayoutParams(layoutParams);
                        row.addView(replacementSubject);
                        TextView room = new TextView(this);
                        room.setText(currentLine.getRoom());
                        room.setLayoutParams(layoutParams);
                        row.addView(room);
                        /** deprecated: TextView type = new TextView(this);
                        type.setText(currentLine.getType());
                        type.setLayoutParams(layoutParams);
                        row.addView(type);
                        TextView subject = new TextView(this);
                        subject.setText(currentLine.getSubject());
                        subject.setLayoutParams(layoutParams);
                        row.addView(subject);*/
                        TextView comment = new TextView(this);
                        comment.setText(currentLine.getComment());
                        comment.setLayoutParams(layoutParams);
                        row.addView(comment);
                        table.addView(row);
                    }

                }
            }
            else
            {
                table.setVisibility(View.GONE);
            }
        }
        oldWeek=selectedWeek;
        oldDay=selectedDay;
        oldGroup=selectedGroup;
    }
    public void displayError(String errorDesc,Context context)
    {
        CharSequence text = errorDesc;
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context,text,duration);
        toast.show();
    }
    private void launchSettings()
    {
        Intent intent = new Intent(this, Settings.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    private void launchAbbrEditor()
    {
        Intent intent = new Intent(this, Teacher_Abbreviation_Editor.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    private boolean isNetworkAvailable()
    {
        ConnectivityManager manager =(ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        try{NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();}
        catch(NullPointerException e)
        {
            Main.log("Error","NullPointerException occured while accessing network state"+e,this.getClass());
        }
        return false;
    }
    public void launchEmail()
    {
        if(Main.EULAAccepted) {
            AddLogDialog dialog = new AddLogDialog(VPLDisplayer.this);
            dialog.show();
        }else {
            Toast toast = Toast.makeText(this.getApplicationContext(),"Sie haben entweder den Allgemeinen Geschäftsbedingungen (AGB) der App oder der Datenschutzerklärung der App nicht zugestimmt oder Sie sind nicht mindestens 13 Jahre alt.",Toast.LENGTH_LONG);
            toast.show();
            Main.log("Error","User did not accept EULA",VPLDisplayer.class);
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
    public void launchLogin() {
        Intent intent = new Intent(this, VPLLogin.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("Action", "Login");
        startActivity(intent);
    }
    public static String lookUpTeacherAbbr(String abbreviation)
    {
        //replaces the abbreviation of the teachers name found on the online VPL with their real name
        for(Teacher_Abbreviations_Storage abbr :abbreviations)
        {
            if(abbreviation.equals(abbr.getShortHand()))
            {
                return abbr.getName();
            }
        }
        return abbreviation;
    }


}
