/** 
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.ArrayList;

/**
 *  The root class of all query operators that use a retrieval model
 *  to determine whether a query matches a document and to calculate a
 *  score for the document.  This class has two main purposes.  First, it
 *  allows query operators to easily recognize any nested query
 *  operator that returns a document scores (e.g., #AND (a #OR(b c)).
 *  Second, it is a place to store data structures and methods that are
 *  common to all query operators that calculate document scores.
 */
public abstract class QrySop extends Qry {

  /**
   *  Get a score for the document that docIteratorHasMatch matched.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public abstract double getScore (RetrievalModel r)
    throws IOException;

  /**
   *  Get a default score for the given docId
   *  @param r The retrieval model.
   *  @param docId used to calculate default score
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public abstract double getDefaultScore (RetrievalModel r, long docId)
          throws IOException;

  /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators.  If the query operator is of type QryIop, it
   *  is fully evaluated, and the results are stored in an internal
   *  inverted list that may be accessed via the internal iterator.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   */
  public void initialize(RetrievalModel r) throws IOException {
    for (Qry q_i: this.args) {
      q_i.initialize (r);
    }
  }


  public ArrayList<Double> arg_weights = new ArrayList<Double>();
  public double[] arg_weights_portion;

  /**
   * compute the weight portion of ith element
   * @param i
   * @return return w_i / w_sum
   */
  public double weightPortion(int i) {
    if (arg_weights_portion == null) {
      // initialize the weightSum and arg_weights_portion
      initWeightPortion();
    }
    return arg_weights_portion[i];
  }


  public void initWeightPortion() {
    double weightSum = 0;
    for (double weight: arg_weights) {
      weightSum += weight;
    }
    arg_weights_portion = new double[arg_weights.size()];
    for (int i = 0; i < arg_weights.size(); i++) {
      arg_weights_portion[i] = arg_weights.get(i) / weightSum;
    }
  }
}
