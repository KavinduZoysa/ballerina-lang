package org.wso2.ballerinalang.compiler.semantics.model.types;

import org.wso2.ballerinalang.compiler.util.TypeTags;

public class BSequenceType extends BType {
    public BType elementType;
    public BSequenceType(BType elementType) {
        super(TypeTags.SEQUENCE, null);
        this.elementType = elementType;
    }

    @Override
    public String toString() {
        return "seq " + elementType;
    }
}
