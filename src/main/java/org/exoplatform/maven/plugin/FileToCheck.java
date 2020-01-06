package org.exoplatform.maven.plugin;

import difflib.Patch;

public class FileToCheck {

    String groupId;
    String artifactId;
    String path;
    String type;

    private Patch patch;

    @Override
    public String toString() {
        return "FileToCheck{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", type='" + type + '\'' +
                ", path='" + path + '\'' +
                '}';
    }

    public Patch getPatch() {
        return patch;
    }

    public void setPatch(Patch patch) {
        this.patch = patch;
    }
}
