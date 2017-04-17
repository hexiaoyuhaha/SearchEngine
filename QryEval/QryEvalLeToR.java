import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;

import java.io.*;
import java.util.*;

/**
 * Created by hexiaoyu on 4/2/17.
 */
public class QryEvalLeToR {
    final String[] fields = new String[]{"body", "title", "url", "inlink"};

    String trainingQueryFile, trainingQrelsFile, trainingFeatureVectorsFile, pageRankFile,
            svmRankLearnPath, svmRankClassifyPath, svmRankParamC, svmRankModelFile,
            testingFeatureVectorsFile, testingDocumentScores, trecEvalOutputPath;
    Set<Integer> featureDisable;
    String queryFilePath;
    RetrievalModelBM25 bm25;
    RetrievalModelIndri indri;
    int mu;
    double lambda, k_1, b, k_3;
    QryEvalLeToRHelper helper = new QryEvalLeToRHelper();


    // key: external docid, value: pagerank score
    Map<String, Double> pageRankScoreMap = new HashMap<>();

    // key: qid, value: {key: external docid, value: relevance score}
    Map<String, Map<String, Double>> qrelsMap = new HashMap<>();

    // key: fields
    Map<String, Double> fieldAvgDocLenMap = new HashMap<>();


    Map<QryIopTerm, Double> indriDefault;
    Map<QryIopTerm, Double> indriCMLE;

    public void clearIndriValues() {
        indriDefault = new HashMap<>();
        indriCMLE = new HashMap<>();
    }

