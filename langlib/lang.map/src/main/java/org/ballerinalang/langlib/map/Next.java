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

package org.ballerinalang.langlib.map;

import org.ballerinalang.jvm.api.BStringUtils;
import org.ballerinalang.jvm.api.BValueCreator;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.IteratorValue;
import org.ballerinalang.jvm.values.MapValueImpl;
import org.ballerinalang.jvm.values.ObjectValue;

/**
 * Native implementation of lang.map.MapIterator:next().
 *
 * @since 1.0
 */
//@BallerinaFunction(
//        orgName = "ballerina", packageName = "lang.map", functionName = "next",
//        receiver = @Receiver(type = TypeKind.OBJECT, structType = "MapIterator",
//        structPackage = "ballerina/lang.map"),
//        returnType = {@ReturnType(type = TypeKind.RECORD)},
//        isPublic = true
//)
public class Next {
    //TODO: refactor hard coded values
    public static Object next(ObjectValue m) {
        IteratorValue mapIterator = (IteratorValue) m.getNativeData("&iterator&");
        MapValueImpl mapValue = (MapValueImpl) m.get(BStringUtils.fromString("m"));
        if (mapIterator == null) {
            mapIterator = mapValue.getIterator();
            m.addNativeData("&iterator&", mapIterator);
        }

        if (mapIterator.hasNext()) {
            ArrayValue keyValueTuple = (ArrayValue) mapIterator.next();
            return BValueCreator.createRecordValue(new MapValueImpl<>(mapValue.getIteratorNextReturnType()),
                                                   keyValueTuple.get(1));
        }

        return null;
    }
}
