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

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.*;
import android.view.*;

public class EULA extends AppCompatActivity {
    Main mainInstance = new Main();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(mainInstance.ThemeNametoThemeInt(mainInstance.theme,"ID"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eula_and_privacy_policy);
        //ensures line separators work
        ((TextView) findViewById(R.id.eulaview)).setText(this.getResources().getString(R.string.EULA).replaceAll("\\n", System.getProperty("line.separator")));
        ((TextView) findViewById(R.id.privacypolicyview)).setText(((TextView) findViewById(R.id.privacypolicyview)).getText().toString().replaceAll("\\n",System.getProperty("line.separator")));
        final Button eulaAcceptButton = (Button) findViewById(R.id.EULA_Accept_Button);
        //ensures links work
        CheckBox eulaAcceptedCheckBox1 = findViewById(R.id.eulaCheckBox);
        eulaAcceptedCheckBox1.setMovementMethod(LinkMovementMethod.getInstance());
        CheckBox privacyPolicyAcceptedCheckBox1 = findViewById(R.id.privacyPolicyCheckBox);
        privacyPolicyAcceptedCheckBox1.setMovementMethod(LinkMovementMethod.getInstance());
        CheckBox ageCheckBox =findViewById(R.id.ageCheckBox);
        if(!this.getIntent().getBooleanExtra("Agreeable",true))
        {
            eulaAcceptButton.setVisibility(View.GONE);
            eulaAcceptedCheckBox1.setVisibility(View.GONE);
            privacyPolicyAcceptedCheckBox1.setVisibility(View.GONE);
            ageCheckBox.setVisibility(View.GONE);
        }
        else {
            eulaAcceptButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox eulaAcceptedCheckBox = findViewById(R.id.eulaCheckBox);
                    CheckBox privacyPolicyAcceptedCheckBox = findViewById(R.id.privacyPolicyCheckBox);
                    CheckBox ageCheckBox1=findViewById(R.id.ageCheckBox);
                    Button eulaAcceptButton1 = findViewById(R.id.EULA_Accept_Button);
                    if(eulaAcceptedCheckBox.isChecked()&&privacyPolicyAcceptedCheckBox.isChecked()&&ageCheckBox1.isChecked()) {
                        eulaAcceptedCheckBox.setEnabled(false);
                        privacyPolicyAcceptedCheckBox.setEnabled(false);
                        eulaAcceptButton1.setEnabled(false);
                        ageCheckBox1.setEnabled(false);

                        Main.eulaAcceptedInitiator.trigger();
                        finish();
                    }else
                    {
                        Context context = v.getContext();
                        CharSequence text = v.getContext().getText(R.string.EULAorPrivacyPolicyToast);
                        int duration = Toast.LENGTH_LONG;
                        Toast toast = Toast.makeText(context,text,duration);
                        toast.show();
                    }
                }
            });
        }



    }
}