    public QryEvalLeToR(Map<String, String> parameters) throws Exception {
        trainingQueryFile = parameters.get("letor:trainingQueryFile");
        trainingQrelsFile = parameters.get("letor:trainingQrelsFile");
        trainingFeatureVectorsFile = parameters.get("letor:trainingFeatureVectorsFile");
        pageRankFile = parameters.get("letor:pageRankFile");
        svmRankLearnPath = parameters.get("letor:svmRankLearnPath");
        svmRankClassifyPath = parameters.get("letor:svmRankClassifyPath");
        svmRankParamC = parameters.get("letor:svmRankParamC");
        svmRankModelFile = parameters.get("letor:svmRankModelFile");
        testingFeatureVectorsFile = parameters.get("letor:testingFeatureVectorsFile");
        testingDocumentScores = parameters.get("letor:testingDocumentScores");
        trecEvalOutputPath = parameters.get("trecEvalOutputPath");


        // Read featureDisable if have
        if (parameters.containsKey("letor:featureDisable")) {
            featureDisable = new HashSet<>();
            String[] temp = parameters.get("letor:featureDisable").trim().split(",");
            for (int i = 0; i < temp.length; i++) {
                if (temp[i].length() != 0) {
                    featureDisable.add(Integer.parseInt(temp[i]));
                }
            }
        }

        // Read pageRank Score
        // clueweb09-en0011-58-31570	6.984868
        try (BufferedReader br = new BufferedReader(new FileReader(pageRankFile))) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() != 0) {
                    String[] ar = line.split("\t");
                    assert ar.length == 2;
                    pageRankScoreMap.put(ar[0], (double) Float.parseFloat(ar[1]));
                }
            }
        }

        // Read relevance Score
        // 10 0 clueweb09-en0000-01-21462 1
        try (BufferedReader br = new BufferedReader(new FileReader(trainingQrelsFile))) {
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] ar = line.split(" ");
                String qid = ar[0];
                String externalDocid = ar[2];
                double score = (double) Integer.parseInt(ar[3]);
                assert ar.length == 4;
                if (qrelsMap.containsKey(qid) == false) {
                    qrelsMap.put(qid, new HashMap<String, Double>());
                }
                qrelsMap.get(qid).put(externalDocid, score);
            }
        }

        // Compute average document length
        for (String field: fields) {
            double avg_doclen = Idx.getSumOfFieldLengths(field) / (double) Idx.getDocCount(field);
            fieldAvgDocLenMap.put(field, avg_doclen);
        }

        queryFilePath = parameters.get("queryFilePath");
        k_1 = Double.parseDouble(parameters.get("BM25:k_1"));
        b = Double.parseDouble(parameters.get("BM25:b"));
        k_3 = Double.parseDouble(parameters.get("BM25:k_3"));
        mu = Integer.parseInt(parameters.get("Indri:mu"));
        lambda = Double.parseDouble(parameters.get("Indri:lambda"));
        bm25 = new RetrievalModelBM25(k_1, b, k_3);
        indri = new RetrievalModelIndri(mu, lambda);
    }


    public ScoreList processQueryFile() throws Exception {
        // generating training data
        writeTrainFeatureVec(trainingFeatureVectorsFile, trainingQueryFile);

        // train
        String[] commandLineParams = new String[]{svmRankLearnPath, "-c", svmRankParamC, trainingFeatureVectorsFile, svmRankModelFile};
        Helper.excuteCommandLine(commandLineParams);

        // generate testing data for top 100 documents in initial BM25 ranking
        writeTestFeatureVec(testingFeatureVectorsFile, queryFilePath);

        // predict testing features using SVM
        String[] cli2 = new String[]{svmRankClassifyPath, testingFeatureVectorsFile, svmRankModelFile, testingDocumentScores};
        Helper.excuteCommandLine(cli2);

        // re-rank test data
        helper.reRankTestData(trecEvalOutputPath, testingFeatureVectorsFile, testingDocumentScores);
        return null;
    }




    public void writeTrainFeatureVec(String featureVectorsFile, String queryFile) throws Exception {
        try (BufferedWriter fvbr = new BufferedWriter(new FileWriter(featureVectorsFile))) {
            try (BufferedReader input = new BufferedReader(new FileReader(queryFile))) {
                String qLine = null;
                while ((qLine = input.readLine()) != null) {
                    // Validat that qid is in the query
                    int d = qLine.indexOf(':');
                    if (d < 0)  throw new IllegalArgumentException("Syntax error:  Missing ':' in query line.");
                    String qid = qLine.substring(0, d);
                    String query = qLine.substring(d + 1);
                    String[] qTerms = QryParser.tokenizeString(query);

                    Map<String, QryIopTerm[]> fieldsIopTerms = getFieldsIopTerms(qTerms);
                    clearIndriValues();

                    // For each document, docid is subsequent numbers from 0 to IndexReader.maxDoc()-1.
                    // But we only need to iterate over those doc that is contained in query referal files
//                    for (int docid = 0; docid < Idx.getNumDocs(); docid++) {
                    // key: ExternalDocId, value: features, length 19, with 0th index target score
                    Map<Integer, double[]> totalFeatVec = new HashMap<>();
                    for (String exteralId: qrelsMap.get(qid).keySet()) {
                        int docid = -1;
                        try {
                            docid = Idx.getInternalDocid(exteralId);
                        } catch (Exception e) {
//                            System.out.println("Ignore: " + exteralId + "External id not found.");
                            continue;
                        }
                        double[] features = getFeatureStr(docid, qid, fieldsIopTerms, true);
                        totalFeatVec.put(docid, features);
                    }
                    // write the feature vector to output file
                    fvbr.write(helper.getNormalizedFeatureStr(qid, totalFeatVec, featureDisable));
                } // this query ends
            }
        }
    }



    public void writeTestFeatureVec(String featureVectorsFile, String queryFile) throws Exception{
        try (BufferedWriter fvbr = new BufferedWriter(new FileWriter(featureVectorsFile))) {
            try (BufferedReader input = new BufferedReader(new FileReader(queryFile))) {
                String qLine = null;
                while ((qLine = input.readLine()) != null) {
                    // Validat that qid is in the query
                    int d = qLine.indexOf(':');
                    if (d < 0) throw new IllegalArgumentException("Syntax error:  Missing ':' in query line.");
                    String qid = qLine.substring(0, d);
                    String query = qLine.substring(d + 1);
                    String[] qTerms = QryParser.tokenizeString(query);

                    Map<String, QryIopTerm[]> fieldsIopTerms = getFieldsIopTerms(qTerms);
                    clearIndriValues();

                    // initial BM25 ranking, get top 100 docid
                    ScoreList result = QryEval.processQuery(query, bm25);
                    result.sort();
                    assert result.size() >= 100;

                    // iterate over the documents, compute feature vector write featureVec to outputfile
                    // key: ExternalDocId, value: features, length 19, with 0th index target score
                    Map<Integer, double[]> totalFeatVec = new HashMap<>();
                    for (int i = 0; i < 100; i++) {
                        int docid = result.getDocid(i);
                        double[] features = getFeatureStr(docid, qid, fieldsIopTerms, false);
                        totalFeatVec.put(docid, features);
                    }
                    // write the feature vector to output file
                    fvbr.write(helper.getNormalizedFeatureStr(qid, totalFeatVec, featureDisable));
                } // this query ends
            }
        }
    }



    public Map<String, QryIopTerm[]> getFieldsIopTerms(String[] qTerms) throws IOException {
        Map<String, QryIopTerm[]> fieldsIopTerms = new HashMap<>();
        for (String field: fields) {
            QryIopTerm[] IopTerms = new QryIopTerm[qTerms.length];
            for (int i = 0; i < qTerms.length; i++) {
                IopTerms[i] = new QryIopTerm(qTerms[i], field);
                IopTerms[i].evaluate();
            }
            fieldsIopTerms.put(field, IopTerms);
        }
        return fieldsIopTerms;
    }


    public double[] getFeatureStr(int docid, String qid, Map<String, QryIopTerm[]> fieldsIopTerms, Boolean isTrain) throws Exception {
        String externalDocId = Idx.getExternalDocid(docid);
        //create an empty feature vector
        double[] features = new double[19];

        if (isTrain) features[0] = qrelsMap.get(qid).get(externalDocId);
        else features[0] = 0;
        int spamScore = Integer.parseInt (Idx.getAttribute ("score", docid));
        int urlDepth = getUrlDepth(docid);
        int wikiScore = getWikiScore(docid);

        double pageRankScore = 0.0;
        try {
            pageRankScore = pageRankScoreMap.get(externalDocId);
        } catch (Exception e) {
//            System.out.println("no pageRankScore for externalDocId: " + externalDocId);
            pageRankScore = Double.MAX_VALUE;
        }
        features[1] = spamScore;
        features[2] = urlDepth;
        features[3] = wikiScore;
        features[4] = pageRankScore;

        int i = 5;
        for (String field: fields) {
//                            f5: BM25 score for <q, dbody>. f6: Indri score for <q, dbody>. f7: Term overlap score for <q, dbody>.
            HashMap<String, Integer> stems = getDocumentVocab(field, docid);
            if (stems == null || stems.size() == 0) {
                features[i++] = Double.MAX_VALUE;
                features[i++] = Double.MAX_VALUE;
                features[i++] = Double.MAX_VALUE;

            } else {
                double overlap = calTermOverLapScore(fieldsIopTerms.get(field), field, docid, stems);
                // For the Indri retrieval model features, if a field does not match any term of a query, the score for the field is 0.
                if (overlap == 0) {
                    features[i++] = 0.0;
                    features[i++] = 0.0;
                } else {
                    features[i++] = calBM25Score(fieldsIopTerms.get(field), field, docid, stems);
                    features[i++] = calIndriScore(fieldsIopTerms.get(field), field, docid, stems);
                }
                features[i++] = overlap;
            }
        }

        assert i == 17;
        String curField = "body";
        HashMap<String, Integer> stems = getDocumentVocab(curField, docid);
        if (stems == null || stems.size() == 0) {
            features[17] = Double.MAX_VALUE;
            features[18] = Double.MAX_VALUE;
        } else {
            // if overlap of body field terms and query terms is zero
            if (features[6] == 0) {
                features[17] = 0;
                features[18] = 0;
            } else {
//                features[17] = 0;
//                features[18] = 0;
                features[17] = calRankedBooleanScore(fieldsIopTerms.get(curField), curField, docid, stems);
                features[18] = calVSMScore(fieldsIopTerms.get(curField), curField, docid, stems, features[17]);
            }
        }

        return features;
    }

    public double calVSMScore(QryIopTerm[] IopTerms, String field, int docid, HashMap<String, Integer> stems, double totalFreq) throws IOException {
        // compute the dot product
        // dot product is the sum of total frequency
        // divide the dot product
        double vsm = totalFreq / Math.sqrt(IopTerms.length) / Math.sqrt(stems.size());
        return vsm;
    }

    public double calRankedBooleanScore(QryIopTerm[] IopTerms, String field, int docid, HashMap<String, Integer> stems) throws IOException {
        if (stems == null) return 0.0;

        int count = 0;
        for (int j = 0; j < IopTerms.length; j++) {
            if (stems.containsKey(IopTerms[j].term)) {
                count += stems.get(IopTerms[j].term);
            }
        }
        return count;
    }


    public double calTermOverLapScore(QryIopTerm[] IopTerms, String field, int docid, HashMap<String, Integer> stems) throws IOException {
        if (stems == null) return 0.0;

        int count = 0, size = 0;
        for (int j = 0; j < IopTerms.length; j++) {
            if (Idx.getTotalTermFreq (field, IopTerms[j].term) != 0) {  //  IopTerms[j].invertedList.ctf
                size++;
            }
            if (stems.containsKey(IopTerms[j].term)) {
                count++;
            }
        }
        double score = count * 1.0 / size;
        return score;
    }


    public HashMap<String, Integer> getDocumentVocab(String field, int docid) throws IOException {
        Terms luceneTerms = Idx.INDEXREADER.getTermVector(docid, field);
        HashMap<String, Integer> stems = new HashMap<>();
        //  If Lucene doesn't have a term vector, our TermVector is empty.
        if (luceneTerms == ((Terms) null)) {
            return null;
        }

        //  Iterate through the terms
        TermsEnum ithTerm = luceneTerms.iterator(null);
        for (int i = 0; ithTerm.next() != null; i++) {
            stems.put(ithTerm.term().utf8ToString(), (int) ithTerm.totalTermFreq());
        }
        if (stems != null && stems.size() == 0) return null;
        return stems;
    }


    public double calIndriScore(QryIopTerm[] IopTerms, String field, int docid, HashMap<String, Integer> stems) throws IOException {
        double prob = 1;
        double docLen = Idx.getFieldLength(field, docid);

        for (QryIopTerm IopTerm: IopTerms) {
            double score;
            double cMLE = getIndriCMLE(IopTerm);
            if (cMLE == 0) {
                System.out.println("!!Indri score zero!!!" + IopTerm.term + " " + IopTerm.field);
                continue;
            }
            if (stems.containsKey(IopTerm.term)) {
                int tf = stems.get(IopTerm.term);
                score = 1. * (1 - lambda) * (tf + mu * cMLE) / (docLen + mu) + lambda * cMLE;
            } else {
                // If this documents does not contains current term
                // use default indri score
                if (indriDefault.containsKey(IopTerm)) {
                    score = indriDefault.get(IopTerm);
                } else {
                    score = 1. * (1 - lambda) * (mu * cMLE) / (docLen + mu) + lambda * cMLE;
                    indriDefault.put(IopTerm, score);
                }
            }
            prob *= Math.pow(score, 1. / IopTerms.length);
        }
        return prob;
    }

    public double getIndriCMLE(QryIopTerm IopTerm) throws IOException {
        double cMLE;
        if (indriCMLE.containsKey(IopTerm)) {
            cMLE = indriCMLE.get(IopTerm);
        } else {
//            cMLE = (double) IopTerm.getCtf() / Idx.getSumOfFieldLengths(IopTerm.field);
            double ctf = Idx.getTotalTermFreq (IopTerm.field, IopTerm.term);
            cMLE = ctf / Idx.getSumOfFieldLengths(IopTerm.field);
            indriCMLE.put(IopTerm, cMLE);
        }
        return cMLE;
    }




    public double calBM25Score(QryIopTerm[] IopTerms, String field, int docid, HashMap<String, Integer> stems) throws IOException {
        double sum = 0;
        // If the vocabulary for current field of cur document is empty, return 0
        if (stems == null) return sum;

        // iterate all the terms, compute the BM25 score and add them together
        for(QryIopTerm IopTerm: IopTerms) {
            assert IopTerm.field.equals(field);
            if (stems.containsKey(IopTerm.term)) {
                double doclen = Idx.getFieldLength(field, docid);
                double avg_doclen = fieldAvgDocLenMap.get(field);

                long N = Idx.getNumDocs();
                int df = IopTerm.getDf();
                double RSJ;
                if (N < 2 * df) RSJ = 0;
                else RSJ = Math.log((N - df + 0.5) / (df + 0.5));

                int tf = stems.get(IopTerm.term);
                double tfWeight = (double) tf / (tf + k_1 * ((1 - b) + b * doclen / avg_doclen));
                sum += RSJ * tfWeight;
            }
        }
        return sum;
    }


    /** Raw urls all start with "http://", just count the number of '/' after that prefix
      (including the trailing '/' like www.google.com/ which doesn't seem to mean more depth than www.google.com).
      In other words, simply count the number of '/' and deduct 2("http://" prefix).
     */
    public int getUrlDepth(int docid) throws Exception{
        String rawUrl = Idx.getAttribute ("rawUrl", docid);
        return rawUrl.length() - rawUrl.replace("/", "").length() - 2;
    }

    public int getWikiScore(int docid) throws Exception{
        String rawUrl = Idx.getAttribute ("rawUrl", docid);
        if (rawUrl.contains("wikipedia.org")) return 1;
        else return 0;
    }
}
