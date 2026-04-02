package com.externalvariable.cobolparser.model;

import java.util.*;

public class CobolVariable {

    private int level;
    private String name;
    private String pic;
    private String position;

    private List<CobolVariable> children = new ArrayList<>();

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPic() { return pic; }
    public void setPic(String pic) { this.pic = pic; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public List<CobolVariable> getChildren() { return children; }
}