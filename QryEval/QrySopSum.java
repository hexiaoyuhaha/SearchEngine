import java.io.IOException;

/**
 * Created by hexiaoyu on 2/19/17.
 */
public class QrySopSum extends QrySop {

    // Calculate scores only for documents that contains a query term
    @Override
    public boolean docIteratorHasMatch(RetrievalModel r) {
        return this.docIteratorHasMatchMin (r);
    }



    // Get a score for the document that docIteratorHasMatch matched.
    @Override
    public double getScore(RetrievalModel r) throws IOException {
        if (r instanceof RetrievalModelBM25) {
            return this.getScoreKM25 (r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SUM operator.");
        }
    }


    private double getScoreKM25 (RetrievalModel r) {

    }
}
