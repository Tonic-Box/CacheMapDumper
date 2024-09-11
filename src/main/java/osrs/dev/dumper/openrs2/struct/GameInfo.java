package osrs.dev.dumper.openrs2.struct;

import com.google.gson.annotations.Expose;
import lombok.Data;

import java.util.List;

@Data
public class GameInfo {
    @Expose
    private int id;
    @Expose
    private String scope;
    @Expose
    private String game;
    @Expose
    private String environment;
    @Expose
    private String language;
    @Expose
    private List<Build> builds;
    @Expose
    private String timestamp;
    @Expose
    private List<String> sources;
    @Expose
    private int valid_indexes;
    @Expose
    private int indexes;
    @Expose
    private int valid_groups;
    @Expose
    private int groups;
    @Expose
    private int valid_keys;
    @Expose
    private int keys;
    @Expose
    private long size;
    @Expose
    private long blocks;
    @Expose
    private boolean disk_store_valid;
}