/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package software.amazon.ai.integration.tests;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.mxnet.engine.MxGradientCollector;
import software.amazon.ai.integration.IntegrationTest;
import software.amazon.ai.integration.exceptions.FailedTestException;
import software.amazon.ai.integration.util.Assertions;
import software.amazon.ai.integration.util.RunAsTest;
import software.amazon.ai.ndarray.NDArray;
import software.amazon.ai.ndarray.NDArrays;
import software.amazon.ai.ndarray.NDManager;
import software.amazon.ai.ndarray.types.Shape;
import software.amazon.ai.training.GradientCollector;

public class NDArrayElementArithmeticOpTest {

    public static void main(String[] args) {
        String[] cmd = {"-c", NDArrayElementArithmeticOpTest.class.getName()};
        new IntegrationTest()
                .runTests(
                        Stream.concat(Arrays.stream(cmd), Arrays.stream(args))
                                .toArray(String[]::new));
    }

    private void testCornerCase(
            NDManager manager,
            BiFunction<NDArray, NDArray, NDArray> arrayArithmeticOp,
            BiFunction<Float, Float, Float> arithmeticOp,
            boolean inplace)
            throws FailedTestException {
        // test scalar with scalar
        float x1 = 10f;
        float x2 = 2f;
        NDArray array1 = manager.create(x1);
        NDArray array2 = manager.create(x2);
        NDArray result = arrayArithmeticOp.apply(array1, array2);
        Assertions.assertEquals(arithmeticOp.apply(x1, x2), result.getFloat());
        if (inplace) {
            Assertions.assertInPlace(array1, result);
        } else {
            // other cases only apply to non inplace test
            // test NDArray with scalar
            x1 = 10f;
            x2 = 5f;
            float y = arithmeticOp.apply(x1, x2);
            array1 = manager.create(new float[] {x1, x1});
            array2 = manager.create(x2);
            NDArray actual = manager.create(new float[] {y, y});
            Assertions.assertEquals(actual, arrayArithmeticOp.apply(array1, array2));

            // test zero-dim with zero-dim
            array1 = manager.create(new Shape(4, 0, 1));
            array2 = manager.create(new Shape(1, 0));
            Assertions.assertEquals(
                    manager.create(new Shape(4, 0, 0)), arrayArithmeticOp.apply(array1, array2));

            // test NDArray with zero-dim
            array1 = manager.create(new float[] {10f});
            array2 = manager.create(new Shape(2, 0, 3));
            Assertions.assertEquals(
                    manager.create(new Shape(2, 0, 3)), arrayArithmeticOp.apply(array1, array2));
        }
    }

    private void testReverseCornerCase(
            NDManager manager,
            NDArray scalarNDArray,
            Function<NDArray, NDArray> arrayArithmeticOp,
            BiFunction<Float, Float, Float> arithmeticOp,
            boolean inplace)
            throws FailedTestException {
        // scalar with number
        float x1 = scalarNDArray.getFloat();
        float x2 = 2f;
        NDArray ndArray2 = manager.create(x2);
        NDArray result = arrayArithmeticOp.apply(ndArray2);
        Assertions.assertEquals(arithmeticOp.apply(x1, x2), result.getFloat());
        if (inplace) {
            Assertions.assertInPlace(scalarNDArray, result);
        }
    }

    private void testScalarCornerCase(
            NDManager manager,
            BiFunction<NDArray, Number, NDArray> arrayArithmeticOp,
            BiFunction<Float, Float, Float> arithmeticOp,
            boolean inplace)
            throws FailedTestException {
        // scalar with number
        float x1 = 20f;
        float x2 = 4f;
        NDArray ndArray = manager.create(x1);
        NDArray result = arrayArithmeticOp.apply(ndArray, x2);
        Assertions.assertEquals(arithmeticOp.apply(x1, x2), result.getFloat());
        if (inplace) {
            Assertions.assertInPlace(ndArray, result);
        } else {
            // zero-dim with number
            ndArray = manager.create(new Shape(2, 0));
            Assertions.assertEquals(
                    manager.create(new Shape(2, 0)), arrayArithmeticOp.apply(ndArray, x2));
        }
    }

