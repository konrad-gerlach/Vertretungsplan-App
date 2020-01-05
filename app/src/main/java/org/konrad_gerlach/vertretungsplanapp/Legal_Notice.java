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

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class Legal_Notice extends AppCompatActivity {
    Main mainInstance = new Main();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(mainInstance.ThemeNametoThemeInt(mainInstance.theme,"ID"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal__notice);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //ensures links work
        TextView legalQuote =findViewById(R.id.legalQuote);
        legalQuote.setMovementMethod(LinkMovementMethod.getInstance());
        TextView notice =findViewById(R.id.NOTICE);
        notice.setMovementMethod(LinkMovementMethod.getInstance());
        TextView about =findViewById(R.id.About);
        about.setMovementMethod(LinkMovementMethod.getInstance());
    }

}
