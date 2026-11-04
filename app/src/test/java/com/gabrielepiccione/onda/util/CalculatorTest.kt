// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorTest {

    @Test fun precedence() = assertEquals("14", Calculator.evaluate("2+3*4"))
    @Test fun parentheses() = assertEquals("20", Calculator.evaluate("(2+3)*4"))
    @Test fun decimalComma() = assertEquals("3,75", Calculator.evaluate("1,5*2,5"))
    @Test fun percent() = assertEquals("12,5", Calculator.evaluate("50%*25"))
    @Test fun power() = assertEquals("1024", Calculator.evaluate("2^10"))
    @Test fun unaryMinus() = assertEquals("-6", Calculator.evaluate("-2*3"))
    @Test fun xAsMultiply() = assertEquals("21", Calculator.evaluate("3x7"))
    @Test fun plainNumberIsNotAnExpression() = assertNull(Calculator.evaluate("42"))
    @Test fun textIsNotAnExpression() = assertNull(Calculator.evaluate("whatsapp"))
    @Test fun divisionByZeroIsNull() = assertNull(Calculator.evaluate("1/0"))
    @Test fun unbalancedIsNull() = assertNull(Calculator.evaluate("(1+2"))
}
