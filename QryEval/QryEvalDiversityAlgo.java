import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by hexiaoyu on 4/17/17.
 */
public class QryEvalDiversityAlgo {

    public static ScoreList diversifiedRanking(Map<Integer, double[]> rowWiseDocScores, int numOfIntents,
                                                 int maxResultRankingLength, String algorithm, double lambda) {
        algorithm = algorithm.toLowerCase().trim();
        if (algorithm.equals("pm2")) {
            return diversifiedRankingxPM2(rowWiseDocScores, numOfIntents, maxResultRankingLength, lambda);
        } else {
            return diversifiedRankingxQuAD(rowWiseDocScores, numOfIntents, maxResultRankingLength, lambda);
        }
    }


    public static ScoreList diversifiedRankingxPM2(Map<Integer, double[]> rowWiseDocScores, int numOfIntents,
                                                    int maxResultRankingLength, double lambda) {

        ScoreList result = new ScoreList();
        int diversifiedRankingSize = rowWiseDocScores.size();
        double v = diversifiedRankingSize * 1. / numOfIntents;
        double uniformIntentWeight = 1. / numOfIntents;
        Set<Integer> R = new HashSet<>(rowWiseDocScores.keySet());
        Set<Integer> S = new HashSet<>();

        double[] s = new double[numOfIntents];
        double[] qt = new double[numOfIntents];

        QryEvalDiversityUtil.setZero(s);

        while (result.size() < maxResultRankingLength) {
            // updata qt
            set_qt(qt, s, v);

            int i_star = QryEvalDiversityUtil.argmax(qt);
            // compute score for each remaining documents, only keep the max score
            int d_star = -1;
            double maxScore = -Double.MAX_VALUE;

            for (int docId: R) {
                double[] scores = rowWiseDocScores.get(docId);
                double relevanceScore = qt[i_star] * scores[i_star];
                double diversityScore = 0;
                for (int i = 0; i < numOfIntents; i++) {
                    if (i != i_star) {
                        diversityScore += qt[i] * scores[i];
                    }
                }
                double curScore =  lambda * relevanceScore + (1 - lambda) * diversityScore;
                if (curScore < 0) {
                    System.out.println("xxx");
                }
                if (curScore > maxScore) {
                    d_star = docId;
                    maxScore = curScore;
                }
            }
            // Update R, S, s[i]
            R.remove(d_star);
            S.add(d_star);
            result.add(d_star, maxScore);
            update_s(s, rowWiseDocScores.get(d_star));
        }
        return result;
    }


    public static void set_qt(double[] qt, double[] s, double v) {
        for (int i = 0; i < qt.length; i++) {
            qt[i] = v / (2 * s[i] + 1 );
        }
    }


    public static void update_s(double[] s, double[] scores) {
        double sum = QryEvalDiversityUtil.arraysum(scores);
        for (int i = 0; i < s.length; i++) {
            s[i] += scores[i] / sum;
        }
    }


    public static ScoreList diversifiedRankingxQuAD(Map<Integer, double[]> rowWiseDocScores, int numOfIntents,
                                                    int maxResultRankingLength, double lambda) {
        ScoreList result = new ScoreList();
        double uniformIntentWeight = 1. / numOfIntents;
        Set<Integer> R = new HashSet<>(rowWiseDocScores.keySet());
        Set<Integer> S = new HashSet<>();

        while (result.size() < maxResultRankingLength) {
            // compute score for each remaining documents, only keep the max score
            int d_star = -1;
            double maxScore = -Double.MAX_VALUE;

            for (int docId: R) {
                double[] scores = rowWiseDocScores.get(docId);
                // relevanceScore
                double relevanceScore = scores[0];

                // diversityScore
                double diversityScore = 0;
                // iterate on intents
                for (int i = 1; i < numOfIntents + 1; i++) {
                    double coverdScore = 1;
                    // iterate on selected docs
                    for (Integer d_j: S) {
                        coverdScore *= (1 - rowWiseDocScores.get(d_j)[i]);
                    }
                    diversityScore += uniformIntentWeight * scores[i] * coverdScore;
                }
                double curScore = (1 - lambda) * relevanceScore + lambda * diversityScore;
                if (curScore < 0) {
                    System.out.println("xxx");
                }
                if (curScore > maxScore) {
                    d_star = docId;
                    maxScore = curScore;
                }
            }

            // Update R, S
            R.remove(d_star);
            S.add(d_star);
            result.add(d_star, maxScore);
        }

        return result;
    }
}
