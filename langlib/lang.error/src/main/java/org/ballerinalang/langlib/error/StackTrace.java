/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 **/
package org.ballerinalang.langlib.error;

import org.ballerinalang.jvm.api.BErrorCreator;
import org.ballerinalang.jvm.api.BStringUtils;
import org.ballerinalang.jvm.api.BValueCreator;
import org.ballerinalang.jvm.api.values.BMap;
import org.ballerinalang.jvm.api.values.BString;
import org.ballerinalang.jvm.scheduling.Strand;
import org.ballerinalang.jvm.types.AttachedFunction;
import org.ballerinalang.jvm.types.BArrayType;
import org.ballerinalang.jvm.types.BField;
import org.ballerinalang.jvm.types.BObjectType;
import org.ballerinalang.jvm.types.BPackage;
import org.ballerinalang.jvm.types.BType;
import org.ballerinalang.jvm.types.BTypes;
import org.ballerinalang.jvm.values.AbstractObjectValue;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.ArrayValueImpl;
import org.ballerinalang.jvm.values.ErrorValue;
import org.ballerinalang.jvm.values.FutureValue;
import org.ballerinalang.jvm.values.ObjectValue;

import java.util.Collections;

import static org.ballerinalang.jvm.util.BLangConstants.BALLERINA_LANG_ERROR_PKG_ID;
import static org.ballerinalang.jvm.values.ErrorValue.CALL_STACK_ELEMENT;

/**
 * Get the stackTrace of an error value.
 *
 * @since 0.990.4
 */
public class StackTrace {

    public static ObjectValue stackTrace(ErrorValue value) {

        BObjectType callStackObjType = new BObjectType("CallStack", new BPackage("ballerina", "lang.error", null), 0);
        callStackObjType.setAttachedFunctions(new AttachedFunction[]{});
        callStackObjType.setFields(
                Collections.singletonMap("callStack", new BField(new BArrayType(BTypes.typeAny), null, 0)));

        CallStack callStack = new CallStack(callStackObjType);
        callStack.callStack = getCallStackArray(value.getStackTrace());
        callStack.callStack.freezeDirect();
        return callStack;
    }

    private static ArrayValue getCallStackArray(StackTraceElement[] stackTrace) {
        BType recordType = BValueCreator
                .createRecordValue(BALLERINA_LANG_ERROR_PKG_ID, CALL_STACK_ELEMENT).getType();
        Object[] array = new Object[stackTrace.length];
        for (int i = 0; i < stackTrace.length; i++) {
            array[i] = getStackFrame(stackTrace[i]);
        }
        return new ArrayValueImpl(array, new BArrayType(recordType));
    }

    static BMap<BString, Object> getStackFrame(StackTraceElement stackTraceElement) {
        Object[] values = new Object[4];
        values[0] = stackTraceElement.getMethodName();
        values[1] = stackTraceElement.getClassName();
        values[2] = stackTraceElement.getFileName();
        values[3] = stackTraceElement.getLineNumber();
        return BValueCreator.createRecordValue(
                BValueCreator.createRecordValue(BALLERINA_LANG_ERROR_PKG_ID, CALL_STACK_ELEMENT), values);
    }

    /**
     * Represent Ballerina call stack when the error is constructed.
     */
    public static class CallStack extends AbstractObjectValue {
        ArrayValue callStack;

        public CallStack(BObjectType type) {
            super(type);
        }

        @Override
        public Object call(Strand strand, String funcName, Object... args) {
            throw BErrorCreator.createError(BStringUtils.fromString("No such field or method: " + funcName));
        }

        @Override
        public FutureValue start(Strand strand, String funcName, Object... args) {
            throw BErrorCreator.createError(BStringUtils.fromString("No such field or method: " + funcName));
        }

        @Override
        public Object get(BString fieldName) {
            if (fieldName.getValue().equals("callStack")) {
                return callStack;
            }
            throw BErrorCreator.createError(BStringUtils.fromString("No such field or method: callStack"));
        }

        @Override
        public void set(BString fieldName, Object value) {
            throw BErrorCreator.createError(BStringUtils.fromString("No such field or method: callStack"));
        }
    }
}
