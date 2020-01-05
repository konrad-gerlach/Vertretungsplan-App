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

/**
 * Created by User on 11.01.2018.
 */

/**a class that holds the names of teachers and their shorthands ( abbreviations)
 *
 */
public class Teacher_Abbreviations_Storage {
    private String name ="";

    public String getShortHand() {
        return shortHand;
    }

    public void setShortHand(String shortHand) {
        this.shortHand = shortHand;
    }

    private String shortHand = "";

    public Teacher_Abbreviations_Storage(String name, String shortHand) {
        this.name = name;
        this.shortHand = shortHand;
    }
    public Teacher_Abbreviations_Storage()
    {}
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }








}
