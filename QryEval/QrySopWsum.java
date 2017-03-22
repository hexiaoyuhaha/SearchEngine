import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by hexiaoyu on 2/19/17.
 */
public class QrySopWsum extends QrySop {


    // Calculate list only for documents that contains a query term
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
        if (r instanceof RetrievalModelIndri) {
            return getDefaultScoreIndri(r, docId);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the getDefaultScore operator.");
        }
    }

    private double getScoreIndri (RetrievalModel r) throws IOException {
        int size = this.args.size();
        double sum = 0;
        int docId = this.docIteratorGetMatch();
        QrySop q;
        for (int i = 0; i < size; i++) {
            q = (QrySop) this.args.get(i);
            double score;
            if (q.docIteratorHasMatchCache() && q.docIteratorGetMatch() == docId) {
                score = q.getScore(r);
            } else {
                score = q.getDefaultScore(r, docId);
            }
            sum += weightPortion(i) * score;
        }
        return sum;
    }


    private double getDefaultScoreIndri (RetrievalModel r, long docId) throws IOException {
        int size = this.args.size();
        double sum = 0;
        QrySop q;
        for (int i = 0; i < size; i++) {
            q = (QrySop) this.args.get(i);
            double score = ((QrySop) q).getDefaultScore(r, docId);
            sum += weightPortion(i) * score;
        }
        return sum;
    }

}
