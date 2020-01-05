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

package org.konrad_gerlach.vertretungsplanapp.security;

public class Validator {
    public static final char[] validCharset =
            {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'
                    ,'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
                    '0','1','2','3','4','5','6','7','8','9'
                    ,'.',',',';',':','?','!','_','-','@','/',' '};
    //checks if all chars in string are represented in charset
    public static boolean checkString(String toCheck)
    {
        if(toCheck.equals(""))
            return false;
        boolean checksOut =true;
        for(char c: toCheck.toCharArray())
        {
            try{
                findIndex(c);
            }
            catch(IllegalArgumentException e)
            {
                checksOut=false;
            }
        }
        return checksOut;
    }
    //finds the index of the char toFind within the validCharset
    private static int findIndex(char toFind)
            throws IllegalArgumentException
    {
        int index =-1;
        for(int j = 0; j< validCharset.length; j++)
        {
            if(toFind== validCharset[j])
            {
                index=j;
            }
        }
        if(index==-1) {
            throw new IllegalArgumentException("Character not supported by charset");
        }
        return index;
    }
}
