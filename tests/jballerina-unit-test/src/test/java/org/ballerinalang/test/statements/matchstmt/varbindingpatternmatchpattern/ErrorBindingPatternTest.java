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
package org.ballerinalang.test.statements.matchstmt.varbindingpatternmatchpattern;

import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.BRunUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test cases to verify the behaviour of the error-binding-pattern.
 *
 * @since 2.0.0
 */
public class ErrorBindingPatternTest {
    private CompileResult result, restPatternResult;

    @BeforeClass
    public void setup() {
        result = BCompileUtil.compile("test-src/statements/matchstmt/varbindingpatternmatchpattern/error-binding" +
                "-pattern.bal");
        restPatternResult = BCompileUtil.compile("test-src/statements/matchstmt/varbindingpatternmatchpattern/error" +
                "-binding-pattern-with-rest-binding-pattern.bal");
    }

    @Test
    public void testErrorBindingPattern1() {
        BRunUtil.invoke(result, "testErrorBindingPattern1");
    }

    @Test
    public void testErrorBindingPattern2() {
        BRunUtil.invoke(result, "testErrorBindingPattern2");
    }

    @Test
    public void testErrorBindingPattern3() {
        BRunUtil.invoke(result, "testErrorBindingPattern3");
    }

    @Test
    public void testErrorBindingPattern4() {
        BRunUtil.invoke(result, "testErrorBindingPattern4");
    }

    @Test
    public void testErrorBindingPattern5() {
        BRunUtil.invoke(result, "testErrorBindingPattern5");
    }

    @Test
    public void testErrorBindingPattern6() {
        BRunUtil.invoke(result, "testErrorBindingPattern6");
    }

    @Test
    public void testErrorBindingPattern7() {
        BRunUtil.invoke(result, "testErrorBindingPattern7");
    }

    @Test
    public void testErrorBindingPattern8() {
        BRunUtil.invoke(result, "testErrorBindingPattern8");
    }

    @Test
    public void testErrorBindingPatternWithRestBindingPattern1() {
        BRunUtil.invoke(restPatternResult, "testErrorBindingPattern1");
    }

    @Test
    public void testErrorBindingPatternWithRestBindingPattern2() {
        BRunUtil.invoke(restPatternResult, "testErrorBindingPattern2");
    }

    @Test
    public void testErrorBindingPatternWithRestBindingPattern3() {
        BRunUtil.invoke(restPatternResult, "testErrorBindingPattern3");
    }
}
