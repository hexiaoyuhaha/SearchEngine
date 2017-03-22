import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hexiaoyu on 3/17/17.
 */
public class QryEvalQueryExpension {


    public static ScoreList processQueryWithQueryExpansion(String qid,
                                                           String query,
                                                           RetrievalModel model,
                                                           Map<String, String> parameters) throws Exception {
        // HW3 query expension
        ScoreList r = null;

        // No query expansion is needed
        if (!parameters.containsKey("fb")
                || parameters.get("fb").toLowerCase().equals("false")
                || parameters.get("fb").toLowerCase().equals("")) {
            System.out.println("-- No Expand Query");
            r = QryEval.processQuery(query, model);
        } else {
            System.out.println("-- Expand Query");
            // Assertion: Only indri have query expension
            assert model instanceof RetrievalModelIndri;

            // Validate Params
            int fbDocs = Integer.parseInt(parameters.get("fbDocs"));
            int fbTerms = Integer.parseInt(parameters.get("fbTerms"));
            double fbMu = Double.parseDouble(parameters.get("fbMu"));
            double fbOrigWeight = Double.parseDouble(parameters.get("fbOrigWeight"));
            if (!(fbDocs>0 && fbTerms>0 && fbMu>=0 && fbOrigWeight>=0 && fbOrigWeight <= 1)) {
                throw new IllegalArgumentException("Required parameters were wrong for Indri Query Expension model.");
            }

            // Retrieve Original document ranking
            ScoreList origResult = null;
            if (!parameters.containsKey("fbInitialRankingFile") || parameters.get("fbInitialRankingFile").equals("")) {
                // use the query to retrieve documents;
                origResult = QryEval.processQuery(query, model);
            } else {
                // use the fbInitialRankingFile to retrieve documents
                origResult = getRankingFromfbInitialRanking(qid, parameters.get("fbInitialRankingFile"));
            }

            // get all query terms from the original query
            Set<String> origQueryTermsSet = getQueryTermsSet(query);

            // produce an expanded query;
            String qexpanded = getExpanededQuery(origQueryTermsSet, origResult, fbDocs, fbTerms, fbMu);

            // write the expanded query to a file specified by the fbExpansionQueryFile=
            if (parameters.containsKey("fbExpansionQueryFile")) {
                writeToExpensionQueryFile(qid, qexpanded, parameters.get("fbExpansionQueryFile"));
            }

            // create a combined query as #wand (w qoriginal + (1-w) qexpandedquery);
            String qcombined = getCombineQuery(query, qexpanded, fbOrigWeight, model);
            System.out.println("qexpanded:" + qexpanded);
            System.out.println("qcombined:" + qcombined);


            // use the combined query to retrieve documents;
            r = QryEval.processQuery(qcombined, model);
        }

        return r;
    }


    public static Set<String> getQueryTermsSet(String query) {
//        String line = "#wand( 0.7 #and( korean language ) 0.2 #and( #near/1( korean language ) ) 0.1 #and( #window/8( korean language ) ) )\n";
//        String b = "penguins";

        Set<String> queryTermsSet = new HashSet<String>();
        String pattern = "\\b(?<!#)[a-zA-Z]+\\b";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(query);
        while (m.find( )) {
            queryTermsSet.add(m.group(0));
        }
//
//        for(String word: queryTermsSet) {
//            System.out.println(word);
//        }
        return queryTermsSet;
    }


    public static ScoreList getRankingFromfbInitialRanking(String qid, String fbInitialRankingFilePath) throws Exception {
        ScoreList scoreList = new ScoreList();
        BufferedReader bf = new BufferedReader(new FileReader(new File(fbInitialRankingFilePath)));
        String line;
        while ((line = bf.readLine()) != null) {
            if (line.startsWith(qid)) {
                String[] splitted = line.split("\\s+");
                // if qid match
                String externalId = splitted[2];
                int internalDocId = Idx.getInternalDocid(externalId);
                double score = Double.parseDouble(splitted[4]);
                scoreList.add(internalDocId, score);
            }
        }
        bf.close();
        return scoreList;
    }


