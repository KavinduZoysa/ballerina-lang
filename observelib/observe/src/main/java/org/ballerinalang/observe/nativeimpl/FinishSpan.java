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
import org.ballerinalang.jvm.scheduling.Scheduler;

/**
 * This function which implements the finishSpan method for observe.
 */

public class FinishSpan {
    private static final OpenTracerBallerinaWrapper otWrapperInstance = OpenTracerBallerinaWrapper.getInstance();

    public static Object finishSpan(long spanId) {
        boolean isFinished = OpenTracerBallerinaWrapper.getInstance().finishSpan(Scheduler.getStrand(), spanId);

        if (isFinished) {
            return null;
        }

        return BErrorCreator.createError(BStringUtils.fromString(("Can not finish span with id " + spanId + ". Span " +
                "already finished")));
    }
}
