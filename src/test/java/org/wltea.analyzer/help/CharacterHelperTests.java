package org.wltea.analyzer.help;

import org.junit.Assert;
import org.junit.Test;
import org.wltea.analyzer.help.CharacterHelper;

public class CharacterHelperTests {

    @Test
    public void regularizeInputNotNullOutputNotNull() {
        final char input = '\u3010';
        final char retval = CharacterHelper.regularize(input);
        Assert.assertEquals('\u3010', retval);
    }

    @Test
    public void regularizeInputNotNullOutputNotNull2() {
        final char input = '\u3000';
        final char retval = CharacterHelper.regularize(input);
        Assert.assertEquals(' ', retval);
    }

    @Test
    public void regularizeInputNotNullOutputNotNull3() {
        final char input = '\uff01';
        final char retval = CharacterHelper.regularize(input);
        Assert.assertEquals('!', retval);
    }

    @Test
    public void regularizeInputPOutputp() {
        final char input = 'P';
        final char retval = CharacterHelper.regularize(input);
        Assert.assertEquals('p', retval);
    }

    @Test
    public void isSpaceLetterInputNotNullOutputTrue() {
        final char input = '\t';
        final boolean retval = CharacterHelper.isSpaceLetter(input);
        Assert.assertEquals(true, retval);
    }

    @Test
    public void isSpaceLetterInputNotNullOutputFalse() {
        final char input = '\u0000';
        final boolean retval = CharacterHelper.isSpaceLetter(input);
        Assert.assertEquals(false, retval);
    }

    @Test
    public void isEnglishLetterInputPOutputTrue() {
        final char input = 'P';
        final boolean retval = CharacterHelper.isEnglishLetter(input);
        Assert.assertEquals(true, retval);
    }

    @Test
    public void isEnglishLetterInputNotNullOutputFalse() {
        final char input = '\u8061';
        final boolean retval = CharacterHelper.isEnglishLetter(input);
        Assert.assertEquals(false, retval);
    }

    @Test
    public void isArabicNumberInput9OutputTrue() {
        final char input = '9';
        final boolean retval = CharacterHelper.isArabicNumber(input);
        Assert.assertEquals(true, retval);
    }

    @Test
    public void isArabicNumberInputNotNullOutputFalse() {
        final char input = '\u0000';
        final boolean retval = CharacterHelper.isArabicNumber(input);
        Assert.assertEquals(false, retval);
    }

}
