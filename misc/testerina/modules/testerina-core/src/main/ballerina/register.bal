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

final TestRegistry testRegistry = new ();
final TestRegistry beforeSuiteRegistry = new ();
final TestRegistry afterSuiteRegistry = new ();
final TestRegistry beforeEachRegistry = new ();
final TestRegistry afterEachRegistry = new ();

final GroupRegistry beforeGroupsRegistry = new ();
final GroupRegistry afterGroupsRegistry = new ();
final GroupStatusRegistry groupStatusRegistry = new ();

type TestFunction record {|
    string name;
    function executableFunction;
    boolean enabled = true;
    DataProviderReturnType? params = ();
    function? before = ();
    function? after = ();
    boolean alwaysRun = false;
    string[] groups = [];
    boolean skip = false;
    error? diagnostics = ();
    function[] dependsOn = [];
    int dependsOnCount = 0;
    TestFunction[] dependents = [];
    boolean visited = false;
|};

class TestRegistry {
    private final TestFunction[] rootRegistry = [];
    private final TestFunction[] dependentRegistry = [];

    function addFunction(*TestFunction functionDetails) {
        if functionDetails.dependsOn == [] {
            self.rootRegistry.push(functionDetails);
        } else {
            self.dependentRegistry.push(functionDetails);
        }
    }

    function getTestFunction(function f) returns TestFunction|error {
        TestFunction[] filter;
        filter = self.rootRegistry.filter(testFunction => f === testFunction.executableFunction);
        if filter.length() == 0 {
            filter = self.dependentRegistry.filter(testFunction => f === testFunction.executableFunction);
            if filter.length() == 0 {
                //TODO: need to obtain the function name form the variable
                return error(string `The dependent test function is either disabled or not included.`);
            }
        }
        return filter.pop();
    }

    function getFunctions() returns TestFunction[] => self.rootRegistry.sort(key = testFunctionsSort);

    function getDependentFunctions() returns TestFunction[] => self.dependentRegistry;

}

class GroupRegistry {
    private final map<TestFunction[]> registry = {};

    function addFunction(string g, *TestFunction testFunction) {
        if self.registry.hasKey(g) {
            self.registry.get(g).push(testFunction);
        } else {
            self.registry[g] = [testFunction];
        }
    }

    function getFunctions(string g) returns TestFunction[]? {
        if self.registry.hasKey(g) {
            return self.registry.get(g);
        }
        return;
    }
}

class GroupStatusRegistry {
    private final map<int> totalTests = {};
    private final map<int> executedTests = {};
    private final map<boolean> skip = {};

    function firstExecuted(string g) returns boolean => self.executedTests.get(g) > 0;

    function lastExecuted(string g) returns boolean => self.executedTests.get(g) == self.totalTests.get(g);

    function incrementTotalTest(string g) {
        self.skip[g] = false;
        if self.totalTests.hasKey(g) {
            self.totalTests[g] = self.totalTests.get(g) + 1;
        } else {
            self.totalTests[g] = 1;
            self.executedTests[g] = 0;
        }
    }

    function incrementExecutedTest(string g) {
        if self.executedTests.hasKey(g) {
            self.executedTests[g] = self.executedTests.get(g) + 1;
        } else {
            self.executedTests[g] = 1;
        }
    }

    function setSkipAfterGroup(string g) {
        self.skip[g] = true;
    }

    function getSkipAfterGroup(string g) returns boolean => self.skip.get(g);

    function getGroupsList() returns string[] => self.totalTests.keys();
}

isolated function testFunctionsSort(TestFunction testFunction) returns string => testFunction.name;

function isDataDrivenTest(TestFunction testFunction) returns boolean =>
    testFunction.params is map<AnyOrError[]> || testFunction.params is AnyOrError[][];
