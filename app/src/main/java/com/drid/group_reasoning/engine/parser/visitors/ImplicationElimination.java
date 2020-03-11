package com.drid.group_reasoning.engine.parser.visitors;


import com.drid.group_reasoning.engine.parser.ast.ComplexSentence;
import com.drid.group_reasoning.engine.parser.ast.Connective;
import com.drid.group_reasoning.engine.parser.ast.Sentence;

public class ImplicationElimination extends SimplificationVisitor {

    @Override
    public Sentence visitBinarySentence(ComplexSentence node) {
        Sentence result;
        if (node.isImplication()) {
            Sentence a = node.getSentenceA().accept(this);
            Sentence b = node.getSentenceB().accept(this);
            Sentence notA = new ComplexSentence(a, Connective.NOT);
            result = new ComplexSentence(notA, b, Connective.OR);

        } else {
            result = super.visitBinarySentence(node);

        }
        return result;
    }

}
