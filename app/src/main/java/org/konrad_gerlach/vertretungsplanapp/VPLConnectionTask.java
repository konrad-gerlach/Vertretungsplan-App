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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.net.*;
import android.app.*;

import org.konrad_gerlach.vertretungsplanapp.security.Cryptography;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;


/**
 * Created by User on 14.09.2017.
 */

class VPLConnectionTask extends AsyncTask<ConnectionTaskInput,Void,ConnectionTaskInput>{

    /**
     * builds up a connection to the two websitets containing the substitution plan source code, reads their source code, and builds up two substitution plans contained in two VPLWeek objects
     * @param Input Information to pass on to the OnPostExecute method
     * @return the generated two VPLWeek objects and the Input Information
     */
    @Override
        protected ConnectionTaskInput doInBackground(final ConnectionTaskInput... Input) {
        ConnectionTaskInput Output = new ConnectionTaskInput(Input[0].getContext(),Input[0].getMode(),Input[0].getSaveFile());
        contentAndError Website1 = new contentAndError();
        contentAndError Website2 = new contentAndError();
        VPLWeek week1=new VPLWeek();
        VPLWeek week2=new VPLWeek();
        URL redirectedURL;
        URL redirectedURL2;
        if(Input[0].getMode().equals("online"))
        {
            Main.log("Info","started updating process of websites",this.getClass());
        }
        try {
           /*
            //calculates the week Of the Year based on ISO 8601
            //calculates the week in the year
            Calendar cal1 = Calendar.getInstance();
            cal1.set(Calendar.DAY_OF_WEEK,Calendar.THURSDAY);
            cal1.set(Calendar.WEEK_OF_YEAR,1);

            cal1.getTime();
            Calendar cal2 = Calendar.getInstance();
            cal2.set(Calendar.DAY_OF_WEEK,Calendar.THURSDAY);
            cal2.getTime();
            */
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setFirstDayOfWeek(GregorianCalendar.MONDAY);
            calendar.setMinimalDaysInFirstWeek(4);
            //Double tempWeek =  Math.ceil((cal2.getTimeInMillis()-cal1.getTimeInMillis())/604800000d);
            int weekInTheYear = calendar.get(GregorianCalendar.WEEK_OF_YEAR);
            String finishedWeek1 = Integer.toString(weekInTheYear);
            if(weekInTheYear<10)
            {
                finishedWeek1="0"+Integer.toString(weekInTheYear);
            }
            String finishedWeek2 = Integer.toString(weekInTheYear+1);
            if((weekInTheYear+1)<10)
            {
                finishedWeek2="0"+Integer.toString(weekInTheYear+1);
            }
            if(Input[0].getMode().equals("online")) {
                Main.log("Info", "calculated week in the year: " + finishedWeek1, this.getClass());
            }
            String baseUrl = Main.websiteAddress;
            //ensures https connection
            Pattern headerPattern = Pattern.compile( "^(http)?[s]?:?/*" );
            Matcher matcher = headerPattern.matcher( baseUrl );
            StringBuffer buffer= new StringBuffer(baseUrl.length());
            matcher.find();
            matcher.appendReplacement(buffer,"https://");
            matcher.appendTail(buffer);
            redirectedURL = new URL(buffer+"/w/" + finishedWeek1 + "/w00000.htm");
            Website1=readFromUrl(redirectedURL,Input[0]);
            redirectedURL2 = new URL(buffer+"/w/" + finishedWeek2 + "/w00000.htm");
            Website2=readFromUrl(redirectedURL2,Input[0]);
            //toast corresponding to error status codes
            if(Input[0].getMode().equals("online")) {
                if (Website1.getError().trim().equals("404") || Website2.getError().trim().equals("404")) {
                    Input[0].getErrorInitiator().trigger(404, Input[0].getContext());

                } else if (Website1.getError().trim().equals("401") || Website2.getError().trim().equals("401")) {
                    Input[0].getErrorInitiator().trigger(401, Input[0].getContext());
                }
            }
            //checks if the interaction with the Website was successful if not it logs the errors and declares the connection unsuccessful
            if(!Website1.getError().equals("")||!Website1.getError().isEmpty())
            {
                week1.setSuccessful(false);
            }
            else
            {
                if(Input[0].getMode().equals("online")) {
                    Main.log("Info","Successfully got VPL from Website at "+redirectedURL,this.getClass());
                }
                week1 = cleanVPL(Website1.getContent(),Input[0]);
            }
            if(!Website2.getError().equals("")||!Website1.getError().isEmpty())
            {
                week2.setSuccessful(false);
            }
            else {
                if (Input[0].getMode().equals("online")) {
                    Main.log("Info", "Successfully got VPL from Website at " + redirectedURL2, this.getClass());
                }
                week2 = cleanVPL(Website2.getContent(), Input[0]);
            }
        }
        catch (MalformedURLException e) {
            if(Input[0].getMode().equals("online")) {
                Main.log("Error", "Failed to properly form URL: "+e, this.getClass());
            }
            week1.setSuccessful(false);
            week2.setSuccessful(false);
        }
        Output = Input[0];
        ArrayList<VPLWeek> returnVPLWeeks =new ArrayList<VPLWeek>();
        //returns the relevant information
        returnVPLWeeks.add(week1);
        returnVPLWeeks.add(week2);
        Output.setWeekArray(returnVPLWeeks);
        return Output;
    }



