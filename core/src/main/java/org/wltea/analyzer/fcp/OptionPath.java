package org.wltea.analyzer.fcp;

import java.util.Arrays;

/**
 * present a no conflict path for choose
 */
public class OptionPath implements Comparable<OptionPath> {
    private static final int DEFAULT_CAPACITY = 10;
    int[] groups;
    int size = 0;
    int payloadLength = 0;

    OptionPath() {
        groups = new int[DEFAULT_CAPACITY];
    }

    OptionPath(int capacity) {
        assert capacity > 0;
        groups = new int[capacity];
    }

    private OptionPath(int size, int[] groups) {
        this.size = size;
        int newCapacity = Math.max(size * 2, groups.length);
        this.groups = Arrays.copyOf(groups, newCapacity);
    }

    OptionPath copy() {
        return new OptionPath(this.size, this.groups);
    }

    void addElement(int startPosition, int endPosition) {
        assert endPosition > startPosition;
        this.size++;
        if (this.size*2 >= this.groups.length) {
            this.groups = Arrays.copyOf(this.groups, this.groups.length * 2);
        }
        this.payloadLength += (endPosition - startPosition + 1);
        this.groups[size*2 - 2] = startPosition;
        this.groups[size*2 - 1] = endPosition;
    }

    int getValueByIndex(int index) {
        assert -1 < index && index < this.groups.length;
        return this.groups[index];
    }

    int getEndPosition(int startPosition) {
        int endPosition = -1;
        for(int i = 0; i < size && this.groups[2*i] <= startPosition; i++) {
            if (startPosition == this.groups[2*i]) {
                endPosition = this.groups[2*i + 1];
            }
        }
        return endPosition;
    }

    int getPathLength() {
        return this.groups[this.size*2+1] - this.groups[0];
    }

    int getPathEnd() {
        return this.groups[size*2+1];
    }

    int getXWeight() {
        int product = 1;
        for(int i = 0; i < size; i++) {
            product *= (this.groups[2*i+1] - this.groups[2*i]);
        }
        return product;
    }

    int getPWeight() {
        int pWeight = 0;
        int p = 0;
        for(int i = 0; i < size; i++) {
            p++;
            pWeight += p * (this.groups[2*i+1] - this.groups[2*i]);
        }
        return pWeight;
    }

    // ik_smart 解决歧义问题的实现逻辑
    @Override
    public int compareTo(OptionPath o) {
        if (this.payloadLength != o.payloadLength) {
            return Integer.compare(this.payloadLength, o.payloadLength);
        } else if (this.size != o.size) {
            return Integer.compare(this.size, o.size);
        } else if (this.getPathLength() !=  o.getPathLength()) {
            return Integer.compare(this.getPathLength(), o.getPathLength());
        } else if(this.getPathEnd() != o.getPathEnd()) {
            return Integer.compare(this.getPathEnd(), o.getPathEnd());
        } else if (this.getXWeight() != o.getXWeight()) {
            return Integer.compare(this.getXWeight(), o.getXWeight());
        } else {
            return Integer.compare(this.getPWeight(), o.getPWeight());
        }
    }
}
