import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by hexiaoyu on 4/3/17.
 */
public class QryEvalLeToRHelper {

    public void reRankTestData(String trecEvalOutputPath, String featureFile, String predictionFile) throws Exception{

        Map<String, ScoreList> qidResultMap = new HashMap<>();
        ArrayList<String> qids = new ArrayList<>();

        // read Test Result
        try (BufferedReader featbr = new BufferedReader(new FileReader(featureFile));
             BufferedReader predbr = new BufferedReader(new FileReader(predictionFile))) {
            //featLine   2 qid:1 1:1 2:1 3:0 4:0.2 5:0 # clueweb09-en0000-48-24794
            //predLine   1.42976468
            String featLine = null, predLine = null;
            while (((featLine = featbr.readLine()) != null) && ((predLine = predbr.readLine()) != null)) {
                String[] tmp = featLine.trim().split(" ");
                String qid = tmp[1].split(":")[1];
                String externalDocid = tmp[tmp.length - 1];
                int docid = Idx.getInternalDocid(externalDocid);
                double score = Double.parseDouble(predLine);

                if (!qidResultMap.containsKey(qid)) {
                    qidResultMap.put(qid, new ScoreList());
                    qids.add(qid);
                }
                qidResultMap.get(qid).add(docid, score);
            }
        }

        // write to TrecEval Output
        // The sort key is column 2 (ascending numeric order for the query id portion of the field).
        Collections.sort(qids);
        for (String qid: qids) {
            QryEval.writeResultToFile(trecEvalOutputPath, qid, qidResultMap.get(qid));
        }
    }


    public String getNormalizedFeatureStr(String qid, Map<Integer, double[]> totalFeatVec, Set<Integer> featureDisable) throws IOException {

        // Normalize the features
        // (val - min) / (max - min)

        int numFeat = 19;
        // store the min/max for each feature
        // init min/max
        double[] max = new double[numFeat];
        double[] min = new double[numFeat];
        for (int i = 0; i < numFeat; i++) {
            max[i] = -Double.MAX_VALUE;
            min[i] = Double.MAX_VALUE;
        }

        // get min/max
        for (Integer j: totalFeatVec.keySet()) {
            for (int i = 0; i < numFeat; i++) {
                if (totalFeatVec.get(j)[i] != Double.MAX_VALUE && totalFeatVec.get(j)[i] < min[i]) min[i] = totalFeatVec.get(j)[i];
                if (totalFeatVec.get(j)[i] != Double.MAX_VALUE && totalFeatVec.get(j)[i] > max[i]) max[i] = totalFeatVec.get(j)[i];
            }
        }

        double[] range = new double[numFeat];
        for (int i = 0; i < numFeat; i++) {
            range[i] = max[i] - min[i];
        }

        // Normalize score
        for (Integer j: totalFeatVec.keySet()) {
            // 0th is target value, don't normalize
            for (int i = 1; i < numFeat; i++) {
                // If the min and max are the same value, set the feature value to 0.
                if (range[i] == 0 || totalFeatVec.get(j)[i] == Double.MAX_VALUE) {
                    totalFeatVec.get(j)[i] = 0;
                } else {
                    totalFeatVec.get(j)[i] = (totalFeatVec.get(j)[i] - min[i]) / range[i];
                }
            }
        }

        // output the normalized score

        // 2 qid:1 1:1 2:1 3:0 4:0.2 5:0 # clueweb09-en0000-48-24794
        // <target value> qid:<qid> <feature>:<value> <feature>:<value> ... <feature>:<value> # externalDocid
        // Note: feature number starts from 1, while array index starts from 0
        StringBuilder sb = new StringBuilder();
        for (int docid: totalFeatVec.keySet()) {
            double[] features = totalFeatVec.get(docid);
            sb.append(features[0]).append(" qid:").append(qid).append(" ");
            for (int i = 1; i < features.length; i++) {
                if (featureDisable != null && featureDisable.contains(i)) {
                    sb.append(i).append(":").append("0.0").append(" ");
                } else {
                    sb.append(i).append(":").append(String.valueOf(features[i])).append(" ");
                }
            }
            sb.append("# ").append(Idx.getExternalDocid(docid)).append("\n");
        }
        return sb.toString();
    }
}
