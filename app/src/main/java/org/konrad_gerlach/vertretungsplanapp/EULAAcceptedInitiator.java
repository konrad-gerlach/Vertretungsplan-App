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


import java.util.ArrayList;

/**
 * Created by User on 26.11.2017.
 */

public class EULAAcceptedInitiator {
    ArrayList<EULAAcceptedListener> listeners = new ArrayList<EULAAcceptedListener>();

    public void addEventListener(EULAAcceptedListener listenerToAdd)
    {
        listeners.add(listenerToAdd);
    }
    public void trigger ()
    {
        for(EULAAcceptedListener l : listeners)
        {
            l.onEULAAccepted();
        }
    }
}