    public static String getExpanededQuery(Set<String> origQueryTermsSet,
                                           ScoreList origResult,
                                           int fbDocs,
                                           int fbTerms,
                                           double fbMu) throws IOException {

        // Create TermVector for each document
        TermVector[] termVectors = new TermVector[fbDocs];
        double[] vectorScore = new double[fbDocs];
        origResult.sort();
//        origResult.truncate(fbDocs);
        for (int i = 0; i < Math.min(fbDocs, origResult.size()); i++) {
            termVectors[i] = new TermVector(origResult.getDocid(i), "body");
            vectorScore[i] = origResult.getDocidScore(i);
        }


        // Get all the terms -> iterate over documents
        Set<String> fbVocabulary = new HashSet<String>();
        for (int i = 0; i < Math.min(fbDocs, origResult.size()); i++) {
            fbVocabulary.addAll(Arrays.asList(termVectors[i].getStems()));
        }
        fbVocabulary.remove("");
        fbVocabulary.remove(null);
//        fbVocabulary.removeAll(origQueryTermsSet);

        // Compute term score -> Iterate over terms
        TermScoreList termScoreList = new TermScoreList(fbTerms);
        Iterator<String> iter = fbVocabulary.iterator();
        while (iter.hasNext()) {
            String term = iter.next();
            if (term.contains(".")) {
                iter.remove();
            } else {
                double termScore = calcTermScore(term, termVectors, fbMu, vectorScore);
                termScoreList.add(term, termScore);
            }
        }

        // Get top fbTerm
//        termScoreList.sort();
//        termScoreList.truncate(fbTerms);

        // Produce the expanded query
        // #wand (0.69 french 0.83 lick 0.76 indiana ...)
        StringBuilder sb = new StringBuilder();
        sb.append("#wand (");
        while (termScoreList.hasNext()) {
            Entry entry = termScoreList.getNext();
            sb.append(String.format("%.4f %s ", entry.score, entry.term));
        }
        sb.append(")");
        String expanededQuery = sb.toString();

        return expanededQuery;
    }



    static double calcTermScore(String term, TermVector[] termVectors, double fbMu, double[] vectorScore) throws IOException {
        if (term == null || term.length() == 0) return 0;

        // 1. P_MLE(t|C) = collection term frequency / collection total length
        InvList invList = new InvList(term, "body");
        int ctf = invList.ctf;
        long collectonLength = Idx.getSumOfFieldLengths("body");
        double cMLE =   1.0 * ctf / collectonLength;

        // 3. idf
        double idf = Math.log(1.0 * collectonLength / ctf);

        // 2. p(t|d)
        double sumscore = 0;
        // Summing up the prob of term given doc -> iterate over documents
        for (int i = 0; i < termVectors.length; i++) {
            TermVector vector = termVectors[i];

            // 2.1 p(t|d)
            int index = vector.indexOfStem(term);
            int tf_td = 0;
            if (index != -1) {
                tf_td = vector.stemFreq(index);
            }
            int doc_len = vector.positionsLength();
            double p_td = (tf_td + fbMu * cMLE) / (doc_len + fbMu);

            // 2.2 p(I|d)
            // get indri score for current doccument
            double indriScore = vectorScore[i];

            // SUM UP
            double curSore = p_td * indriScore * idf;
            sumscore += curSore;
        }

        return sumscore;
    }



    public static String getCombineQuery(String qorig, String qexpanded, double fbOrigWeight, RetrievalModel model) {
        if (QryEval.needDefaultOperator(qorig)) {
            // Add "#or" for default parameter, qString: "forearm pain"
            String defaultOp = model.defaultQrySopName();
            qorig = defaultOp + "(" + qorig + ")";
        }

        String qcombined = String.format("#wand( %f %s  %f %s )", fbOrigWeight, qorig, 1 - fbOrigWeight, qexpanded);
        return qcombined;
    }


    public static void writeToExpensionQueryFile(String qid, String qexpanded, String fbExpansionQueryFile) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fbExpansionQueryFile)))) {
            bw.write(qid + ":" + qexpanded);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
