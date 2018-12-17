package org.wltea.analyzer.core;

import org.junit.Assert;
import org.junit.Test;
import org.wltea.analyzer.core.Lexeme;

public class LexemeTests {

    @Test
    public void equalsInputNotNullOutputFalse() {
        final Lexeme objectUnderTest = new Lexeme(0, 0, 0, 0);
        objectUnderTest.setLength(-2_147_483_648);
        final Lexeme o = new Lexeme(0, 0, 0, 0);
        final boolean retval = objectUnderTest.equals(o);
        Assert.assertFalse(retval);
    }

    @Test
    public void compareToInputNotNullOutputNegative() {
        final Lexeme objectUnderTest = new Lexeme(0, -1_604_313_089, 1, 0);
        final Lexeme other = new Lexeme(0, -1_604_313_089, 0, 0);
        final int retval = objectUnderTest.compareTo(other);
        Assert.assertEquals(-1, retval);
    }

    @Test
    public void appendInputNotNullZeroOutputTrue() {
        final Lexeme objectUnderTest = new Lexeme(0, 0, 0, 0);
        final Lexeme l = new Lexeme(0, 0, 0, 0);
        final int lexemeType = 0;
        final boolean retval = objectUnderTest.append(l, lexemeType);
        Assert.assertTrue(retval);
    }

    @Test
    public void appendInputNotNullZeroOutputFalse() {
        final Lexeme objectUnderTest = new Lexeme(1, 0, 0, 0);
        final Lexeme l = new Lexeme(0, 0, 0, 0);
        final int lexemeType = 0;
        final boolean retval = objectUnderTest.append(l, lexemeType);
        Assert.assertFalse(retval);
    }

}
