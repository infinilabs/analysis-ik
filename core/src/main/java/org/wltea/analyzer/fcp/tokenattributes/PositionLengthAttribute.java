package org.wltea.analyzer.fcp.tokenattributes;

import org.apache.lucene.util.Attribute;

/** Determines how many positions this
 *  token spans.  Very few analyzer components actually
 *  produce this attribute, and indexing ignores it, but
 *  it's useful to express the graph structure naturally
 *  produced by decompounding, word splitting/joining,
 *  synonym filtering, etc.
 *
 * <p>NOTE: this is optional, and most analyzers
 *  don't change the default value (1). */

@Deprecated
public interface PositionLengthAttribute extends Attribute {
    /**
     * Set the position length of this Token.
     * <p>
     * The default value is one.
     * @param positionLength how many positions this token
     *  spans.
     * @throws IllegalArgumentException if <code>positionLength</code>
     *         is zero or negative.
     * @see #getPositionLength()
     */
    public void setPositionLength(int positionLength);

    /** Returns the position length of this Token.
     * @see #setPositionLength
     */
    public int getPositionLength();
}

