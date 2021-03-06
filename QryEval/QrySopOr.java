/**
 * Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopOr extends QrySop {

    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch(RetrievalModel r) {
        return this.docIteratorHasMatchMin(r);
    }

    /**
     *  Get a score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how list are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScore(RetrievalModel r) throws IOException {

        if (r instanceof RetrievalModelUnrankedBoolean) {
            return this.getScoreUnrankedBoolean(r);
        } else if (r instanceof RetrievalModelRankedBoolean) {
            return this.getScoreRankedBoolean(r);
        } else if (r instanceof RetrievalModelIndri) {
            return this.getScoreIndri((RetrievalModelIndri) r);
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


    /**
     *  getScore for the UnrankedBoolean retrieval model.
     *  @param r The retrieval model that determines how list are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    private double getScoreUnrankedBoolean(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            return 1.0;
        }
    }

    private double getScoreRankedBoolean(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            // Normal case, OR operator only return the score of sub Qry that have docIteratorGetMatch() == docId
            // But if more than one file are pointing at current docId, we need to return the max
            // Return the MAX function to combine the list from the query argument
            int docId = docIteratorGetMatch();
            Boolean flag = false;
            double max = 0;

            for (Qry q : this.args) {
                if (q.docIteratorHasMatchCache() && q.docIteratorGetMatch() == docId) {
                    flag = true;
                    double score = ((QrySop) q).getScore(r);
                    max = max >= score ? max : score;
                }
            }
            if (!flag) {
                System.out.println("ERROR!!!!!  Ranked OR didn't match anything");
            }
            return max;
        }
    }

    private double getScoreIndri(RetrievalModel r) throws IOException {
        double prob = 1;
        int docId = this.docIteratorGetMatch();
        for (Qry q : this.args) {
            double score;
            if (q.docIteratorGetMatch() == docId) {
                score = ((QrySop) q).getScore(r);
            } else {
                score = ((QrySop) q).getDefaultScore(r, docId);
            }
            prob *= 1 - score;
        }
        return 1 - prob;
    }



    private double getDefaultScoreIndri(RetrievalModel r, long docId) throws IOException {
        double prob = 1;
        for (Qry q : this.args) {
            double score = ((QrySop) q).getDefaultScore(r, docId);
            prob *= 1 - score;
        }
        return 1 - prob;
    }
}
