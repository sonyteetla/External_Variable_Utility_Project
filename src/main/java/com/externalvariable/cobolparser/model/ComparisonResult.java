package com.externalvariable.cobolparser.model;

public class ComparisonResult {

    private String variable;
    private String issue;
    private String file1Value;
    private String file2Value;

    public String getVariable() { return variable; }
    public void setVariable(String variable) { this.variable = variable; }

    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }

    public String getFile1Value() { return file1Value; }
    public void setFile1Value(String file1Value) { this.file1Value = file1Value; }

    public String getFile2Value() { return file2Value; }
    public void setFile2Value(String file2Value) { this.file2Value = file2Value; }
}