    private void testReverseScalarCornerCase(
            NDManager manager,
            BiFunction<Number, NDArray, NDArray> arrayArithmeticOp,
            BiFunction<Float, Float, Float> arithmeticOp,
            boolean inplace)
            throws FailedTestException {
        // number with scalar
        float x1 = 9f;
        float x2 = 3f;
        NDArray ndArray = manager.create(x1);
        NDArray result = arrayArithmeticOp.apply(x2, ndArray);
        Assertions.assertEquals(arithmeticOp.apply(x2, x1), result.getFloat());
        if (inplace) {
            Assertions.assertInPlace(ndArray, result);
        } else {
            // number with zero-dim
            ndArray = manager.create(new Shape(0, 2, 3));
            Assertions.assertEquals(
                    manager.create(new Shape(0, 2, 3)), arrayArithmeticOp.apply(x2, ndArray));
        }
    }

    @RunAsTest
    public void testAddScalar() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray lhs = manager.create(new float[] {1f, 2f, 3f, 4f});
            NDArray result;
            try (GradientCollector gradCol = new MxGradientCollector()) {
                lhs.attachGradient();
                result = NDArrays.add(lhs, 2);
                // check add scalar result
                gradCol.backward(result);

                Assertions.assertFalse(
                        NDArrays.equals(lhs, result),
                        "None in-place operator returned in-place result");
                NDArray actual = manager.create(new float[] {3f, 4f, 5f, 6f});
                Assertions.assertEquals(
                        actual, result, "AddScala: Incorrect value in summed array");

                // check add backward
                NDArray expectedGradient = manager.create(new float[] {1f, 1f, 1f, 1f});
                Assertions.assertEquals(
                        expectedGradient,
                        lhs.getGradient(),
                        "AddScala backward: Incorrect gradient after backward");
            }
            // test inplace
            lhs = manager.create(new float[] {1f, 2f, 3f, 4f});
            result = NDArrays.addi(lhs, 2);
            Assertions.assertInPlace(lhs, result, "In-place summation failed");
            NDArray actual = manager.create(new float[] {3f, 4f, 5f, 6f});
            Assertions.assertEquals(actual, result, "Incorrect value in summed array");

