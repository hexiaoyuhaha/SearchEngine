import org.junit.After;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Created by hexiaoyu on 3/17/17.
 */
public class QryEvalTestHW3 {
    String indexPath = "/Users/hexiaoyu/Desktop/11642 search/index";
    String defaultQueryFilePath = "hw3/query/Indri-Bow.qry.txt";
    String defaultParamFile = "hw3/param/parameterFile.txt";
    String fbInitialRankingFile = "hw3/initialRanking/Indri-Bow.teIn.txt";

    int fbDoc = 10;
    int fbTerms = 10;
    double fbMu = 0;
    double fbOrigWeight = 0.5;

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void main() throws Exception {
//        exp1();
//        exp2();
//        exp3();
        exp4();
//        exp5();
//        other();
//        test();
    }


    public void other() throws Exception {
        // pass the parameter file to main
        String[] args = new String[1];
        args[0] = defaultParamFile;
        QryEval.main(args);
    }

    public void exp1() throws Exception {
        /**
         * 1. Ranked Boolean AND results;
         2. Results from your Indri retrieval model using unstructured queries;
         3. Results from the reference system provided to you;
         4. Results from the Indri retrieval model using query expansion on your system's retrieval results.
         5. Results from the Indri retrieval model using query expansion on the results from the reference system.
         */
        String exp = "E1";
//
//        testNoExpansion(exp, "RankedBoolean");

        // pass the parameter file to main
        String[] args = new String[1];
        String[] slist = new String[]{"param-E1-RankedBoolean.txt"};
        //"param-E1-Indri-NoRef.txt" "param-E1-Indri-YRef.txt","param-Exp1-Indri-false-1000-0.4.txt", "param-Exp1-Indri-false-1000-0.7.txt","param-Exp1-Indri-false-2500-0.4.txt", "param-Exp1-Indri-false-2500-0.7.txt",
        for (String paramFilePath: slist) {
            args[0] = "hw3/param/" + paramFilePath;
            QryEval.main(args);
        }
    }


    public void exp2() throws Exception {
        // setting the fbDocs parameter value to 10, 20, 30, 40, 50, and 100
        String exp = "E2";
        int[] fbDocTestValue = new int[]{10, 20, 30, 40, 50, 100};
        for (int newfbDoc: fbDocTestValue) {
            testWithExpansion(exp, "Indri", newfbDoc, fbTerms, fbMu, fbOrigWeight, "Y", fbInitialRankingFile);

        }
    }

    public void exp3() throws Exception {
        // setting the fbDocs parameter value to 10, 20, 30, 40, 50, and 100
        String exp = "E3";
        int[] testValue = new int[]{5, 20, 30, 40, 50}; //
        for (int newfbTerms: testValue) {
            testWithExpansion(exp, "Indri", 40, newfbTerms, fbMu, fbOrigWeight, "Y", fbInitialRankingFile);

        }
    }


    public void exp4() throws Exception {
        // fbOrigWeight = 0.2, 0.4, 0.6, 0.8, and 1.0
        String exp = "E4";
        double[] testValue = new double[]{1.0}; //0, 0.2, 0.4, 0.6, 0.8,
        for (double newweight: testValue) {
            testWithExpansion(exp, "Indri", 40, 50, fbMu, newweight, "Y", fbInitialRankingFile);

        }
    }


    public void testWithExpansion(String exp, String algo, int fbDocs, int fbTerms, double fbMu, double fbOrigWeight,
                                  String ref, String fbInitialRankingFile) throws Exception {
        // generate a new parameter file
        String paramFilePath = generateParameterFile(defaultQueryFilePath, exp, algo, fbDocs, fbTerms, fbMu, fbOrigWeight, ref, fbInitialRankingFile);

        // pass the parameter file to main
        String[] args = new String[1];
        args[0] = paramFilePath;
        QryEval.main(args);
    }



