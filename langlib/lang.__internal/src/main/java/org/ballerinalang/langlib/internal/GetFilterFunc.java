/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.langlib.internal;

import org.ballerinalang.jvm.types.BFunctionType;
import org.ballerinalang.jvm.types.BType;
import org.ballerinalang.jvm.types.BTypes;
import org.ballerinalang.jvm.types.BUnionType;
import org.ballerinalang.jvm.values.FPValue;

/**
 * Native implementation of lang.internal:getFilterFunc(func).
 *
 * @since 1.2.0
 */
//@BallerinaFunction(
//        orgName = "ballerina", packageName = "lang.__internal", functionName = "getFilterFunc",
//        args = {@Argument(name = "func", type = TypeKind.ANY)},
//        returnType = {@ReturnType(type = TypeKind.FUNCTION)}
//)
public class GetFilterFunc {

    public static FPValue getFilterFunc(Object obj) {
        FPValue fpValue = (FPValue) obj;
        BFunctionType functionType = (BFunctionType) fpValue.getType();
        functionType.paramTypes[0] = new BUnionType(new BType[]{BTypes.typeAny, BTypes.typeError}, 0);
        return fpValue;
    }
}
