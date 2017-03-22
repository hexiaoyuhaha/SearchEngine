/**
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

/**
 *  An object that stores parameters for the unranked Boolean
 *  retrieval model (there are none) and indicates to the query
 *  operators how the query should be evaluated.
 *
 *  fb=true
 fbDocs=10
 fbTerms=10
 fbMu=0
 fbOrigWeight=0.5
 fbInitialRankingFile=hw3/initialRanking/Indri-Bow.teIn.txt
 fbExpansionQueryFile=hw3/query/expension.txt

 */
public class RetrievalModelIndri extends RetrievalModel {

  private double mu, lambda;

  public double getMu() {
    return mu;
  }

  public double getLambda() {
    return lambda;
  }

  public RetrievalModelIndri(double mu, double lambda) {
    if (mu >= 0 && lambda >=0 && lambda <= 1) {
      this.mu = mu;
      this.lambda = lambda;
    } else {
      throw new IllegalArgumentException
              ("Required parameters were wrong for Indri model.");
    }

  }

  public String defaultQrySopName () {return "#and";};

}
