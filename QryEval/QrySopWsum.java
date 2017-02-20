import java.io.IOException;

/**
 * Created by hexiaoyu on 2/19/17.
 */
public class QrySopWsum extends QrySop {

    public double[] arg_weights;

    // Calculate scores only for documents that contains a query term
    @Override
    public boolean docIteratorHasMatch(RetrievalModel r) {
        return this.docIteratorHasMatchMin (r);
    }


    // Get a score for the document that docIteratorHasMatch matched.
    @Override
    public double getScore(RetrievalModel r) throws IOException {
        if (r instanceof RetrievalModelIndri) {
            return this.getScoreIndri (r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SUM operator.");
        }
    }

    public double getDefaultScore(RetrievalModel r, long docId) throws IOException {
        throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the getDefaultScore operator.");

    }

    private double getScoreIndri (RetrievalModel r) throws IOException {

    }
}
