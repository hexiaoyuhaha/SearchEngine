import java.io.*;
import java.util.*;

/**
 * Created by hexiaoyu on 4/16/17.
 */
public class QryEvalDiversity {
/**
    read a query from a file;
    if (the diversity= parameter is missing from the parameter file or set to false) {
        use the query to retrieve documents;
    } else {
        if (the diversity:initialRankingFile= parameter is specified) {
            read relevance-based document rankings for query q from the the diversity:initialRankingFile file;
            read relevance-based document rankings for query intents q.i from the diversity:initialRankingFile file;
        } else {
            read query q from the query file
            use query q to retrieve documents;
            for each of query q's intents
            read intent qi from the diversity:intentsFile file;
            use query qi to retrieve documents;
        }
        use the diversity:algorithm (Lecture 20) to produce a diversified ranking;
    }
    write the retrieval results to a file in trec_eval input format;
 */

    String queryFilePath, intentsFile, algorithm, trecEvalOutputPath;
    int maxInputRankingsLength, maxResultRankingLength;
    double lambda;

    // original query id -> corresponding query intents id
    // 157 -> {157.1, 157.2, 157.3, .....}
    Map<String, Set<String>> qintentsSet = new HashMap<>();

    // query id (including query intents id)  -> query line
    // 157 -> 157:the beatles rock band
    // 157.1 -> 157.1: listing beatles songs
    Map<String, String> queryMap = new HashMap<>();


    // Initial ranking model
    RetrievalModel model;

    Boolean initialRankingFileFlag = false;
    String initialRankingFile;

    Boolean WRITE_inRanks = false;
    Boolean WRITE_scaled = false;

    public QryEvalDiversity(Map<String, String> parameters) throws Exception {
        queryFilePath = parameters.get("queryFilePath");
        intentsFile = parameters.get("diversity:intentsFile");
        algorithm = parameters.get("diversity:algorithm").toLowerCase().trim();
        trecEvalOutputPath = parameters.get("trecEvalOutputPath");
        maxInputRankingsLength = Integer.parseInt(parameters.get("diversity:maxInputRankingsLength"));
        maxResultRankingLength = Integer.parseInt(parameters.get("diversity:maxResultRankingLength"));
        lambda = Double.parseDouble(parameters.get("diversity:lambda"));

        read_qintentsSet_queryLineMap();

        if (parameters.containsKey("diversity:initialRankingFile")) {
            // read relevance-based document rankings for query q, q_i from the the diversity:initialRankingFile file;
            initialRankingFileFlag = true;
            initialRankingFile = parameters.get("diversity:initialRankingFile");
        } else {
            // initial ranking mode
            if (model == null) {
                model = QryEval.initializeRetrievalModel(parameters);
            }
        }
    }

    public void go() throws Exception {
        String inRankPath = trecEvalOutputPath + ".inRanks";
        QryEval.clearOutputPath(trecEvalOutputPath + ".scaled");
        QryEval.clearOutputPath(inRankPath);

        for (String origQid: qintentsSet.keySet()) {
            ScoreList origQidScore = new ScoreList();
            Map<String, ScoreList> intentQidScoreMap = new TreeMap<>();

            // get document rankings
            if (initialRankingFileFlag) {
                // read relevance-based document rankings for query q, q_i from the the diversity:initialRankingFile file;
                QryEvalDiversityUtil.readInitialRankingFile(initialRankingFile, maxInputRankingsLength, origQid, origQidScore, intentQidScoreMap);
            } else {
                // self ranking
                origQidScore = compRanking(origQid, intentQidScoreMap, inRankPath);
            }


            int numOfIntents = intentQidScoreMap.size();
            String[] qids = new String[numOfIntents + 1];
            ScoreList[] scoreLists = new ScoreList[numOfIntents + 1];
            Map<Integer, double[]> rowWiseDocScores = new HashMap<>();


            String header = "docid," + origQid;
            qids[0] = origQid;
            scoreLists[0] = origQidScore;
            int index = 1;
            for (String qid: intentQidScoreMap.keySet()) {
                header += ',' + qid;
                qids[index] = qid;
                scoreLists[index] = intentQidScoreMap.get(qid);
                index++;
            }


            // normalize the rankings
            double[] scoreSum = QryEvalDiversityUtil.normalizeScore(origQidScore, intentQidScoreMap);

            // Store the data per document
            for (int j = 0; j < scoreLists.length; j++) {
                updateRowWiseDocScore(rowWiseDocScores, scoreLists[j], j, scoreLists.length);
            }

            // Write the result to output scaled file
            if (WRITE_scaled) {
                QryEvalDiversityUtil.writeToScaledFile(header, rowWiseDocScores, trecEvalOutputPath + ".scaled", scoreSum);
            }


            // Diversified ranking
            ScoreList result = QryEvalDiversityAlgo.diversifiedRanking(rowWiseDocScores, numOfIntents, maxResultRankingLength, algorithm, lambda);
            result.sort();
            result.truncate(maxResultRankingLength);
            if (result != null) {
                System.out.println("Writing final output to " + trecEvalOutputPath);
                QryEval.writeResultToFile(trecEvalOutputPath, origQid, result);
                System.out.println();
            }
        }
    }





