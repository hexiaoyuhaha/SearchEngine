import java.io.*;
import java.util.*;

/**
 * Created by hexiaoyu on 4/16/17.
 */

public class QryEvalDiversityUtil {

    /**
     * read relevance-based document rankings for query q, q_i from the the diversity:initialRankingFile file;
     * @throws Exception
     */
    public static void readInitialRankingFile(String initialRankingFile, int maxInputRankingsLength,
                                              String requiredOrigQid, ScoreList origQidScore,
                                              Map<String, ScoreList> intentScore) throws Exception {
        // 10 0 clueweb09-en0000-01-21462 1
        try (BufferedReader br = new BufferedReader(new FileReader(initialRankingFile))) {
            String line = null;
            while ((line = br.readLine()) != null) {

                if (line.startsWith(requiredOrigQid) == false) {
                    continue;
                }

                String[] ar = line.split("\\s+");
                assert ar.length == 6;
                String qid = ar[0];
                int docId = Idx.getInternalDocid(ar[2]);
                double score = (double) Double.parseDouble(ar[4]);

                if (qid.equals(requiredOrigQid)) {
                    origQidScore.add(docId, score);
                }
            }
        }

        origQidScore.sort();
        origQidScore.truncate(maxInputRankingsLength);
        Set<Integer> topQids = new HashSet<>();
        for (int i = 0; i < origQidScore.size(); i++) {
            topQids.add(origQidScore.getDocid(i));
        }

        try (BufferedReader br = new BufferedReader(new FileReader(initialRankingFile))) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(requiredOrigQid) == false) {
                    continue;
                }

                String[] ar = line.split("\\s+");
                String qid = ar[0];
                int docId = Idx.getInternalDocid(ar[2]);
                double score = (double) Double.parseDouble(ar[4]);

                if (qid.contains(".") && topQids.contains(docId)) {
                    if (intentScore.containsKey(qid) == false) {
                        intentScore.put(qid, new ScoreList());
                    }
                    intentScore.get(qid).add(docId, score);
                }
            }
        }
    }



    public static void writeToScaledFile(String header, Map<Integer, double[]> docScoresMap, String outputFile, double[] scoreSum) throws IOException {
        try (BufferedWriter fvbr = new BufferedWriter(new FileWriter(outputFile))) {
            // sort the data by origina intent score
            LinkedHashMap<Integer, double[]> sortedResult = sortByValue(docScoresMap);
            fvbr.write(header);
            fvbr.newLine();
            String line;
            for (Integer key: sortedResult.keySet()) {
                double[] nums = sortedResult.get(key);
                line = Idx.getExternalDocid(key);
                for (double num:nums) {
                    line += String.format(",%.9f", num);
                }
                fvbr.write(line);
                fvbr.newLine();
            }

            fvbr.write("TOTALS,");
            for (double num: scoreSum) {
                fvbr.write("," + num);
            }
        }
    }


    /**
    We only need the documents that ranks top maxInputRankingsLength
    */
    public static double[] normalizeScore(ScoreList origQidScore, Map<String, ScoreList> intentQidScoreMap) {
        double[] scoreSum = new double[intentQidScoreMap.size() + 1];
        double maxSumScore = 0, maxScore;
        maxScore = origQidScore.getDocidScore(0);
        for (ScoreList scoreList: intentQidScoreMap.values()) {
            scoreList.sort();
            maxScore = max(maxScore, scoreList.getDocidScore(0));
        }

        origQidScore.sort();
        scoreSum[0] = sum(origQidScore);
        maxSumScore = max(maxSumScore, scoreSum[0]);

        int j = 1;
        for (ScoreList scoreList: intentQidScoreMap.values()) {
            scoreSum[j] = sum(scoreList);
            maxSumScore = max(maxSumScore, scoreSum[j]);
            j++;
        }

        if (maxScore <= 1) {
            return scoreSum;
        }

        for (int i = 0; i < origQidScore.size(); i++) {
            origQidScore.setDocidScore(i, origQidScore.getDocidScore(i) / maxSumScore);
        }
        for (ScoreList scoreList: intentQidScoreMap.values()) {
            for (int i = 0; i < scoreList.size(); i++) {
                scoreList.setDocidScore(i, scoreList.getDocidScore(i) / maxSumScore);
            }
        }

        return scoreSum;
    }



    public static LinkedHashMap<Integer, double[]> sortByValue(Map<Integer, double[]> map) {

        List<Map.Entry<Integer, double[]>> list = new LinkedList<Map.Entry<Integer, double[]>>( map.entrySet() );
        Collections.sort(list, new Comparator<Map.Entry<Integer, double[]>>() {
            public int compare( Map.Entry<Integer, double[]> o1, Map.Entry<Integer, double[]> o2 ) {
                return ((Double)o2.getValue()[0]).compareTo(o1.getValue()[0]);
            }
        } );

        LinkedHashMap<Integer, double[]> result = new LinkedHashMap<Integer, double[]>();
        for (Map.Entry<Integer, double[]> entry : list) {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }


    public static double max(double a, double b) {
        return a > b? a:b;
    }

    public static double sum(ScoreList scoreList) {
        double sum = 0;
        for (int i = 0; i < scoreList.size(); i++) {
            sum += scoreList.getDocidScore(i);
        }
        return sum;
    }

    public static void setZero(double[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = 0.0;
        }
    }

    public static int argmax(double[] a) {
        double max = -Double.MAX_VALUE;
        int argmax = -1;
        for (int i = 0; i < a.length; i++) {
            if (a[i] > max) {
                max = a[i];
                argmax = i;
            }
        }
        return argmax;
    }

    public static double arraysum(double[] a) {
        double sum = 0;
        for (double num: a) {
            sum += num;
        }
        return sum;
    }
}

