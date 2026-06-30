package org.wltea.analyzer.core;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class IKArbitratorTests {

    @Test
    public void fallbackPathUsesFullCrossPathEndAndStableOffset() throws Exception {
        LexemePath crossPath = new LexemePath();
        crossPath.addCrossLexeme(new Lexeme(0, 0, 21, Lexeme.TYPE_CNWORD));
        crossPath.addCrossLexeme(new Lexeme(0, 2, 60, Lexeme.TYPE_CNWORD));
        for (int i = 4; i <= 21; i++) {
            crossPath.addCrossLexeme(new Lexeme(0, i, 21, Lexeme.TYPE_CNWORD));
        }

        Assert.assertEquals(20, crossPath.size());

        LexemePath fallback = buildFallback(crossPath);

        Assert.assertNotNull(fallback);
        Assert.assertEquals(2, fallback.size());

        Lexeme first = fallback.pollFirst();
        Lexeme remain = fallback.pollFirst();

        Assert.assertEquals(0, first.getBeginPosition());
        Assert.assertEquals(21, first.getEndPosition());

        Assert.assertEquals(0, remain.getOffset());
        Assert.assertEquals(21, remain.getBeginPosition());
        Assert.assertEquals(62, remain.getEndPosition());
        Assert.assertEquals(Lexeme.TYPE_CNWORD, remain.getLexemeType());
    }

    @Test
    public void fallbackPathDoesNotTriggerForSmallLongCnWordCrossPath() throws Exception {
        LexemePath crossPath = new LexemePath();
        for (int i = 0; i < 6; i++) {
            crossPath.addCrossLexeme(new Lexeme(0, i * 2, 11, Lexeme.TYPE_CNWORD));
        }

        Assert.assertEquals(6, crossPath.size());
        Assert.assertNull(buildFallback(crossPath));
    }

    @Test
    public void fallbackPathStillTriggersWhenTotalLexemeCountIsTooHigh() throws Exception {
        LexemePath crossPath = new LexemePath();
        crossPath.addCrossLexeme(new Lexeme(0, 0, 100, Lexeme.TYPE_CNWORD));
        for (int i = 1; i <= 50; i++) {
            crossPath.addCrossLexeme(new Lexeme(0, i, 1, Lexeme.TYPE_CNWORD));
        }

        LexemePath fallback = buildFallback(crossPath);

        Assert.assertNotNull(fallback);
        Assert.assertEquals(1, fallback.size());
        Assert.assertEquals(100, fallback.peekFirst().getEndPosition());
    }

    private LexemePath buildFallback(LexemePath crossPath) throws Exception {
        Method method = IKArbitrator.class.getDeclaredMethod(
                "tryBuildFallbackPathForComplexCrossPath",
                LexemePath.class);
        method.setAccessible(true);
        return (LexemePath) method.invoke(new IKArbitrator(), crossPath);
    }
}
