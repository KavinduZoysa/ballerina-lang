/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.langlib.string;

import org.ballerinalang.jvm.api.BStringUtils;
import org.ballerinalang.jvm.api.BValueCreator;
import org.ballerinalang.jvm.api.values.BString;
import org.ballerinalang.jvm.types.BTypes;
import org.ballerinalang.jvm.values.MapValueImpl;
import org.ballerinalang.jvm.values.ObjectValue;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;


/**
 * Native implementation of lang.string.StringIterator:next().
 *
 * @since 1.0
 */
public class Next {

    //TODO: refactor hard coded values
    public static Object next(ObjectValue m) {
        StringCharacterIterator stringCharacterIterator = (StringCharacterIterator) m.getNativeData("&iterator&");
        if (stringCharacterIterator == null) {
            String s = ((BString) m.get(BStringUtils.fromString("m"))).getValue();
            stringCharacterIterator = new StringCharacterIterator(s);
            m.addNativeData("&iterator&", stringCharacterIterator);
        }

        if (stringCharacterIterator.current() != CharacterIterator.DONE) {
            char character = stringCharacterIterator.current();
            stringCharacterIterator.next();
            Object charAsStr = BStringUtils.fromString(String.valueOf(character));
            return BValueCreator.createRecordValue(new MapValueImpl<>(BTypes.stringItrNextReturnType), charAsStr);
        }

        return null;
    }
}
