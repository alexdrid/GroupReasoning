package com.drid.group_reasoning.engine.parser.ast;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class AtomicSentence extends Sentence implements Serializable,Parcelable {

    private String symbol;
    private String proposition;

    public AtomicSentence(String symbol) {
        this.symbol = symbol;
    }

    protected AtomicSentence(Parcel in) {
        symbol = in.readString();
        proposition = in.readString();
    }

    public static final Creator<AtomicSentence> CREATOR = new Creator<AtomicSentence>() {
        @Override
        public AtomicSentence createFromParcel(Parcel in) {
            return new AtomicSentence(in);
        }

        @Override
        public AtomicSentence[] newArray(int size) {
            return new AtomicSentence[size];
        }
    };

    public void setProposition(String proposition) {
        this.proposition = proposition;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public String getProposition() {
        return proposition;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(symbol);
        dest.writeString(proposition);
    }

    @Override
    public String toString() {
        return "{Atomic sentence: symbol=" + symbol + " , proposition=" +  proposition + "}";
    }


    @Override
    public Connective getConnective() {
        return null;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if ((o == null) || (this.getClass() != o.getClass())) {
            return false;
        }
        AtomicSentence atom = (AtomicSentence) o;
        return symbol.equals(atom.symbol) && proposition.equals(atom.proposition);

    }

    @Override
    public int hashCode() {
        return symbol.hashCode();
    }


}
