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

import org.ballerinalang.jvm.XMLFactory;
import org.ballerinalang.jvm.api.values.BString;
import org.ballerinalang.jvm.values.XMLValue;

/**
 * XML Processing Instruction constructor function.
 *
 * @since 2.0.0
 */
//@BallerinaFunction(
//        orgName = "ballerina", packageName = "lang.__internal", version = "0.1.0",
//        functionName = "processingInstructionCtor",
//        args = {
//                @Argument(name = "target", type = TypeKind.STRING),
//                @Argument(name = "content", type = TypeKind.STRING)
//        },
//        returnType = {@ReturnType(type = TypeKind.XML)}
//)
public class ProcessingInstructionCtor {

    public static XMLValue processingInstructionCtor(BString target, BString content) {
        return XMLFactory.createXMLProcessingInstruction(target, content);
    }
}
