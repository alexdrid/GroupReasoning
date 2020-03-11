package com.drid.group_reasoning.engine.parser.ast;

import com.drid.group_reasoning.engine.parser.visitors.LogicVisitor;

public interface SentenceNode {
     <T> T accept(LogicVisitor<T> visitor);
}