    public void updateRowWiseDocScore(Map<Integer, double[]> rowWiseDocScores, ScoreList scoreList, int posToAdd, int size) {
        for (int i = 0; i < scoreList.size(); i++) {
            int docId = scoreList.getDocid(i);
            if (!rowWiseDocScores.containsKey(docId)) {
                rowWiseDocScores.put(docId, new double[size]); // including the original query
            }
            double score = scoreList.getDocidScore(i);
            rowWiseDocScores.get(docId)[posToAdd] = score;
        }
    }





    public void read_qintentsSet_queryLineMap() throws IOException {

        try (BufferedReader input = new BufferedReader(new FileReader(queryFilePath))) {
            String qLine = null;
            while ((qLine = input.readLine()) != null) {
                // Validat that qid is in the query
                int d = qLine.indexOf(':');
                if (d < 0) throw new IllegalArgumentException("Syntax error:  Missing ':' in query line.");
                String qid = qLine.substring(0, d);
                String query = qLine.substring(d + 1);

                if (qintentsSet.containsKey(qid) == false) {   // original query id
                    qintentsSet.put(qid, new HashSet<>());
                }
                queryMap.put(qid, query);
            }
        }


        try (BufferedReader input = new BufferedReader(new FileReader(intentsFile))) {
            String qLine = null;
            while ((qLine = input.readLine()) != null) {
                // Validat that qid is in the query
                int d = qLine.indexOf(':');
                if (d < 0) throw new IllegalArgumentException("Syntax error:  Missing ':' in query line.");
                String qid = qLine.substring(0, d);
                String query = qLine.substring(d + 1);

                // sub query id, e.g. 157.2
                System.out.println("qid" + qid);
                String origQid = qid.split("\\.")[0];
                qintentsSet.get(origQid).add(qid);
                queryMap.put(qid, query);
            } // this query ends
        }
    }




    /**
     * retrieve documents of q, q_i using our own system
     *
         read query q from the query file
         use query q to retrieve documents;
         for each of query q's intents
         read intent qi from the diversity:intentsFile file;
         use query qi to retrieve documents;
     * @throws Exception
     */
    public ScoreList compRanking(String origQid, Map<String, ScoreList> intentScores, String path) throws IOException {
        ScoreList origQidScore = QryEval.processQuery(queryMap.get(origQid), model);
        origQidScore.sort();
        origQidScore.truncate(maxInputRankingsLength);
        if (WRITE_inRanks) QryEval.writeResultToFile(path, origQid, origQidScore);

        Set<Integer> topQids = new HashSet<>();
        for (int i = 0; i < origQidScore.size(); i++) {
            topQids.add(origQidScore.getDocid(i));
        }

        for (String intentQid: qintentsSet.get(origQid)) {
            ScoreList scoreList = QryEval.processQuery(queryMap.get(intentQid), model);
            scoreList.sort();
            scoreList.truncate(maxInputRankingsLength);
            if (WRITE_inRanks) QryEval.writeResultToFile(path, intentQid, scoreList);

            for (int i = 0; i < scoreList.size(); i++) {
                if (!topQids.contains(scoreList.getDocid(i))) {
                    scoreList.setDocidScore(i, 0.0);
                }
            }
            scoreList.removeZeroScoreEntry();
            intentScores.put(intentQid, scoreList);
        }

        return origQidScore;
    }
}


