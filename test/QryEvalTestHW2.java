import org.junit.After;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by hexiaoyu on 2/20/17.
 * Assuming that parameter only only have maximu 3 digit
 */
public class QryEvalTestHW2 {

     String indexPath = "/Users/hexiaoyu/Desktop/11642 search/index";
     String defaultQueryFilePath = "input/queries.txt";
     String outputPath = "output/";


    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void main() throws Exception {
//        exp1();
//        exp2();
//        exp3();
//        exp4();
//        exp5();
        other();
    }

    public void other() throws Exception {
        String paramFilePath = generateParameterFile("input/queries-exp4-0.txt", "Exp4-0", "Indri", new double[]{2500, 0.4});

        // pass the parameter file to main
        String[] args = new String[1];
        args[0] = paramFilePath;
        QryEval.main(args);
    }

    public void exp1() throws Exception {
        String exp = "Exp1";
        testBoolean(exp);
        testBM25(exp, 1.2, 0.75, 0);
        testIndri(exp, 2500, 0.4);
    }


    public void exp2() throws Exception {
        String exp = "Exp2";
        for (double var = 0.6; var <= 2; var += 0.2) {
            testBM25(exp, var, 0.75, 0);
        }

        for (double var = 0.2; var <= 0.9; var += 0.1) {
            testBM25(exp, 1.2, var, 0);
        }

    }

    public void exp3() throws Exception {
        String exp = "Exp3";

        for (double var = 5000; var <= 10000; var += 1000) {
            testIndri(exp, var, 0.4);
        }
        for (double lam = 0.1; lam < 1; lam += 0.1) {
            testIndri(exp, 2500, lam);
        }
    }


    public void exp4() throws Exception {
        for (int i = 0; i <= 5; i++) {
            String paramFilePath = generateParameterFile("input/queries-exp4-" + i + ".txt", "Exp4-" + i, "Indri", new double[]{2500, 0.4});

            // pass the parameter file to main
            String[] args = new String[1];
            args[0] = paramFilePath;
            QryEval.main(args);
        }
    }


    public void exp5() throws Exception {
        for (int i = 0; i <= 5; i++) {
            String paramFilePath = generateParameterFile("input/queries-exp5-" + i + ".txt", "Exp5-" + i, "Indri", new double[]{2500, 0.4});

            // pass the parameter file to main
            String[] args = new String[1];
            args[0] = paramFilePath;
            QryEval.main(args);
        }
    }



    public void testBoolean(String exp) throws Exception {
        // generate a new parameter file
        String algo = "RankedBoolean";

        String paramFilePath = generateParameterFile(defaultQueryFilePath, exp, algo, null);

        // pass the parameter file to main
        String[] args = new String[1];
        args[0] = paramFilePath;
        QryEval.main(args);
    }

    public void testBM25(String exp, double k1, double b, double k3) throws Exception {
        // generate a new parameter file
        String algo = "BM25";

        String paramFilePath = generateParameterFile(defaultQueryFilePath, exp, algo, new double[]{k1, b, k3});

        // pass the parameter file to main
        String[] args = new String[1];
        args[0] = paramFilePath;
        QryEval.main(args);
    }

    public void testIndri(String exp, double mu, double lambda) throws Exception {
        // generate a new parameter file
        String algo = "Indri";

        String paramFilePath = generateParameterFile(defaultQueryFilePath, exp, algo, new double[]{mu, lambda});

        // pass the parameter file to main
        String[] args = new String[1];
        args[0] = paramFilePath;
        QryEval.main(args);
    }




    public String generateParameterFile(String queryFilePath, String exp, String algo, double[] nums) {
        String paramFilePath, trecEvalOutputPath;
        String algoLower = algo.toLowerCase();
        switch (algoLower) {
            case "indri":
                paramFilePath = String.format("input/parameterFile-%s-%s-%d-%.2f.txt", exp, algo, (int) nums[0], nums[1]);
                trecEvalOutputPath = String.format("output/HW2-%s-%s-%d-%.2f.teln", exp, algo, (int) nums[0], nums[1]);
                break;

            case "bm25":
                paramFilePath = String.format("input/parameterFile-%s-%s-%.2f-%.2f.txt", exp, algo, nums[0], nums[1]);
                trecEvalOutputPath = String.format("output/HW2-%s-%s-%.2f-%.2f.teln", exp, algo, nums[0], nums[1]);

//                only use k_1 and b for now
//                paramFilePath = String.format("input/parameterFile-%s-%s-%.2f-%.2f-%.2f.txt", exp, algo, nums[0], nums[1], nums[2]);
//                trecEvalOutputPath = String.format("output/HW2-%s-%s-%.2f-%.2f-%.2f.teln", exp, algo, nums[0], nums[1], nums[2]);
                break;

            default:
                paramFilePath = String.format("input/parameterFile-%s-%s.txt", exp, algo);
                trecEvalOutputPath = String.format("output/HW2-%s-%s.teln", exp, algo);
        }


        String outputString = String.format("indexPath=%s\n" +
                "retrievalAlgorithm=%s\n" +
                "queryFilePath=%s\n" +
                "trecEvalOutputPath=%s\n", indexPath, algo, queryFilePath, trecEvalOutputPath);

        switch (algoLower) {
            case "bm25":
                outputString += String.format(
                        "BM25:k_1=%f\n" +
                        "BM25:b=%f\n" +
                        "BM25:k_3=%f", nums[0], nums[1], nums[2]);;
                break;

            case "indri":
                outputString += String.format(
                        "Indri:mu=%f\nIndri:lambda=%f", nums[0], nums[1]);
                break;

            default:
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(paramFilePath)))) {
            bw.write(outputString);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return paramFilePath;
    }

}