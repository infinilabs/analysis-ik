package org.wltea.analyzer.fcp.tokenattributes;


import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/** Default implementation of {@link PositionLengthAttribute}. */
public class PositionLengthAttributeImpl extends AttributeImpl implements PositionLengthAttribute, Cloneable {
    private int positionLength = 1;

    /** Initializes this attribute with position length of 1. */
    public PositionLengthAttributeImpl() {}

    @Override
    public void setPositionLength(int positionLength) {
        if (positionLength < 1) {
            throw new IllegalArgumentException("Position length must be 1 or greater; got " + positionLength);
        }
        this.positionLength = positionLength;
    }

    @Override
    public int getPositionLength() {
        return positionLength;
    }

    @Override
    public void clear() {
        this.positionLength = 1;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (other instanceof PositionLengthAttributeImpl) {
            PositionLengthAttributeImpl _other = (PositionLengthAttributeImpl) other;
            return positionLength ==  _other.positionLength;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return positionLength;
    }

    @Override
    public void copyTo(AttributeImpl target) {
        PositionLengthAttribute t = (PositionLengthAttribute) target;
        t.setPositionLength(positionLength);
    }

    @Override
    public void reflectWith(AttributeReflector reflector) {
        reflector.reflect(PositionLengthAttribute.class, "positionLength", positionLength);
    }
}

