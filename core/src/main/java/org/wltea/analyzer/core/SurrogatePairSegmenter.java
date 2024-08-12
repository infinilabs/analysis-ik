/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 * 
 */
package org.wltea.analyzer.core;

import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.dic.Hit;

import java.util.*;
import java.util.Optional;

/**
 * 
 * 中文数量词子分词器
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
                this.highSurrogate = Optional.empty();
            }
            this.start = -1;
            this.end = -1;
        }

        if (context.isBufferConsumed() && this.highSurrogate.isPresent()) {
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
            Lexeme lexeme = new Lexeme(context.getBufferOffset(), this.start, this.end - this.start + 1, Lexeme.TYPE_CNWORD);
            lexeme.setLexemeText(lexemeText);
            context.addLexeme(lexeme);
        }
    }
}