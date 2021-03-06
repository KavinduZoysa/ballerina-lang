// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/file;
import ballerina/http;
import ballerina/io;

public function main() {
    http:CsvPersistentCookieHandler myPersistentStore = new("./cookie-test-data/client-6.csv");
    http:Client cookieClientEndpoint = new ("http://localhost:9253", {
            retryConfig: {
                intervalInMillis: 3000,
                count: 3,
                backOffFactor: 2.0,
                maxWaitIntervalInMillis: 20000
            },
            circuitBreaker: {
                rollingWindow: {
                    timeWindowInMillis: 10000,
                    bucketSizeInMillis: 2000,
                    requestVolumeThreshold: 0
                },
                failureThreshold: 0.2,
                resetTimeInMillis: 10000,
                statusCodes: [400, 404, 500]
            },
            cookieConfig: {
                enabled: true,
                persistentCookieHandler: myPersistentStore
            }
        });
    http:Request req = new;
    // Server sends cookies in the response for the first request.
    var response = cookieClientEndpoint->get("/cookie/cookieBackend_1", req);
    // Second request is with a cookie header and server sends more cookies in the response.
    response = cookieClientEndpoint->get("/cookie/cookieBackend_1", req);
     // Third request is with the cookie header including all relevant cookies.
    response = cookieClientEndpoint->get("/cookie/cookieBackend_1", req);
    if (response is http:Response) {
        var payload = response.getTextPayload();
        if (payload is string) {
            io:print(payload);
        }
    }
    error? removeResults = file:remove("./cookie-test-data", true);
}
