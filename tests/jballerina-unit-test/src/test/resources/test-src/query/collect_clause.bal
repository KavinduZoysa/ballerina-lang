// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

function testInvocationAsCollectExpression() {
    int x1 = from var {x} in [{"x":2, "y":3}, {"x":4, "y":5}]
                    collect int:sum(x);
    assertEquality(x1, 6);
    int x2 = from var {x, y} in [{"x":2, "y":3}, {"x":4, "y":5}]
                    collect int:sum(y); // error
    assertEquality(x2, 8);
    string x3 = from var {x} in [{"x":"2", "y":"3"}, {"x":"4", "y":"5"}]
                    collect ",".'join(x);
    assertEquality(x3, "2,4");
    // int x4 = from var {salary, bonus} in [{salary: 2, bonus: 1}, {salary: 4, bonus: 2}]
    //             collect int:sum(salary + bonus);
    // assertEquality(x4, 9);
}

function testUnqualifiedInvocationAsCollectExpression() {
    int x1 = from var {salary, bonus} in [{salary: 2, bonus: 1}, {salary: 4, bonus: 2}]
                collect sum(salary);
    assertEquality(x1, 6);
}

function testListCtrAsCollectExpression() {
    record {| int[] x; |} rec = from var {x, y} in [{"x":2, "y":3}, {"x":4, "y":5}]
                                    collect { x: [x] };
    assertEquality(rec.toString(), "{\"x\":[2,4]}");
}

function assertEquality(any|error expected, any|error actual) {
    if expected is anydata && actual is anydata && expected == actual {
        return;
    }

    if expected === actual {
        return;
    }

    string expectedValAsString = expected is error ? expected.toString() : expected.toString();
    string actualValAsString = actual is error ? actual.toString() : actual.toString();
    panic error(string `expected '${expectedValAsString}', found '${actualValAsString}'`);
}