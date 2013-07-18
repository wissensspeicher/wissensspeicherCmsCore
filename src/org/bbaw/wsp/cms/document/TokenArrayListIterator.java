package org.bbaw.wsp.cms.document;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.lucene.search.spell.TermFreqIterator;

public class TokenArrayListIterator implements TermFreqIterator {
  private ArrayList<Token> tokens;
  private Iterator<Token> iterator;
  private Token current;

  public TokenArrayListIterator(ArrayList<Token> tokens) {
    this.tokens = tokens;
    iterator = tokens.iterator();
  }

  public boolean hasNext() {
    return iterator.hasNext();
  }

  public String next() {
    if (tokens.iterator().hasNext()) {
      current = iterator.next();
      return current.getTerm().text();
    }
    return null;
  }

  public void remove() {
    iterator.remove();
  }

  public float freq() {
    return (float) current.getFreq();
  }

}