    /**
     * connects to a url using an HttpUrlConnection and reads the InputStream
     * @param url the url to read from
     * @return the InputStream from that Url
     */
    //TODO authentication error not detected properly
    private contentAndError readFromUrl (URL url,ConnectionTaskInput input)
    {
        contentAndError Output = new contentAndError();
        try
        {
            //checks if Internet connection is available
            if(isNetworkAvailable(input))
            {
                //opens a connection to the url
                HttpsURLConnection openConnection = (HttpsURLConnection) url.openConnection();
                openConnection.setRequestProperty("Accept-Charset", "ISO-8859-1");
                openConnection.setRequestMethod("GET");
                openConnection.setDoOutput(true);
                openConnection.setDoInput(true);
                openConnection.connect();


                Main.log("Info", "connected to website at " + url.toString(), this.getClass());



                int responseCode = openConnection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {

                    Output.setError(Integer.toString(responseCode));
                    if(input.getMode().equals("online")) {
                        Main.log("Error", "Failed to get VPL from Website at " + url.toString() + System.getProperty("line.separator") + "Website Response Code not OK; Response Code: " + Integer.toString(responseCode), this.getClass());
                    }
                    return Output;
                }

                //changes certain settings in the Connection
                openConnection.setReadTimeout(5000);




                //reads the InputStream from the Website and saves it in the object "Output"
                BufferedReader in = new BufferedReader(new InputStreamReader(openConnection.getInputStream(),"ISO-8859-1"));
                String decodedString;
                while ((decodedString = in.readLine()) != null) {
                    Output.setContent(Output.getContent().concat(decodedString.concat(System.getProperty("line.separator"))));
                }
                in.close();
                openConnection.disconnect();
            }
            else
            {
                if(input.getMode().equals("online")) {
                    Main.log("Error", "Failed to get VPL from Website at " + url.toString() + System.getProperty("line.separator") + "No Internet Connection (Possibly erronous)", this.getClass());
                }
                Output.setError("no Internet connection");
                return Output;
            }
        }
        catch (SSLPeerUnverifiedException e)
        {
            Main.log("Error", "Failed to get VPL from Website at " + url.toString() + System.getProperty("line.separator") + "SSLPeerUnverifiedException" + e, this.getClass());
        }
        catch(IOException e)
        {
            Output.setError("Failed to connect to website for unknown reason");
            if(input.getMode().equals("online")) {
                Main.log("Error", "Failed to get VPL from Website at " + url.toString() + System.getProperty("line.separator") + "IOException " + e, this.getClass());
                if(!isNetworkAvailable(input))
                {
                    Main.log("Error","No Internet Connection (Possibly erronous)",this.getClass());
                }
            }

            return Output;
        }
        return Output;
    }
    /**
     * builds a VPLWeek object from the given HTML source code
     * @param InputVPL the HTML source code to extract the substitution plan from
     * @return the built VPLWeek object
     */
    private VPLWeek cleanVPL(String InputVPL,ConnectionTaskInput input)
    {
        VPLWeek Output = new VPLWeek();
        String workCopyofVPLCode = InputVPL;
        Scanner VPLscanner = new Scanner(workCopyofVPLCode);
        //goes to the start of the source code of the actual substitution plan
        VPLscanner.useDelimiter("<BR><div id=\"vertretung\">");
        if(VPLscanner.hasNext())
        {
            VPLscanner.next();
        }else
        {
            Output.setSuccessful(false);
            if(input.getMode().equals("online")) {
                Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.1", this.getClass());
            }
            return Output;
        }
        //skips a Line
        VPLscanner.useDelimiter(System.getProperty("line.separator"));
        if(VPLscanner.hasNext())
        {
            VPLscanner.next();
        }
        else
        {
            Output.setSuccessful(false);
            if(input.getMode().equals("online")) {
                Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.2", this.getClass());
            }
            return Output;
        }
        //extracts the week type from the sourceCode and saves it in Output.weekType
        if(VPLscanner.hasNext())
        {
            String weekAorB ="";
            weekAorB=VPLscanner.next();
            weekAorB=weekAorB.replace("<div class=\"title\">","");
            weekAorB=weekAorB.replace("</div>","");
            Output.setWeekType(weekAorB);
        }else
        {
            Output.setSuccessful(false);
            if(input.getMode().equals("online")) {
                Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.3", this.getClass());
            }
            return Output;
        }


        for(int i = 0;i<5;i++)
        {

            //gets the date of the day
            String date = "";
            //goes to the position in the header bar of the that days table and moves to the position containing that days date

            //skips a day in the header bar of the table containing the links to the other days
            VPLscanner.useDelimiter("<b>");
            if (VPLscanner.hasNext())
            {
                VPLscanner.next();
            }
            else
            {
                Output.setSuccessful(false);
                if(input.getMode().equals("online")) {
                    Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.4", this.getClass());
                }
                return Output;
            }

            VPLscanner.useDelimiter("</b>");
            if (VPLscanner.hasNext()) {
                date = VPLscanner.next();
                date = date.replace("<b>","");
            }
            else
            {
                Output.setSuccessful(false);
                if(input.getMode().equals("online")) {
                    Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.5", this.getClass());
                }
                return Output;
            }
            //skips a Line
            VPLscanner.useDelimiter(System.getProperty("line.separator"));
            if (VPLscanner.hasNext()) {
                VPLscanner.next();
            }
            else
            {
                Output.setSuccessful(false);
                if(input.getMode().equals("online")) {
                    Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.6", this.getClass());
                }
                return Output;
            }
            //skips a Line
            VPLscanner.useDelimiter(System.getProperty("line.separator"));
            if (VPLscanner.hasNext()) {
                VPLscanner.next();
            }
            else
            {
                Output.setSuccessful(false);
                if(input.getMode().equals("online")) {
                    Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.7", this.getClass());
                }return Output;
            }
            //gets the table containing the substitution occuring on the day, that is being looked at and extracts the relevant Information and saves it in a VPLDay object
            VPLscanner.useDelimiter("</table>");
            if (VPLscanner.hasNext()) {
                String table1 = VPLscanner.next();
                contentAndError message = tryToGetMessage(table1);
                if(!message.getError().equals(""))
                {
                    Output.setSuccessful(false);
                    if(input.getMode().equals("online")) {
                        Main.log("Error", "unexpected error occured while trying to get the message of the day for the day:8" + date, this.getClass());
                    }return Output;
                }
                VPLDay viewedDay = new VPLDay();
                //if there is no message before the substitution plan, the program executes the method tryToGetVPLDay on the already acquired table, in order to get the substitution plan for that day
                if(message.getContent().equals("MessageNotFound"))
                {
                    viewedDay=tryToGetVPLDay(table1, date,input);
                    if(!viewedDay.isSuccessful())
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                            Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.9", this.getClass());
                        }return Output;
                    }
                }
                //if there is a message before the substitution plan, it gets the table after that message and executes the method tryToGetVPLDay on that, in order to get the substitution plan for that day
                else
                {
                    VPLscanner.useDelimiter("<table class=\"subst\" >");
                    if(VPLscanner.hasNext())
                    {
                        VPLscanner.next();
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                            Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.10", this.getClass());
                        }
                        return Output;
                    }
                    VPLscanner.useDelimiter(System.getProperty("line.separator"));
                    if(VPLscanner.hasNext())
                    {
                        VPLscanner.next();
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                            Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.11", this.getClass());
                        }return Output;
                    }
                    VPLscanner.useDelimiter("</table>");
                    if(VPLscanner.hasNext())
                    {
                        String table2 = VPLscanner.next();
                        viewedDay=tryToGetVPLDay(table2, date,input);
                        if(!viewedDay.isSuccessful())
                        {
                            Output.setSuccessful(false);
                            if(input.getMode().equals("online")) {
                                Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.12", this.getClass());
                            }
                            return Output;
                        }
                        viewedDay.setMessage(message.getContent());
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                            Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.13", this.getClass());
                        }
                        return Output;
                    }
                }
                ArrayList<VPLDay> allVPLDays = Output.getDays();
                allVPLDays.add(viewedDay);
                Output.setDays(allVPLDays);
            }
            else
            {
                Output.setSuccessful(false);
                if(input.getMode().equals("online")) {
                    Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.14", this.getClass());
                }
                return Output;
            }
        }
        //gets the day on which the VPL Week becomes valid
        VPLscanner.useDelimiter("</div>");
        VPLscanner.next();
        VPLscanner.useDelimiter("</font>");
        VPLscanner.next();
        String validFrom ="";
        if(VPLscanner.hasNext())
        {
            validFrom=VPLscanner.next();
            validFrom=validFrom.replace("<font size=\"3\" face=\"Arial\">","");
            Output.setValidity(validFrom);
        }


        VPLscanner.close();
        return Output;
    }

    /**
     * gets all the information contained in the HTML Table, that shows one day of the substitution plan
     * @param givenVPLTable the source code (HTML) of the day to look at of the substution plan
     * @param date the date of the day to look at
     * @return returns the gathered information within a VPLDay object
     */
    private VPLDay tryToGetVPLDay(String givenVPLTable,String date,ConnectionTaskInput input)
    {
        VPLDay Output = new VPLDay();
        Output.setDate(date);
        Scanner VPLTableScanner = new Scanner(givenVPLTable);
        VPLTableScanner.useDelimiter(System.getProperty("line.separator"));
        if(VPLTableScanner.hasNext())
        {
            //only runs if there is a substitution plan for that day, it then gets all the information about that day and sets available to true, if not, it leaves available at its default false
            if(!VPLTableScanner.next().contains("<tr><td align=\"center\" colspan=\"10\" >Vertretungen sind nicht freigegeben</td></tr>"))
            {
                //goes through all the Lines individually, scanning them for the information needed for a VPLLine object
                while ( VPLTableScanner.hasNext())
                {
                    ArrayList<VPLLine> currentLines = new ArrayList<VPLLine>();
                    Output.setAvailable(true);
                    String scanned =VPLTableScanner.next();

                    Scanner LineScanner = new Scanner(scanned);

                    LineScanner.useDelimiter(">");
                    if(LineScanner.hasNext()) {
                        LineScanner.next();
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                            Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.15", this.getClass());
                        }
                        return Output;
                    }
                    LineScanner.useDelimiter("</td>");
                    //gets the classes of that line
                    ArrayList<String> classesList = new ArrayList<String>();
                    if(LineScanner.hasNext())
                    {
                        String classes = LineScanner.next();
                        classes=classes.replace("><td class=\"list\" align=\"center\">","");
                        classes=classes.replace("><td class=\"list\">","");

                        Scanner classesScanner = new Scanner(classes);
                        classesScanner.useDelimiter(", ");
                        //goes through the string containing the classes, that will have a substitution lessons and gets the individual classes

                        while(classesScanner.hasNext())
                        {
                            String currentClass = classesScanner.next();
                            currentClass=currentClass.replace("&nbsp;","");
                            classesList.add(currentClass);

                        }
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                            Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.16", this.getClass());
                        }
                        return Output;
                    }
                    //gets the date of the day the substitution occurs
                    String Date ="";
                    if(LineScanner.hasNext())
                    {
                        Date=LineScanner.next();
                        Date=Date.replace("<td class=\"list\" align=\"center\">","");
                        Date = Date.replace("<td class=\"list\">","");
                        Date = Date.replace("&nbsp;","");
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                            Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.17", this.getClass());
                        }
                        return Output;
                    }
                    //gets the lesson numbers, that will be substituted
                    String lessons="";
                    if(LineScanner.hasNext())
                    {
                        lessons = LineScanner.next();
                        lessons=lessons.replace("<td class=\"list\" align=\"center\">","");
                        lessons=lessons.replace("<td class=\"list\">","");
                        lessons=lessons.replace("&nbsp;","");
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                            Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.18", this.getClass());
                        }return Output;
                    }
                    //gets the substitution teacher
                    String substitute = "";
                    if(LineScanner.hasNext())
                    {
                        substitute = LineScanner.next();
                        substitute=substitute.replace("<td class=\"list\" align=\"center\">","");
                        substitute=substitute.replace("<td class=\"list\">","");
                        substitute=substitute.replace("&nbsp;","");
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                            Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.19", this.getClass());
                        }return Output;
                    }
                    //gets the subject, the normal subject will be replaced with
                    String replacementSubject="";
                    if(LineScanner.hasNext())
                    {
                        replacementSubject = LineScanner.next();
                        replacementSubject=replacementSubject.replace("<td class=\"list\" align=\"center\">","");
                        replacementSubject=replacementSubject.replace("<td class=\"list\">","");
                        replacementSubject=replacementSubject.replace("&nbsp;","");
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                            Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.20", this.getClass());
                        }return Output;
                    }
                    //gets the room the substitution will occur in
                    String room ="";
                    if(LineScanner.hasNext())
                    {
                        room = LineScanner.next();
                        room=room.replace("<td class=\"list\" align=\"center\">","");
                        room=room.replace("<td class=\"list\">","");
                        room=room.replace("&nbsp;","");
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                            Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.21", this.getClass());
                        }
                        return Output;
                    }
                    /** deprecated//gets the Type of the Substitution e.g. "Entfall","Vertretung","eingverantw, Arbeiten"
                    String Type ="";
                    if(LineScanner.hasNext())
                    {
                        Type = LineScanner.next();
                        Type =Type.replace("<td class=\"list\" align=\"center\">","");
                        Type=Type.replace("<td class=\"list\">","");
                        Type=Type.replace("&nbsp;","");
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                     Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.22", this.getClass());
                        }
                        return Output;
                    }
                    //gets the subject, that would normally occur
                    String subject = "";
                    if(LineScanner.hasNext() )
                    {
                        subject = LineScanner.next();
                        subject=subject.replace("<td class=\"list\" align=\"center\">","");
                        subject=subject.replace("<td class=\"list\">","");
                        subject=subject.replace("&nbsp;","");
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                     Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.23", this.getClass());
                        }
                        return Output;
                    }*/
                    //gets the comment,that has been left by the substitution teacher
                    String comment= "";
                    if(LineScanner.hasNext() )
                    {
                        comment = LineScanner.next();
                        comment=comment.replace("<td class=\"list\" align=\"center\">","");
                        comment=comment.replace("<td class=\"list\">","");
                        comment=comment.replace("&nbsp;","");
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                            Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.24", this.getClass());
                        }
                        return Output;
                    }
                    /** deprecated//gets the NDruck
                    String NDruck ="";
                    if(LineScanner.hasNext() )
                    {
                        NDruck = LineScanner.next();
                        NDruck=NDruck.replace("<td class=\"list\" align=\"center\">","");
                        NDruck=NDruck.replace("<td class=\"list\">","");
                        NDruck=NDruck.replace("&nbsp;","");
                    }
                    else
                    {
                        Output.setSuccessful(false);
                        if(input.getMode().equals("online")) {
                     Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.25", this.getClass());
                        }
                        return Output;
                    }*/
                    //builds the substitution lines
                    for(String currentClass :classesList)
                    {
                        VPLLine lineToBuild = new VPLLine();
                        lineToBuild.setGroup(currentClass);
                        lineToBuild.setDate(Date);
                        lineToBuild.setLessons(lessons);
                        lineToBuild.setSubstitute(substitute);
                        lineToBuild.setReplacementSubject(replacementSubject);
                        lineToBuild.setRoom(room);
                        //lineToBuild.setType(Type);
                        //lineToBuild.setSubject(subject);
                        lineToBuild.setComment(comment);
                        //lineToBuild.setN_Druck(NDruck);
                        currentLines.add(lineToBuild);
                    }
                    ArrayList<VPLLine> allLines = Output.getLines();
                    allLines.addAll(currentLines);
                    Output.setLines(allLines);
                    LineScanner.close();
                }
            }
        }
        else
        {
            Output.setSuccessful(false);
            if(input.getMode().equals("online")) {
                Main.log("Error", "Unvollständiger Source Code der VPLWebsite, eventuelle Internetverbindungsprobleme.26", this.getClass());
            }
            return Output;
        }
        VPLTableScanner.close();
        return Output;

    }

    /**
     * gets the message contained in the substitution plan for a certain day and cleans it
     * @param Table the code fragment that may or may not contain the message for the day
     * @return if there is a message, it is returned, otherwise it returns
     */
    private contentAndError tryToGetMessage(String Table)
    {
        contentAndError Output = new contentAndError();
        Scanner messageScanner = new Scanner(Table);
        Output.setContent("MessageNotFound");
        //skips a line
        messageScanner.useDelimiter(System.getProperty("line.separator"));
        //checks if the line is the start of a message
        if(messageScanner.hasNext())
        {
            String validator = messageScanner.next();
            if(validator.contains("Nachrichten zum Tag"))
            {

                messageScanner.useDelimiter("</table>");
                //clears all HTML code from the message
                if(messageScanner.hasNext())
                {
                    String message =messageScanner.next();
                    message=message.replaceAll("<tr>"," ");
                    message=message.replaceAll("<td>"," ");
                    message=message.replaceAll("&nbsp;"," ");
                    message=message.replaceAll("</td>"," ");
                    message=message.replaceAll("</tr>"," ");
                    message=message.replaceAll("<b>"," ");
                    message=message.replaceAll("</b>"," ");
                    message=message.replaceAll("<td colspan=\"2\">","");
                    message=message.replaceAll("<br>"," ");



                    Output.setContent(message);
                }



            }
        }
        messageScanner.close();
        return Output;
    }
    private boolean isNetworkAvailable(ConnectionTaskInput input)
    {
        ConnectivityManager manager =(ConnectivityManager) input.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        try{NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();}
        catch(NullPointerException e)
        {
            Main.log("Error","NullPointerException occured while accessing network state"+e,this.getClass());
        }
        return false;
    }
    protected void onProgressUpdate(Void nothing) {

    }
    private String VPLWeekToString(VPLWeek week, ConnectionTaskInput input)
    {

            String VPLWeekToWrite = "";
            //writes the weekType
            VPLWeekToWrite=VPLWeekToWrite.concat(week.getWeekType());
            VPLWeekToWrite=VPLWeekToWrite.concat("<delimiter>");
            VPLWeekToWrite=VPLWeekToWrite.concat(week.getValidity());
            VPLWeekToWrite=VPLWeekToWrite.concat("<delimiter>");
            //writes the days
            for (VPLDay day : week.getDays()) {
                VPLWeekToWrite=VPLWeekToWrite.concat(day.getDate());
                VPLWeekToWrite=VPLWeekToWrite.concat("<delimiter>");
                VPLWeekToWrite=VPLWeekToWrite.concat(day.getMessage());
                VPLWeekToWrite=VPLWeekToWrite.concat("<delimiter>");
                VPLWeekToWrite=VPLWeekToWrite.concat(day.getAvailable().toString());
                VPLWeekToWrite=VPLWeekToWrite.concat("<delimiter>");
                if (day.getAvailable()) {
                    for (VPLLine line : day.getLines()) {
                        if (!line.getGroup().equals("")) {
                            VPLWeekToWrite=VPLWeekToWrite.concat(line.getGroup());
                        } else {
                            VPLWeekToWrite=VPLWeekToWrite.concat("Empty");
                        }
                        VPLWeekToWrite=VPLWeekToWrite.concat("<lineDelimiter>");
                        VPLWeekToWrite=VPLWeekToWrite.concat(line.getDate());
                        VPLWeekToWrite=VPLWeekToWrite.concat("<lineDelimiter>");
                        VPLWeekToWrite=VPLWeekToWrite.concat(line.getLessons());
                        VPLWeekToWrite= VPLWeekToWrite.concat("<lineDelimiter>");
                        VPLWeekToWrite=VPLWeekToWrite.concat(line.getSubstitute());
                        VPLWeekToWrite=VPLWeekToWrite.concat("<lineDelimiter>");
                        VPLWeekToWrite=VPLWeekToWrite.concat(line.getReplacementSubject());
                        VPLWeekToWrite=VPLWeekToWrite.concat("<lineDelimiter>");
                        VPLWeekToWrite=VPLWeekToWrite.concat(line.getRoom());
                        VPLWeekToWrite=VPLWeekToWrite.concat("<lineDelimiter>");
                        VPLWeekToWrite=VPLWeekToWrite.concat(line.getType());
                        VPLWeekToWrite=VPLWeekToWrite.concat("<lineDelimiter>");
                        VPLWeekToWrite=VPLWeekToWrite.concat(line.getSubject());
                        VPLWeekToWrite=VPLWeekToWrite.concat("<lineDelimiter>");
                        VPLWeekToWrite=VPLWeekToWrite.concat(line.getComment());
                        VPLWeekToWrite=VPLWeekToWrite.concat("<lineDelimiter>");
                        VPLWeekToWrite=VPLWeekToWrite.concat(line.getN_Druck());
                        VPLWeekToWrite=VPLWeekToWrite.concat("<lineDelimiter>");
                        VPLWeekToWrite=VPLWeekToWrite.concat("<lineEnd>");

                    }
                }
                VPLWeekToWrite=VPLWeekToWrite.concat("<dayEnd>");
            }
            VPLWeekToWrite=VPLWeekToWrite.concat("<newWeek>");
            return VPLWeekToWrite;



    }

    /**
     * writes VPLWeek objects to a file and triggers the showVPLEvent Event once its done
     * @param result the two VPLWeek objects to write to the save File, the path of the saveFile to write in and the showVPLEventInitiator object containin the listener
     */
    protected void onPostExecute(final ConnectionTaskInput result)
    {

            final Activity instanceOfActivity = new Activity();
            instanceOfActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if(result.getWeekArray().get(0).isSuccessful()||result.getWeekArray().get(1).isSuccessful())
                    {
                        try {
                        Calendar now = Calendar.getInstance();
                        //saves Username and Password
                            if(Main.saveCredentials) {
                                Cryptography.encryptCredentials(Main.websiteAddress+"||"+Main.username + "||" + Main.password, result.getContext());
                            }
                        //clears VPLSaveFile
                        PrintWriter pw1 = new PrintWriter(result.getSaveFile());
                        pw1.close();


                        //prints the update date
                            String VPLToWrite="";
                            VPLToWrite=VPLToWrite.concat(now.get(Calendar.DAY_OF_MONTH)+"."+Integer.toString(now.get(Calendar.MONTH)+1)+"."+now.get(Calendar.YEAR));
                            VPLToWrite=VPLToWrite.concat("<DateDelimiter>");
                            VPLToWrite=VPLToWrite.concat(now.get(Calendar.HOUR_OF_DAY)+":"+now.get(Calendar.MINUTE));
                            VPLToWrite=VPLToWrite.concat("<DateDelimiter>");

                        for(VPLWeek week : result.getWeekArray())
                        {
                            if(week.isSuccessful())
                            {
                                VPLToWrite=VPLToWrite.concat(VPLWeekToString(week,result));
                            }
                        }
                        //encrypts the VPL
                            VPLToWrite=Cryptography.encrypt(Main.SF_INIT_VECTOR,VPLToWrite,result.getContext(),Main.SF_KEY,Main.SF_ENCRYPT_SUCCESS_KEY,Main.SF_KEY_23_AES,Main.SF_KEY_23_RSA);
                        //writes to VPLSaveFile
                            FileWriter FW1 = new FileWriter(result.getSaveFile());
                            FW1.write(VPLToWrite);
                            FW1.flush();
                            FW1.close();

                    } catch (FileNotFoundException e) {
                            if(result.getMode().equals("online")) {
                                Main.log("Error", "Could not find saveFile: FileNotFoundException" + e, this.getClass());
                            }
                    } catch (IOException e) {
                            if(result.getMode().equals("online")) {
                                Main.log("Error", "error occured while interacting with a save file: IOException" + e, this.getClass());
                            }
                    }


                }
                //if an error occured it notifies the user of the occurance
                else
                    {
                        if(result.getMode().equals("online")) {
                            Main.log("Info", "triggered error display", this.getClass());
                            result.getErrorInitiator().trigger(-1,result.getContext());

                        }
                    }
                    //triggers the display of the substitution plan only if it is not simply an update
                        result.getInitiator().trigger();
                }

            });




    }

}
