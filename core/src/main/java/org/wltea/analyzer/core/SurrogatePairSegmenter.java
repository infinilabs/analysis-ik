package org.wltea.analyzer.core;

import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.dic.Hit;

import java.util.Optional;

/**
 * Surrogate Pair Segmenter
 */
class SurrogatePairSegmenter implements ISegmenter {

    static final String SEGMENTER_NAME = "SURROGATE_PAIR_SEGMENTER";

    private int start;
    private int end;
    private Optional<Character> highSurrogate;

    SurrogatePairSegmenter() {
        this.start = -1;
        this.end = -1;
        this.highSurrogate = Optional.empty();
    }

    @Override
    public void analyze(AnalyzeContext context) {
        processSurrogatePairs(context);

        if (this.start == -1 && this.end == -1 && !this.highSurrogate.isPresent()) {
            context.unlockBuffer(SEGMENTER_NAME);
        } else {
            context.lockBuffer(SEGMENTER_NAME);
        }
    }

    @Override
    public void reset() {
        this.start = -1;
        this.end = -1;
        this.highSurrogate = Optional.empty();
    }

    private void processSurrogatePairs(AnalyzeContext context) {
        char currentChar = context.getCurrentChar();

        if (Character.isHighSurrogate(currentChar)) {
            this.highSurrogate = Optional.of(currentChar);
            this.start = context.getCursor();
        } else if (Character.isLowSurrogate(currentChar) && this.highSurrogate.isPresent()) {
            this.end = context.getCursor();
            outputSurrogatePairLexeme(context);
            this.highSurrogate = Optional.empty();
        } else {
            if (this.highSurrogate.isPresent()) {
                // Output the high surrogate as a single character lexeme
                outputSingleCharLexeme(context, this.start);
                this.highSurrogate = Optional.empty();
            }
            this.start = context.getCursor();
            this.end = -1;
        }

        if (context.isBufferConsumed() && this.highSurrogate.isPresent()) {
            // Output the high surrogate as a single character lexeme
            outputSingleCharLexeme(context, this.start);
            this.highSurrogate = Optional.empty();
            this.start = -1;
            this.end = -1;
        }
    }

    private void outputSurrogatePairLexeme(AnalyzeContext context) {
        if (this.start > -1 && this.end > -1) {
            StringBuilder sb = new StringBuilder();
            sb.append(context.getSegmentBuff()[this.start]);
            sb.append(context.getSegmentBuff()[this.end]);
            String lexemeText = sb.toString();
            Lexeme lexeme = new Lexeme(context.getBufferOffset(), this.start, this.end - this.start + 1, Lexeme.TYPE_CNCHAR);
            lexeme.setLexemeText(lexemeText);
            context.addLexeme(lexeme);
        }
    }

    private void outputSingleCharLexeme(AnalyzeContext context, int position) {
        if (position > -1) {
            char singleChar = context.getSegmentBuff()[position];
            String lexemeText = String.valueOf(singleChar);
            Lexeme lexeme = new Lexeme(context.getBufferOffset(), position, 1, Lexeme.TYPE_CNWORD);
            lexeme.setLexemeText(lexemeText);
            context.addLexeme(lexeme);
        }
    }
}