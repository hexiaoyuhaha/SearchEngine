/**
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopAnd extends QrySop {

    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch (RetrievalModel r) {
        if (r instanceof RetrievalModelIndri) {
            return this.docIteratorHasMatchMin(r);
        }
        return this.docIteratorHasMatchAll (r);
    }

    /**
     *  Get a score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScore (RetrievalModel r) throws IOException {

        if (r instanceof RetrievalModelUnrankedBoolean) {
            return this.getScoreUnrankedBoolean (r);
        } else if (r instanceof RetrievalModelRankedBoolean) {
            return this.getScoreRankedBoolean (r);
        } else if (r instanceof RetrievalModelIndri) {
            return this.getScoreIndri ((RetrievalModelIndri) r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the OR operator.");
        }
    }


    public double getDefaultScore(RetrievalModel r, long docId) throws IOException {
        if (r instanceof RetrievalModelIndri) {
            return getDefaultScoreIndri(r, docId);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the getDefaultScore operator.");
        }
    }



    private double getScoreIndri (RetrievalModel r) throws IOException {
        int size = this.args.size();
        double prob = 1;
        int docId = this.docIteratorGetMatch();
        for (Qry q: this.args) {
            double score;
            if (q.docIteratorHasMatchCache() && q.docIteratorGetMatch() == docId) {
                score = ((QrySop) q).getScore(r);
            } else {
                score = ((QrySop) q).getDefaultScore(r, docId);
            }
            prob *= Math.pow(score, 1. / size);
        }
        return prob;
    }


    private double getDefaultScoreIndri (RetrievalModel r, long docId) throws IOException {
        int size = this.args.size();
        double prob = 1;
        for (Qry q: this.args) {
            double score = ((QrySop) q).getDefaultScore(r, docId);
            prob *= Math.pow(score, 1. / size);
        }
        return prob;
    }


    /**
     *  getScore for the UnrankedBoolean retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
        if (! this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            return 1.0;
        }
    }

    private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
        if (! this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            // Return the Min function to combine the scores from the query argument
            Qry q_0 = this.args.get(0);
            double min = ((QrySop) q_0).getScore(r);
            for (int i=1; i<this.args.size(); i++) {
                Qry q_i = this.args.get(i);
                double score = ((QrySop) q_i).getScore(r);
                min = min <= score? min : score;
            }
            return min;
        }
    }
}