    public String generateParameterFile(String queryFilePath, String exp, String algo,
                                        int fbDocs, int fbTerms, double fbMu, double fbOrigWeight,
                                        String ref, String fbInitialRankingFile) {
        String algoLower = algo.toLowerCase();

        String paramFilePath = String.format("hw3/param/param-%s-%s-%s-%d-%d-%.1f-%.1f.txt", exp, algo, ref, fbDocs, fbTerms, fbMu, fbOrigWeight);
        String trecEvalOutputPath = String.format("hw3/output/%s-%s-%s-%d-%d-%.1f-%.1f.teln", exp, algo, ref, fbDocs, fbTerms, fbMu, fbOrigWeight);
        String fbExpansionQueryFile = String.format("hw3/queryExpansion/%s-%s-%s-%d-%d-%.1f-%.1f.teln", exp, algo, ref, fbDocs, fbTerms, fbMu, fbOrigWeight);

        String outputString = String.format("indexPath=%s\n" +
                "retrievalAlgorithm=%s\n" +
                "queryFilePath=%s\n" +
                "trecEvalOutputPath=%s\n" +
                "Indri:mu=%d\n" +
                "Indri:lambda=%.1f\n", indexPath, algo, queryFilePath, trecEvalOutputPath, 2500, 0.4);


        outputString += String.format("fb=%s\n" +
                "fbDocs=%d\n" +
                "fbTerms=%d\n" +
                "fbMu=%.1f\n" +
                "fbOrigWeight=%.1f\n" +
                "fbInitialRankingFile=%s\n" +
                "fbExpansionQueryFile=%s", "true", fbDocs, fbTerms, fbMu, fbOrigWeight, fbInitialRankingFile, fbExpansionQueryFile);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(paramFilePath)))) {
            bw.write(outputString);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return paramFilePath;
    }




    public void exp5() throws Exception {
        int[] termTestValue = new int[]{10}; //20, 30
        int[] muTestValue = new int[]{500, 1500, 2500, 3500, 4500, 5500};

        for (int newfbTerms: termTestValue) {
            for (int newMu: muTestValue) {
                Exp5testWithExpansion(newfbTerms, newMu);

            }
        }

    }


    public void Exp5testWithExpansion(int fbTerms, int mu) throws Exception {
        // generate a new parameter file
        String paramFilePath = Exp5GenParam("E5", "Indri", 40, fbTerms, mu, 0.4);

        // pass the parameter file to main
        String[] args = new String[1];
        args[0] = paramFilePath;
        QryEval.main(args);
    }


    public String Exp5GenParam(String exp, String algo, int fbDocs, int fbTerms, int mu, double lambda) {
        String paramFilePath, trecEvalOutputPath;
        String algoLower = algo.toLowerCase();

        paramFilePath = String.format("hw3/param/param-%s-%s-%d-%d-%.1f.txt", exp, algo, fbTerms, mu, lambda);
        trecEvalOutputPath = String.format("hw3/output/%s-%s-%d-%d-%.1f.teln", exp, algo, fbTerms, mu, lambda);


        String outputString = String.format("indexPath=%s\n" +
                "retrievalAlgorithm=%s\n" +
                "queryFilePath=%s\n" +
                "trecEvalOutputPath=%s\n" +
                "Indri:mu=%d\n" +
                "Indri:lambda=%.1f\n", indexPath, algo, defaultQueryFilePath, trecEvalOutputPath, mu, lambda);


        outputString += String.format("fb=%s\n" +
                "fbDocs=%d\n" +
                "fbTerms=%d\n" +
                "fbMu=%.1f\n" +
                "fbOrigWeight=%.1f\n" +
                "fbInitialRankingFile=%s", "true", fbDocs, fbTerms, fbMu, fbOrigWeight, fbInitialRankingFile);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(paramFilePath)))) {
            bw.write(outputString);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return paramFilePath;
    }


    public void test() {
        String line = "#wand( 0.7 #and( korean language ) 0.2 #and( #near/1( korean language ) ) 0.1 #and( #window/8( korean language ) ) )\n";
//        String b = "penguins";

        Set<String> queryTermsSet = new HashSet<String>();
        String pattern = "\\b(?<!#)[a-zA-Z]+\\b";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(line);
        while (m.find( )) {
            queryTermsSet.add(m.group(0));
        }

        for(String word: queryTermsSet) {
            System.out.println(word);
        }
    }
}