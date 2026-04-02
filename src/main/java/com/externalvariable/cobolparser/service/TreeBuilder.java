package com.externalvariable.cobolparser.service;

import com.externalvariable.cobolparser.model.CobolVariable;
import java.util.*;

public class TreeBuilder {

    public CobolVariable buildTree(List<CobolVariable> vars) {

        Stack<CobolVariable> stack = new Stack<>();
        CobolVariable root = null;

        for (CobolVariable var : vars) {

            while (!stack.isEmpty() && stack.peek().getLevel() >= var.getLevel()) {
                stack.pop();
            }

            if (!stack.isEmpty()) {
                stack.peek().getChildren().add(var);
            } else {
                root = var;
            }

            stack.push(var);
        }

        return root;
    }
}