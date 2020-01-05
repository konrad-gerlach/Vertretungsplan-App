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

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.konrad_gerlach.vertretungsplanapp.security.Cryptography;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Teacher_Abbreviation_Editor extends AppCompatActivity {
    Main mainInstance = new Main();
    VPLDisplayer vplDisplayerInstance = new VPLDisplayer();
    public static Teacher_Abbreviation_Editor openInstance;
    public static ArrayList<ConstraintLayout> abbrTableRows = new ArrayList<ConstraintLayout>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(mainInstance.ThemeNametoThemeInt(mainInstance.theme,"ID"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher__abbreviation__editor);
        openInstance=this;
        TableLayout table =(TableLayout) findViewById(R.id.AbbrTable);
        abbrTableRows.clear();
        table.removeViews(1,table.getChildCount()-1);
        for(Teacher_Abbreviations_Storage abbr :vplDisplayerInstance.abbreviations)
        {
            addAbbr(abbr.getName(),abbr.getShortHand());
        }
        FloatingActionButton actionButton =(FloatingActionButton) findViewById(R.id.addAbbr);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Teacher_Abbreviation_Editor instanceOf = new Teacher_Abbreviation_Editor();
                instanceOf.openInstance.addAbbr("","");
            }
        });
        Button saveButton =(Button) findViewById(R.id.save_Button_Abbr);
        saveButton.setOnClickListener(new View.OnClickListener() {
            /**
             * overwrites the abbreviations ArrayList in the VPLDisplayer to contain the newest names abbreviations and saves the names and their abbreviations in the saveFile
             * @param v
             */
            @Override
            public void onClick(View v) {
                Main main = new Main();
                Main.log("Info","Saving the abbreviations",Teacher_Abbreviation_Editor.class);
                ArrayList<Teacher_Abbreviations_Storage> newList = new ArrayList<>();
                try {
                    PrintWriter writer = new PrintWriter(Main.abbreviationsFile);
                    writer.print("");
                    writer.close();

                    String TeachersAbbrsToWrite="";

                    for (ConstraintLayout row : abbrTableRows){
                        Teacher_Abbreviations_Storage abbreviation = new Teacher_Abbreviations_Storage(((EditText) row.getChildAt(0)).getText().toString(), ((EditText) row.getChildAt(1)).getText().toString());
                        newList.add(abbreviation);
                        TeachersAbbrsToWrite=TeachersAbbrsToWrite.concat(abbreviation.getName());
                        TeachersAbbrsToWrite=TeachersAbbrsToWrite.concat("<delimiter>");
                        TeachersAbbrsToWrite=TeachersAbbrsToWrite.concat(abbreviation.getShortHand());
                        TeachersAbbrsToWrite=TeachersAbbrsToWrite.concat("<delimiter>");
                    }
                    TeachersAbbrsToWrite= Cryptography.encrypt(Main.TA_INIT_VECTOR,TeachersAbbrsToWrite,v.getContext(),Main.TA_KEY,Main.TA_ENCRYPT_SUCCESS_KEY,Main.TA_KEY_23_AES,Main.TA_KEY_23_RSA);
                    FileWriter FW1 = new FileWriter(Main.abbreviationsFile);
                    FW1.write(TeachersAbbrsToWrite);
                    FW1.flush();
                    FW1.close();
                    VPLDisplayer.abbreviations.clear();
                    VPLDisplayer.abbreviations=newList;
                }
                catch (IOException e)
                {
                    Main.log("Error","an error occured while interacting with the abbreviations SaveFile",Teacher_Abbreviation_Editor.class);
                }

                finish();
            }
        });

    }
    public void readdTableViews()
    {
        TableLayout table =(TableLayout) findViewById(R.id.AbbrTable);
        table.removeViews(1,table.getChildCount()-1);
        ArrayList<ConstraintLayout> oldList=new ArrayList<ConstraintLayout>();
        oldList.addAll(abbrTableRows);
        abbrTableRows.clear();
        for(ConstraintLayout row:oldList)
        {
            addAbbr(((EditText) row.getChildAt(0)).getText().toString(),((EditText) row.getChildAt(1)).getText().toString());
        }
    }
    /**
     * adds a new table row containing an edittext for the name of a teacher and one for their abbreviation
     * @param name  the teachers name to add to the name edittext as a default
     * @param abbr  the teachers abbreviation to add to the abbreviation edittext  as a default
     */
    public void addAbbr(String name, String abbr)
    {
        TableLayout table =(TableLayout) findViewById(R.id.AbbrTable);
        ConstraintLayout rowToAdd = new ConstraintLayout(this);
        ConstraintSet constraintSet=new ConstraintSet();
        constraintSet.clone((ConstraintLayout)findViewById(R.id.master));
        EditText nameEdit = new EditText(this);
        EditText abbrEdit = new EditText(this);
        ImageView deleteButton = new ImageView(this);
        nameEdit.setText(name);
        nameEdit.setId(R.id.teachernamelabel);
        nameEdit.setTextSize(20);
        nameEdit.setLayoutParams(new ConstraintLayout.LayoutParams(new ViewGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)));
        abbrEdit.setText(abbr);
        abbrEdit.setTextSize(20);
        abbrEdit.setLayoutParams(new ConstraintLayout.LayoutParams(new ViewGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)));
        abbrEdit.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        abbrEdit.setId(R.id.teacherabbrlabel);
        deleteButton.setTag(abbrTableRows.size());
        deleteButton.setImageResource(R.drawable.ic_delete_24px);
        deleteButton.setId(R.id.imageholder);
        //deletes the added entry from the table
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Teacher_Abbreviation_Editor instance = new Teacher_Abbreviation_Editor();
                instance.openInstance.abbrTableRows.remove((int)v.getTag());
                instance.openInstance.readdTableViews();
            }
        });

        rowToAdd.addView(nameEdit);
        rowToAdd.addView(abbrEdit);
        rowToAdd.addView(deleteButton);
        constraintSet.applyTo(rowToAdd);
        //RippleDrawable ripple = new RippleDrawable("@color/ColorPrimaryLight",rowToAdd,null);
        abbrTableRows.add(rowToAdd);
        table.addView(rowToAdd);
    }
}
