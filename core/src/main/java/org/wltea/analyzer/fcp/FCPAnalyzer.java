package org.wltea.analyzer.fcp;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.synonym.SynonymMap;

import java.util.Arrays;
import java.util.List;

public final class FCPAnalyzer extends Analyzer {
    /** Default maximum allowed token length */
    public static final boolean DEFAULT_SPLIT_COMPLETE = false;

    // 决定分词时对 英文、数字 是否进行完全切分，默认为 false，表示数字和英文为一个整体，不会继续向下切分，完全切分的话 splitComplete = true
    private boolean splitComplete = false;
    // 默认为建立 索引模式， 如果为 查询模式 indexMode = false
    private final boolean indexMode;
    // 特殊字符的映射，默认为 true 表示模糊匹配特殊字符。如果设置为 false ，将会把原始的char放到最终分词结果中。
    private boolean uselessMapping = true;
    // 默认文本是正确文本，其中的空白是有意义的，不能忽略空白。如果认为原文中的空白由于ETL错误引入，应该忽略空白。
    private boolean ignoreBlank = false;
    // 是否使用 first char position ，默认使用，如果为 false，则变为 lcp_analyzer
    private boolean useFirstPos = true;
    // 是否显示 offset，默认随着 indexMode 变化
    private boolean showOffset;

    private int maxTokenLength = CombineCharFilter.DEFAULT_MAX_WORD_LEN;

    public FCPAnalyzer() {
        this(ExtendFilter.DEFAULT_INDEX_MODE);
    }
    public FCPAnalyzer(boolean indexMode) {
        this.indexMode = indexMode;
        // 改变 showOffset 的默认值
        if (indexMode) {
            showOffset = false;
        } else {
            showOffset = true;
        }
    }

    public FCPAnalyzer setIgnoreBlank(boolean ignoreBlank) {
        this.ignoreBlank = ignoreBlank;
        return this;
    }

    public FCPAnalyzer setUselessMapping(boolean uselessMapping) {
        this.uselessMapping = uselessMapping;
        return this;
    }

    public FCPAnalyzer setSplitComplete(boolean splitComplete) {
        this.splitComplete = splitComplete;
        return this;
    }

    public FCPAnalyzer setShowOffset(boolean showOffset) {
        this.showOffset = showOffset;
        return this;
    }

    public FCPAnalyzer setUseFirstPos(boolean useFirstPos) {
        this.useFirstPos = useFirstPos;
        return this;
    }

    /**
     * Set the max allowed token length.  Tokens larger than this will be chopped
     * up at this token length and emitted as multiple tokens.  If you need to
     * skip such large tokens, you could increase this max length, and then
     * use {@code LengthFilter} to remove long tokens.  The default is
     * {@link StandardAnalyzer#DEFAULT_MAX_TOKEN_LENGTH}.
     */
    public FCPAnalyzer setMaxTokenLength(int length) {
        maxTokenLength = length;
        return this;
    }

    /** Returns the current maximum token length
     *
     *  @see #setMaxTokenLength */
    public int getMaxTokenLength() {
        return maxTokenLength;
    }

    public boolean isIgnoreBlank() {
        return ignoreBlank;
    }


    public boolean isIndexMode() {
        return indexMode;
    }

    public boolean isUseFirstPos() {
        return useFirstPos;
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {
        final Tokenizer src = new NGramTokenizer(1, 1);
        TokenStream tok = new FormatFilter(src);
        if (!splitComplete) {
            tok = new CombineCharFilter(tok, maxTokenLength);
        }

        tok = new ExtendFilter(tok, indexMode)
                .setShowOffset(showOffset)
                .setIgnoreBlank(ignoreBlank)
                .setUseFirstPos(useFirstPos)
                .setUselessMapping(uselessMapping);
        return new TokenStreamComponents(src, tok);
    }

    @Override
    public String toString() {
        return "FCPAnalyzer{" +
                "splitComplete=" + splitComplete +
                ", indexMode=" + indexMode +
                ", showOffset=" + showOffset +
                ", uselessMapping=" + uselessMapping +
                ", ignoreBlank=" + ignoreBlank +
                ", useFirstPos=" + useFirstPos +
                ", maxTokenLength=" + maxTokenLength +
                '}';
    }

}
