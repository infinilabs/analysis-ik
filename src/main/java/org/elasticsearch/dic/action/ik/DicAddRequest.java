package org.elasticsearch.dic.action.ik;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;

import java.io.IOException;
import java.util.List;

/**
 * Created on 2022/4/1.
 *
 * @author lan
 */
public class DicAddRequest extends ActionRequest {
    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    private String[] id;

    private String dicCode;

    private String[] items;

    public String[] getId() {
        return id;
    }

    public DicAddRequest setId(String[] id) {
        this.id = id;
        return this;
    }

    public String getDicCode() {
        return dicCode;
    }

    public DicAddRequest setDicCode(String dicCode) {
        this.dicCode = dicCode;
        return this;
    }

    public String[] getItems() {
        return items;
    }

    public DicAddRequest setItems(String[] items) {
        this.items = items;
        return this;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        id = in.readStringArray();
        dicCode = in.readString();
        items = in.readStringArray();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeStringArray(id);
        out.writeString(dicCode);
        out.writeStringArray(items);
    }
}
