package com.drid.group_reasoning.engine.parser.visitors;

import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;
import com.drid.group_reasoning.engine.parser.ast.ComplexSentence;


public interface LogicVisitor<T> {
    T visitAtomicSentence(AtomicSentence atomicSentence);
    T visitUnarySentence(ComplexSentence unarySentence);
    T visitBinarySentence(ComplexSentence binarySentence);    
}
