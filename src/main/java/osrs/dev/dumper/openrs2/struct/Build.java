package osrs.dev.dumper.openrs2.struct;

import com.google.gson.annotations.Expose;
import lombok.Data;

@Data
public class Build
{
    @Expose
    private int major;
    @Expose
    private Integer minor;
}