            testScalarCornerCase(manager, NDArrays::add, Float::sum, false);
            testScalarCornerCase(manager, NDArrays::addi, Float::sum, true);
        }
    }

    @RunAsTest
    public void testAddNDArray() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray addend = manager.create(new float[] {1f, 2f, 3f, 4f});
            NDArray addendum = manager.create(new float[] {2f, 3f, 4f, 5f});
            NDArray result = NDArrays.add(addend, addendum);
            Assertions.assertFalse(
                    NDArrays.equals(addend, result),
                    "None in-place operator returned in-place result");

            result = NDArrays.addi(addend, addendum);
            Assertions.assertInPlace(result, addend, "In-place summation failed");

            NDArray actual = manager.create(new float[] {3f, 5f, 7f, 9f});
            Assertions.assertEquals(actual, result, "Incorrect value in summed array");

            NDArray[] toAddAll = {
                manager.create(new float[] {1, 2, 3, 4}, new Shape(2, 2)),
                manager.create(new float[] {4, 3, 2, 1}, new Shape(2, 2)),
                manager.create(new float[] {2, 2, 2, 2}, new Shape(2, 2))
            };

            NDArray addAll = NDArrays.add(toAddAll);
            Assertions.assertFalse(
                    addAll.equals(toAddAll[0]), "None in-place operator returned in-place result");

            addAll = NDArrays.addi(toAddAll);
            Assertions.assertTrue(addAll.equals(toAddAll[0]), "In-place summation failed");

            actual = manager.create(new float[] {7, 7, 7, 7}, new Shape(2, 2));
            Assertions.assertEquals(actual, addAll, "Incorrect value in summed array");

            testCornerCase(manager, NDArrays::add, Float::sum, false);
            testCornerCase(manager, NDArrays::addi, Float::sum, true);
        }
    }

    @RunAsTest
    public void testSubScalar() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray minuend = manager.create(new float[] {6, 9, 12, 11, 0});
            NDArray result = NDArrays.sub(minuend, 3);
            NDArray inPlaceResult = NDArrays.subi(minuend, 3);
            NDArray actual = manager.create(new float[] {3, 6, 9, 8, -3});
            Assertions.assertEquals(
                    actual, result, "Scalar subtraction: Incorrect value in result ndarray");
            Assertions.assertEquals(
                    actual,
                    inPlaceResult,
                    "Scalar in-place subtraction: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    minuend, inPlaceResult, "Scalar subtraction: In-place operation failed");

            testScalarCornerCase(manager, NDArrays::sub, (x, y) -> x - y, false);
            testScalarCornerCase(manager, NDArrays::subi, (x, y) -> x - y, true);
        }
    }

    @RunAsTest
    public void testSubNDArray() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray minuend = manager.create(new float[] {6, 9, 12, 15, 0});
            NDArray subtrahend = manager.create(new float[] {2, 3, 4, 5, 6});
            NDArray result = NDArrays.sub(minuend, subtrahend);
            NDArray inPlaceResult = NDArrays.subi(minuend, subtrahend);
            NDArray actual = manager.create(new float[] {4, 6, 8, 10, -6});
            Assertions.assertEquals(
                    actual, result, "Element wise subtraction: Incorrect value in result ndarray");
            Assertions.assertEquals(
                    actual,
                    inPlaceResult,
                    "Scalar in-place subtraction: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    minuend, inPlaceResult, "Element wise subtraction: In-place operation failed");

            testCornerCase(manager, NDArrays::sub, (x, y) -> x - y, false);
            testCornerCase(manager, NDArrays::subi, (x, y) -> x - y, true);
        }
    }

    @RunAsTest
    public void testReverseSubScalar() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray minuend = manager.create(new float[] {6, 91, 12, 215, 180});
            NDArray result = NDArrays.sub(180, minuend);
            NDArray inPlaceResult = NDArrays.subi(180, minuend);
            NDArray actual = manager.create(new float[] {174, 89, 168, -35, 0});
            Assertions.assertEquals(
                    actual,
                    result,
                    "Scalar reverse subtraction: Incorrect value in result ndarray");
            Assertions.assertTrue(
                    NDArrays.equals(actual, inPlaceResult),
                    "Scalar in-place reverse subtraction: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    minuend,
                    inPlaceResult,
                    "Scalar reverse subtraction: In-place operation failed");

            testReverseScalarCornerCase(manager, NDArrays::sub, (x, y) -> x - y, false);
            testReverseScalarCornerCase(manager, NDArrays::subi, (x, y) -> x - y, true);
        }
    }

    @RunAsTest
    public void testReverseSubNDArray() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray minuend = manager.create(new float[] {6, 9, 12, 15, 45});
            NDArray subtrahend = manager.create(new float[] {24, 63, 96, 15, 90});
            NDArray result = minuend.getNDArrayInternal().rsub(subtrahend);
            NDArray inPlaceResult = minuend.getNDArrayInternal().rsubi(subtrahend);
            NDArray actual = manager.create(new float[] {18, 54, 84, 0, 45});
            Assertions.assertEquals(
                    actual,
                    result,
                    "Reverse Element wise subtraction: Incorrect value in result ndarray");
            Assertions.assertEquals(
                    actual,
                    inPlaceResult,
                    "Reverse Element wise in-place subtraction: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    minuend,
                    inPlaceResult,
                    "Reverse Element wise subtraction: In-place operation failed");

            NDArray scalarNDArray = manager.create(5f);
            testReverseCornerCase(
                    manager,
                    scalarNDArray,
                    scalarNDArray.getNDArrayInternal()::rsub,
                    (x, y) -> y - x,
                    false);
            testReverseCornerCase(
                    manager,
                    scalarNDArray,
                    scalarNDArray.getNDArrayInternal()::rsubi,
                    (x, y) -> y - x,
                    true);

            NDArray ndArray1 = manager.create(new Shape(4, 0, 1));
            NDArray ndArray2 = manager.create(new Shape(1, 0));
            Assertions.assertEquals(
                    manager.create(new Shape(4, 0, 0)),
                    ndArray1.getNDArrayInternal().rsub(ndArray2));

            ndArray1 = manager.create(new float[] {10f});
            ndArray2 = manager.create(new Shape(2, 0, 3));
            Assertions.assertEquals(
                    manager.create(new Shape(2, 0, 3)),
                    ndArray1.getNDArrayInternal().rsub(ndArray2));
        }
    }

    @RunAsTest
    public void testMulScalar() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray multiplicand = manager.create(new float[] {6, 9, -12, 15, 0});
            NDArray result = NDArrays.mul(multiplicand, 3);
            NDArray inPlaceResult = NDArrays.muli(multiplicand, 3);
            NDArray actual = manager.create(new float[] {18, 27, -36, 45, 0});
            Assertions.assertEquals(
                    actual, result, "Scalar multiplication: Incorrect value in result ndarray");
            Assertions.assertEquals(
                    actual,
                    inPlaceResult,
                    "Scalar in-place multiplication: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    multiplicand,
                    inPlaceResult,
                    "Scalar multiplication: In-place operation failed");

            testScalarCornerCase(manager, NDArrays::mul, (x, y) -> x * y, false);
            testScalarCornerCase(manager, NDArrays::muli, (x, y) -> x * y, true);
        }
    }

    @RunAsTest
    public void testMulNDArray() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray multiplicand = manager.create(new float[] {6, 9, 12, 15, 0});
            NDArray with = manager.create(new float[] {2, 3, 4, 5, 6});
            NDArray result = NDArrays.mul(multiplicand, with);
            NDArray inPlaceResult = NDArrays.muli(multiplicand, with);
            NDArray actual = manager.create(new float[] {12, 27, 48, 75, 0});
            Assertions.assertEquals(
                    actual,
                    result,
                    "Element wise multiplication: Incorrect value in result ndarray");
            Assertions.assertEquals(
                    actual,
                    inPlaceResult,
                    "Scalar in-place multiplication: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    multiplicand,
                    inPlaceResult,
                    "Element wise multiplication: In-place operation failed");

            NDArray[] toMulAll = {
                manager.create(new float[] {1, 2, 3, 4}, new Shape(2, 2)),
                manager.create(new float[] {4, 3, 2, 1}, new Shape(2, 2)),
                manager.create(new float[] {2, 2, 2, 2}, new Shape(2, 2))
            };
            NDArray mulAll = NDArrays.mul(toMulAll);
            NDArray mulAllInPlace = NDArrays.muli(toMulAll);
            Assertions.assertFalse(
                    mulAll.equals(toMulAll[0]), "None in-place operator returned in-place result");
            Assertions.assertTrue(mulAllInPlace.equals(toMulAll[0]), "In-place summation failed");
            actual = manager.create(new float[] {8, 12, 12, 8}, new Shape(2, 2));
            Assertions.assertEquals(actual, mulAll, "Incorrect value in summed array");
            Assertions.assertEquals(actual, mulAllInPlace, "Incorrect value in summed array");

            testCornerCase(manager, NDArrays::mul, (x, y) -> x * y, false);
            testCornerCase(manager, NDArrays::muli, (x, y) -> x * y, true);
        }
    }

    @RunAsTest
    public void testDivScalar() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray dividend = manager.create(new float[] {6, 9, 12, 15, 0});
            NDArray result = NDArrays.div(dividend, 3);
            NDArray inPlaceResult = NDArrays.divi(dividend, 3);
            NDArray actual = manager.create(new float[] {2, 3, 4, 5, 0});
            Assertions.assertEquals(
                    actual, result, "Scalar division: Incorrect value in result ndarray");
            Assertions.assertEquals(
                    actual,
                    inPlaceResult,
                    "Scalar in-place division: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    dividend, inPlaceResult, "Scalar division: In-place operation failed");

            testScalarCornerCase(manager, NDArrays::div, (x, y) -> x / y, false);
            testScalarCornerCase(manager, NDArrays::divi, (x, y) -> x / y, true);
        }
    }

    @RunAsTest
    public void testDivNDArray() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray dividend = manager.create(new float[] {6, 9, 12, 15, 0});
            NDArray divisor = manager.create(new float[] {2, 3, 4, 5, 6});
            NDArray result = NDArrays.div(dividend, divisor);
            NDArray inPlaceResult = NDArrays.divi(dividend, divisor);
            NDArray actual = manager.create(new float[] {3, 3, 3, 3, 0});
            Assertions.assertEquals(
                    actual, result, "Element wise Division: Incorrect value in result ndarray");
            Assertions.assertEquals(
                    actual,
                    inPlaceResult,
                    "Scalar in-place division: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    dividend, inPlaceResult, "Element wise division: In-place operation failed");

            testCornerCase(manager, NDArrays::div, (x, y) -> x / y, false);
            testCornerCase(manager, NDArrays::divi, (x, y) -> x / y, true);
        }
    }

    @RunAsTest
    public void testReverseDivScalar() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray dividend = manager.create(new float[] {6, 9, 12, 15, 45});
            NDArray result = NDArrays.div(180, dividend);
            NDArray inPlaceResult = NDArrays.divi(180, dividend);
            NDArray actual = manager.create(new float[] {30, 20, 15, 12, 4});
            Assertions.assertEquals(
                    actual, result, "Scalar reverse division: Incorrect value in result ndarray");
            Assertions.assertEquals(
                    actual,
                    inPlaceResult,
                    "Scalar in-place reverse division: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    dividend, inPlaceResult, "Scalar reverse division: In-place operation failed");

            testReverseScalarCornerCase(manager, NDArrays::sub, (x, y) -> x - y, false);
            testReverseScalarCornerCase(manager, NDArrays::subi, (x, y) -> x - y, true);
        }
    }

    @RunAsTest
    public void testReverseDivNDArray() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray dividend = manager.create(new float[] {6, 9, 12, 15, 45});
            NDArray divisor = manager.create(new float[] {24, 63, 96, 15, 90});
            NDArray result = dividend.getNDArrayInternal().rdiv(divisor);
            NDArray inPlaceResult = dividend.getNDArrayInternal().rdivi(divisor);
            NDArray actual = manager.create(new float[] {4, 7, 8, 1, 2});
            Assertions.assertEquals(
                    actual,
                    result,
                    "Reverse Element wise Division: Incorrect value in result ndarray");
            Assertions.assertTrue(
                    NDArrays.equals(actual, inPlaceResult),
                    "Reverse Element wise in-place division: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    dividend,
                    inPlaceResult,
                    "Reverse Element wise division: In-place operation failed");

            NDArray scalarNDArray = manager.create(24f);
            testReverseCornerCase(
                    manager,
                    scalarNDArray,
                    scalarNDArray.getNDArrayInternal()::rdiv,
                    (x, y) -> y / x,
                    false);
            testReverseCornerCase(
                    manager,
                    scalarNDArray,
                    scalarNDArray.getNDArrayInternal()::rdivi,
                    (x, y) -> y / x,
                    true);
        }
    }

    @RunAsTest
    public void testModScalar() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray dividend = manager.create(new float[] {5, 6, 7, 8, 9});
            NDArray result = NDArrays.mod(dividend, 3);
            NDArray inPlaceResult = NDArrays.modi(dividend, 3);
            NDArray actual = manager.create(new float[] {2, 0, 1, 2, 0});
            Assertions.assertEquals(
                    actual, result, "Scalar Remainder: Incorrect value in result ndarray");
            Assertions.assertEquals(
                    actual,
                    inPlaceResult,
                    "Scalar in-place Remainder: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    dividend, inPlaceResult, "Scalar division: In-place operation failed");

            testScalarCornerCase(manager, NDArray::mod, (x, y) -> x % y, false);
            testScalarCornerCase(manager, NDArray::modi, (x, y) -> x % y, true);
        }
    }

    @RunAsTest
    public void testModNDArray() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray dividend = manager.create(new float[] {7, 8, 9, 10, 11});
            NDArray divisor = manager.create(new float[] {2, 3, 4, 5, 6});
            NDArray result = NDArrays.mod(dividend, divisor);
            NDArray inPlaceResult = NDArrays.modi(dividend, divisor);
            NDArray actual = manager.create(new float[] {1, 2, 1, 0, 5});
            Assertions.assertEquals(
                    actual, result, "Element wise Remainder: Incorrect value in result ndarray");
            Assertions.assertEquals(
                    actual,
                    inPlaceResult,
                    "Scalar in-place Remainder: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    dividend, inPlaceResult, "Element wise Remainder: In-place operation failed");

            testCornerCase(manager, NDArrays::mod, (x, y) -> x % y, false);
            testCornerCase(manager, NDArrays::modi, (x, y) -> x % y, true);
        }
    }

    @RunAsTest
    public void testReverseModScalar() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray dividend = manager.create(new float[] {5, 6, 7, 8, 9});
            NDArray result = NDArrays.mod(180, dividend);
            NDArray inPlaceResult = NDArrays.modi(180, dividend);
            NDArray actual = manager.create(new float[] {0, 0, 5, 4, 0});
            Assertions.assertEquals(
                    actual, result, "Scalar reverse Remainder: Incorrect value in result ndarray");
            Assertions.assertEquals(
                    actual,
                    inPlaceResult,
                    "Scalar in-place reverse Remainder: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    dividend,
                    inPlaceResult,
                    "Scalar Remainder division: In-place operation failed");

            testReverseScalarCornerCase(manager, NDArrays::mod, (x, y) -> x % y, false);
            testReverseScalarCornerCase(manager, NDArrays::modi, (x, y) -> x % y, true);
        }
    }

    @RunAsTest
    public void testReverseModNDArray() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray dividend = manager.create(new float[] {7, 8, 9, 10, 11});
            NDArray divisor = manager.create(new float[] {20, 21, 22, 23, 24});
            NDArray result = dividend.getNDArrayInternal().rmod(divisor);
            NDArray inPlaceResult = dividend.getNDArrayInternal().rmodi(divisor);
            NDArray actual = manager.create(new float[] {6, 5, 4, 3, 2});
            Assertions.assertEquals(
                    actual,
                    result,
                    "Reverse Element wise Remainder: Incorrect value in result ndarray");
            Assertions.assertTrue(
                    NDArrays.equals(actual, inPlaceResult),
                    "Reverse Element wise in-place Remainder: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    dividend,
                    inPlaceResult,
                    "Reverse Element wise Remainder: In-place operation failed");

            NDArray scalarNDArray = manager.create(20f);
            testReverseCornerCase(
                    manager,
                    scalarNDArray,
                    scalarNDArray.getNDArrayInternal()::rmod,
                    (x, y) -> y % x,
                    false);
            testReverseCornerCase(
                    manager,
                    scalarNDArray,
                    scalarNDArray.getNDArrayInternal()::rmodi,
                    (x, y) -> y % x,
                    true);
        }
    }

    @RunAsTest
    public void testPowScalar() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.create(new float[] {6, 0, -1, 5, 2}, new Shape(1, 5));
            NDArray result = array.pow(2);
            NDArray inPlaceResult = array.powi(2);
            NDArray actual = manager.create(new float[] {36, 0, 1, 25, 4}, new Shape(1, 5));
            Assertions.assertAlmostEquals(actual, result);
            Assertions.assertAlmostEquals(actual, inPlaceResult);
            Assertions.assertInPlace(
                    array, inPlaceResult, "Scalar power: In-place operation failed");

            testScalarCornerCase(manager, NDArray::pow, (x, y) -> (float) Math.pow(x, y), false);
            testScalarCornerCase(manager, NDArray::powi, (x, y) -> (float) Math.pow(x, y), true);
        }
    }

    @RunAsTest
    public void testPowNDArray() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.create(new float[] {6, 9, 12, 2, 0});
            NDArray power = manager.create(new float[] {3, 0, 1, -2, 3});
            NDArray result = array.pow(power);
            NDArray inPlaceResult = array.powi(power);
            NDArray actual = manager.create(new float[] {216, 1, 12, 0.25f, 0});
            Assertions.assertEquals(
                    actual, result, "Scalar power: Incorrect value in result ndarray");
            Assertions.assertEquals(
                    actual,
                    inPlaceResult,
                    "Scalar in-place power: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    array, inPlaceResult, "Scalar power: In-place operation failed");

            testCornerCase(manager, NDArrays::pow, (x, y) -> (float) Math.pow(x, y), false);
            testCornerCase(manager, NDArrays::powi, (x, y) -> (float) Math.pow(x, y), true);
        }
    }

    @RunAsTest
    public void testReversePowScalar() throws FailedTestException {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.create(new float[] {3, 4, 5, 6, 7});
            NDArray power = NDArrays.pow(2, array);
            NDArray inPlaceResult = NDArrays.powi(2, array);
            NDArray actual = manager.create(new float[] {8, 16, 32, 64, 128});
            Assertions.assertEquals(
                    actual, power, "Scalar reverse power: Incorrect value in result ndarray");
            Assertions.assertEquals(
                    actual,
                    inPlaceResult,
                    "Scalar in-place reverse power: Incorrect value in result ndarray");
            Assertions.assertInPlace(
                    array, inPlaceResult, "Scalar reverse division: In-place operation failed");

            testReverseScalarCornerCase(
                    manager, NDArrays::pow, (x, y) -> (float) Math.pow(x, y), false);
            testReverseScalarCornerCase(
                    manager, NDArrays::powi, (x, y) -> (float) Math.pow(x, y), true);
        }
    }
}
