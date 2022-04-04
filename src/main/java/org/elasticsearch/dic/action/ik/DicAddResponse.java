package org.elasticsearch.dic.action.ik;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * Created on 2022/4/1.
 *
 * @author lan
 */
public class DicAddResponse extends ActionResponse {

    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public DicAddResponse setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        success = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(success);
    }
}
