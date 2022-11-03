/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.test.query;

import org.ballerinalang.test.BCompileUtil;
import org.ballerinalang.test.CompileResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.ballerinalang.test.BAssertUtil.validateError;

/**
 * This contains methods to test collect clause in query expression.
 *
 * @since 4.0.0
 */
public class CollectClauseTest {
    private CompileResult negativeResult;

    @BeforeClass
    public void setup() {
        negativeResult = BCompileUtil.compile("test-src/query/collect_clause_negative.bal");
    }

    @Test
    public void testNegativeCases() {
        int i = 0;
        validateError(negativeResult, i++, "incompatible types: expected 'int', found '[seq int]'", 19, 25);
        validateError(negativeResult, i++, "incompatible types: expected 'string', found 'int'", 21, 30);
        validateError(negativeResult, i++, "incompatible types: expected 'string', found 'int'", 26, 29);
        validateError(negativeResult, i++, "incompatible types: expected 'int[]', found 'int'", 28, 29);
        validateError(negativeResult, i++, "incompatible types: expected 'int[]', found 'seq string'", 33, 33);
        validateError(negativeResult, i++, "incompatible types: expected '([int]|record {| int n; |})', " +
                "found 'seq string'", 35, 45);
        validateError(negativeResult, i++, "incompatible types: expected 'string[]', found 'seq int'", 37, 39);
        validateError(negativeResult, i++, "incompatible types: expected 'string[]', found 'seq int'", 39, 43);
        validateError(negativeResult, i++, "incompatible types: expected 'string', found 'seq string'", 44, 25);
        validateError(negativeResult, i++, "sequence variable in invalid context", 44, 25);
        validateError(negativeResult, i++, "operator '+' not defined for 'seq int' and 'int'", 46, 25);
        validateError(negativeResult, i++, "sequence variable in invalid context", 46, 25);
        validateError(negativeResult, i++, "incompatible types: expected 'int', found '[seq int]'", 48, 46);
        validateError(negativeResult, i++, "arguments not allowed after sequence argument", 53, 41);
        validateError(negativeResult, i++, "arguments not allowed after sequence argument", 57, 41);
        validateError(negativeResult, i++, "arguments not allowed after sequence argument", 61, 41);
        validateError(negativeResult, i++, "arguments not allowed after sequence argument", 64, 49);
        validateError(negativeResult, i++, "sequence variable in invalid context", 69, 25);
        validateError(negativeResult, i++, "operator '+' not defined for 'seq int' and 'int'", 74, 26);
        validateError(negativeResult, i++, "sequence variable in invalid context", 74, 26);
        validateError(negativeResult, i++, "incompatible types: expected 'int', found '[seq int,seq int]'", 76, 33);
        validateError(negativeResult, i++, "sequence variable in invalid context", 76, 34);
        validateError(negativeResult, i++, "sequence variable in invalid context", 76, 42);
        validateError(negativeResult, i++, "invalid record binding pattern with type " +
                "'(record {| int salary; int bonus; |}|record {| int salary; int bonus; |})'", 80, 18);
        validateError(negativeResult, i++, "'_' is a keyword, and may not be used as an identifier", 80, 23);
        validateError(negativeResult, i++, "undefined symbol 'salary'", 81, 25);
        validateError(negativeResult, i++, "user defined functions are not allowed in collect clause", 90, 25);
        validateError(negativeResult, i++, "user defined functions are not allowed in collect clause", 92, 26);
        validateError(negativeResult, i++, "sequence value cannot assign to fixed length array", 97, 26);
        validateError(negativeResult, i++, "sequence value cannot assign to fixed length array", 99, 60);
        validateError(negativeResult, i++, "user defined functions are not allowed in collect clause", 104, 25);
        validateError(negativeResult, i++, "arguments not allowed after sequence argument", 104, 37);
        Assert.assertEquals(negativeResult.getErrorCount(), i);
    }
}
