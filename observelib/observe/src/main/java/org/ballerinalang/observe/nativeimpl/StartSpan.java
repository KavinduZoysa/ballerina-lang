/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 */

package org.ballerinalang.observe.nativeimpl;

import org.ballerinalang.jvm.api.BErrorCreator;
import org.ballerinalang.jvm.api.BStringUtils;
import org.ballerinalang.jvm.api.values.BString;
import org.ballerinalang.jvm.observability.ObservabilityConstants;
import org.ballerinalang.jvm.scheduling.Scheduler;
import org.ballerinalang.jvm.values.MapValue;

/**
 * This function which implements the startSpan method for observe.
 */

public class StartSpan {
    public static Object startSpan(BString spanName, Object tags, long parentSpanId) {
        if (parentSpanId < -1) {
            return BErrorCreator.createError(
                    BStringUtils.fromString(("The given parent span ID " + parentSpanId + " " + "is invalid.")));
        } else {
            long spanId = OpenTracerBallerinaWrapper.getInstance().startSpan(
                    (String) Scheduler.getStrand().getProperty(ObservabilityConstants.SERVICE_NAME),
                    spanName.getValue(),
                    Utils.toStringMap((MapValue<BString, ?>) tags), parentSpanId, Scheduler.getStrand());
            if (spanId == -1) {
                return BErrorCreator.createError(BStringUtils.fromString((
                        "No parent span for ID " + parentSpanId + " found. Please recheck the parent span Id")));
            }

            return spanId;
        }
    }
}
