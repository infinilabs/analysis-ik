package org.wltea.analyzer.fcp;

import java.util.List;

/**
 * compose term
 */
class TokenBody {
    String termBuffer;
    int startOffset, endOffset;
    // position 用于表示在 elasticsearch 分词时得到的 position， 通过 curr.position - prev.position 得到 positionIncrement
    // startPosition、endPosition 用于收集 那些在 词库中 扩展出来的 token，主要给 ik_smart 使用
    int position, startPosition = -1, endPosition = -1;
    String type;

    List<TokenBody> child;

    TokenBody(){}
    TokenBody(String termBuffer, int startOffset, int endOffset, int position, int startPosition, int endPosition, String type){
        this.termBuffer = termBuffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.position = position;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.type = type;
    }


    TokenBody copy() {
        return new TokenBody(termBuffer, startOffset, endOffset, position, startPosition, endPosition, "<UNDEFINED>");
    }

    @Override
    public String toString() {
        return "TokenBody{" +
                "termBuffer='" + termBuffer + '\'' +
                ", startOffset=" + startOffset +
                ", endOffset=" + endOffset +
                ", position=" + position +
                ", startPosition=" + startPosition +
                ", endPosition=" + endPosition +
                ", type='" + type + '\'' +
                ", child=" + child +
                '}';
    }
}
