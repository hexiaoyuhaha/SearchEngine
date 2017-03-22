/**
 * Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.IllegalArgumentException;

/**
 *  The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

    /**
     *  Document-independent values that should be determined just once.
     *  Some retrieval models have these, some don't.
     */

    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch(RetrievalModel r) {
        return this.docIteratorHasMatchFirst(r);
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
        } else if (r instanceof RetrievalModelBM25) {
            return this.getScoreBM25((RetrievalModelBM25) r);
        } else if (r instanceof RetrievalModelIndri) {
            return this.getScoreIndri((RetrievalModelIndri) r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SCORE operator.");
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
     *  getScore for the Unranked retrieval model.
     *  @param r The retrieval model that determines how list are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScoreUnrankedBoolean(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            return 1.0;
        }
    }

    /**
     * Ranked, Asumming that QrySopScore only have single args: QryIopTerm
     * @param r The retrieval model that determines how list are calculated.
     * @return The document score
     * @throws IOException
     */
    public double getScoreRankedBoolean(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            int tf = ((QryIop) this.args.get(0)).getCurrentTf();
            return tf;
        }
    }

    public double getScoreBM25(RetrievalModelBM25 r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            long N = Idx.getNumDocs();
            int df = ((QryIop) this.args.get(0)).getDf();
            double RSJ;
            if (N < 2 * df) RSJ = 0;
            else RSJ = Math.log((N - df + 0.5) / (df + 0.5));

            QryIop term = ((QryIop) this.args.get(0));
            int tf = term.getCurrentTf();
            int docid = this.docIteratorGetMatch();
            double doclen = Idx.getFieldLength(term.field, docid);
            double avg_doclen = Idx.getSumOfFieldLengths(term.field) / (double) Idx.getDocCount(term.field);
            double k1 = r.getK_1(), b = r.getB();
            double tfWeight = (double) tf / (tf + k1 * ((1 - b) + b * doclen / avg_doclen));

            return RSJ * tfWeight;
        }
    }


    private double getScoreIndri(RetrievalModel r) throws IOException {

        int docId = this.docIteratorGetMatch();

        QryIop term = ((QryIop) this.args.get(0));
        double mu = ((RetrievalModelIndri) r).getMu();
        double lambda = ((RetrievalModelIndri) r).getLambda();
        if (cMLE == -1) {
            cMLE = (double) term.getCtf() / Idx.getSumOfFieldLengths(term.field);
        }
        double docLen = Idx.getFieldLength(term.field, docId);
        int tf = term.getCurrentTf();
        double score = (1 - lambda) * (tf + mu * cMLE) / (docLen + mu) + lambda * cMLE;
        return score;
    }


    private double cMLE = -1;

    // Now only support Indri
    public double getDefaultScoreIndri(RetrievalModel r, long docId) throws IOException {
        QryIop term = ((QryIop) this.args.get(0));
        double mu = ((RetrievalModelIndri) r).getMu();
        double lambda = ((RetrievalModelIndri) r).getLambda();
        if (cMLE == -1) {
            cMLE = (double) term.getCtf() / Idx.getSumOfFieldLengths(term.field);
        }
        double docLen = Idx.getFieldLength(term.field, (int) docId);
        double defaultScore = (1 - lambda) * mu * cMLE / (docLen + mu) + lambda * cMLE;
        return defaultScore;
    }



    /**
     *  Initialize the query operator (and its arguments), including any
     *  internal iterators.  If the query operator is of type QryIop, it
     *  is fully evaluated, and the results are stored in an internal
     *  inverted list that may be accessed via the internal iterator.
     *  @param r A retrieval model that guides initialization
     *  @throws IOException Error accessing the Lucene index.
     */
    public void initialize(RetrievalModel r) throws IOException {

        Qry q = this.args.get(0);
        q.initialize(r);
    }

}
