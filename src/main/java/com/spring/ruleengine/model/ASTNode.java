package com.spring.ruleengine.model;

import java.util.ArrayList;
import java.util.List;

public class ASTNode {
    private String type; // "operator" for AND/OR, "operand" for condition
    private List<ASTNode> children; // List of child nodes (for operators)
    private String value; // for operands (e.g., "age > 30")

    public ASTNode(String type) {
        this.type = type;
        this.children = new ArrayList<>();
    }

    public ASTNode(String type, String value) {
        this.type = type;
        this.value = value;
        this.children = new ArrayList<>();
    }

    public void addChild(ASTNode child) {
        this.children.add(child);
    }

    public String getType() {
        return type;
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    public String getValue() {
        return value;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
