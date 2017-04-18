import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

/**
 * Created by hexiaoyu on 4/16/17.
 */
public class QryEvalTestHW5 {

    String paramFolderPath = "hw5/param/";

    @Test
    public void exp() throws Exception {
//        test();
//        exp1();
//        exp3();
        exp4();
    }

    /**
     * Experiment: Diversification baselines
     * Conduct an experiment that examines the effects of PM2 and xQuAD on Indri and BM25. Use the following parameter values:

     Indri: mu = 2500, lambda = 0.4
     BM25: b = 0.75, k1 = 1.2, k1=0.0
     diversity:maxInputRankingsLength = 100
     diversity:maxResultRankingLength = 50
     diversity:lambda = 0.5
     Use the P-IA@10, P-IA@20, and alpha-NDCG@20 diversification metrics to analyze the experimental results.
     */
    public void exp1() throws Exception {
        String[] args = new String[1];
        String[] names = {"base-bm25.param"};
        //"base-indri-pm2.param", "base-indri.param", "base-indri-x.param",, "base-bm25-x.param", "base-bm25-pm2.param"
        //
        for (String name: names) {
            args[0] = paramFolderPath + name;
            QryEval.main(args);
        }
    }


    /**
     * Experiment: The effect of lambda on PM2 and xQuAD
     * Conduct an experiment that examines the effect of the lambda parameter on PM2 and xQuAD when used with BM25 and Indri. Use the same parameter values used for the first two experiments, except for lambda. Test the following values for lambda: 0.0, 0.2, 0.4, 0.6, 0.8, and 1.0.

     Use the P-IA@10, P-IA@20, and alpha-NDCG@20 diversification metrics to analyze the experimental results. You may also consider the effects on relevace metrics, however it is not required.
     */
    public void exp3() throws Exception {
        String[] args = new String[1];

        // First, let's do indri experiments
        double[] lambdas = {0.0, 0.2, 0.4, 0.6, 0.8, 1.0};
        String[] algos = {"xQuAD", "PM2"};
        for (double lambda: lambdas) {
            for (String diversAlgo: algos) {
                String outputFileName = generateBM25Param(diversAlgo, lambda);
//                String outputFileName = generateIndriParam(diversAlgo, lambda);
                args[0] =  outputFileName;
                QryEval.main(args);
            }
        }
    }


    public String generateIndriParam(String diversAlgo, double lamda) throws IOException {
        String outputFilePath = paramFolderPath + String.format("exp3-indri-%s-%.1f.param", diversAlgo, lamda);
        String content = String.format("indexPath=/Users/hexiaoyu/Desktop/11642 search/index\n" +
                "queryFilePath=hw5/input/q.qry\n" +
                "trecEvalOutputPath=hw5/output/exp3-indri-%s-%.1f.teln\n" +
                "retrievalAlgorithm=Indri\n" +
                "Indri:mu=2500\n" +
                "Indri:lambda=0.4\n" +
                "diversity=true\n" +
                "diversity:initialRankingFile=hw5/input/base-indri.inRanks\n" +
                "diversity:maxInputRankingsLength=100\n" +
                "diversity:maxResultRankingLength=50\n" +
                "diversity:algorithm=%s\n" +
                "diversity:intentsFile=hw5/input/q.intents\n" +
                "diversity:lambda=%.1f", diversAlgo, lamda, diversAlgo, lamda);
        try (BufferedWriter fvbr = new BufferedWriter(new FileWriter(outputFilePath))) {
            fvbr.write(content);
        }
        System.out.println(outputFilePath);
        System.out.println(content);
        return outputFilePath;
    }


    public String generateBM25Param(String diversAlgo, double lamda) throws IOException {
        String outputFileName = paramFolderPath+ String.format("exp3-bm25-%s-%.1f.param", diversAlgo, lamda);
        String content = String.format("indexPath=/Users/hexiaoyu/Desktop/11642 search/index\n" +
                "queryFilePath=hw5/input/q.qry\n" +
                "trecEvalOutputPath=hw5/output/exp3-bm25-%s-%.1f.teln\n" +
                "retrievalAlgorithm=BM25\n" +
                "BM25:k_1=1.2\n" +
                "BM25:b=0.75\n" +
                "BM25:k_3=0\n" +
                "diversity=true\n" +
                "diversity:initialRankingFile=hw5/input/base-bm25.inRanks\n" +
                "diversity:maxInputRankingsLength=100\n" +
                "diversity:maxResultRankingLength=50\n" +
                "diversity:algorithm=%s\n" +
                "diversity:intentsFile=hw5/input/q.intents\n" +
                "diversity:lambda=%.1f", diversAlgo, lamda, diversAlgo, lamda);
        try (BufferedWriter fvbr = new BufferedWriter(new FileWriter(outputFileName))) {
            fvbr.write(content);
        }
        System.out.println(outputFileName);
        System.out.println(content);
        return outputFileName;
    }




    /**
     *
     Experiment: The effect of the re-ranking depth

     Conduct an experiment that examines the effect of the re-ranking depth parameters on PM2 and xQuAD when used with BM25 and Indri. Use the same parameter values used for the first two experiments, except for the following parameters.

     You may select a value of lambda based on the previous experiment.
     Test the following values for the re-ranking parameters: { (25 / 25), (50, 25), (100, 25), (100, 50), and (200, 100) }, where the values are (maxInputRankingsLength, and maxResultRankingLength), respectively.
     Use the P-IA@10, P-IA@20, and alpha-NDCG@20 diversification metrics to analyze the experimental results. You may also consider the effects on relevace metrics, however it is not required.
     */
    public void exp4() throws Exception {
        String[] args = new String[1];
        double lambda = 0.6;
//        { (25 / 25), (50, 25), (100, 25), (100, 50), and (200, 100) }
        Pair[] parirs = {new Pair(25, 25), new Pair(50, 25), new Pair(100, 25), new Pair(100, 50)};
        String[] algos = {"xQuAD", "PM2"};

//        for (String diversAlgo: algos) {
//            for (Pair pair: parirs) {
//                String outputFileName = exp4GenerateIndriParam(diversAlgo, lambda, pair.insize, pair.outsize);
//                args[0] =  outputFileName;
//                QryEval.main(args);
//            }
//        }
//
//        for (String diversAlgo: algos) {
//            for (Pair pair: parirs) {
//                String outputFileName = exp4GenerateBM25Param(diversAlgo, lambda, pair.insize, pair.outsize);
//                args[0] =  outputFileName;
//                QryEval.main(args);
//            }
//        }

        String[] fileNames = {"exp4-bm25-PM2-200-100-0.6.param", "exp4-bm25-xQuAD-200-100-0.6.param",
                "exp4-indri-PM2-200-100-0.6.param", "exp4-indri-xQuAD-200-100-0.6.param"};

        for (String name: fileNames) {
            args[0] = paramFolderPath + name;
            try (BufferedReader input = new BufferedReader(new FileReader(args[0]))) {
                String qLine = null;
                while ((qLine = input.readLine()) != null) {
                    System.out.println(qLine);
                }
            }
            QryEval.main(args);
        }
    }

    public class Pair {
        int insize;
        int outsize;

        public Pair(int a, int b) {
            insize = a;
            outsize = b;
        }
    }

    public String exp4GenerateIndriParam(String diversAlgo, double lamda,
                                         int maxInputRankingsLength, int maxResultRankingLength) throws IOException {
        String outputFilePath = paramFolderPath + String.format("exp4-indri-%s-%d-%d-%.1f.param", diversAlgo, maxInputRankingsLength, maxResultRankingLength, lamda);
        String content = String.format("indexPath=/Users/hexiaoyu/Desktop/11642 search/index\n" +
                "queryFilePath=hw5/input/q.qry\n" +
                "trecEvalOutputPath=hw5/output/exp4-indri-%s-%d-%d-%.1f.teln\n" +
                "retrievalAlgorithm=Indri\n" +
                "Indri:mu=2500\n" +
                "Indri:lambda=0.4\n" +
                "diversity=true\n" +
                "diversity:initialRankingFile=hw5/input/base-indri.inRanks\n" +
                "diversity:maxInputRankingsLength=%d\n" +
                "diversity:maxResultRankingLength=%d\n" +
                "diversity:algorithm=%s\n" +
                "diversity:intentsFile=hw5/input/q.intents\n" +
                "diversity:lambda=%.1f", diversAlgo, maxInputRankingsLength, maxResultRankingLength, lamda,
                maxInputRankingsLength, maxResultRankingLength, diversAlgo, lamda);
        try (BufferedWriter fvbr = new BufferedWriter(new FileWriter(outputFilePath))) {
            fvbr.write(content);
        }
        System.out.println(outputFilePath);
        System.out.println(content);
        return outputFilePath;
    }


    public String exp4GenerateBM25Param(String diversAlgo, double lamda,
                                         int maxInputRankingsLength, int maxResultRankingLength) throws IOException {
        String outputFileName = paramFolderPath + String.format("exp4-bm25-%s-%d-%d-%.1f.param", diversAlgo, maxInputRankingsLength, maxResultRankingLength, lamda);
        String content = String.format("indexPath=/Users/hexiaoyu/Desktop/11642 search/index\n" +
                "queryFilePath=hw5/input/q.qry\n" +
                "trecEvalOutputPath=hw5/output/exp4-bm25-%s-%d-%d-%.1f.teln\n" +
                "retrievalAlgorithm=BM25\n" +
                "BM25:k_1=1.2\n" +
                "BM25:b=0.75\n" +
                "BM25:k_3=0\n" +
                "diversity=true\n" +
                "diversity:initialRankingFile=hw5/input/base-bm25.inRanks\n" +
                "diversity:maxInputRankingsLength=%d\n" +
                "diversity:maxResultRankingLength=%d\n" +
                "diversity:algorithm=%s\n" +
                "diversity:intentsFile=hw5/input/q.intents\n" +
                "diversity:lambda=%.1f", diversAlgo, maxInputRankingsLength, maxResultRankingLength, lamda,
                maxInputRankingsLength, maxResultRankingLength, diversAlgo, lamda);
        try (BufferedWriter fvbr = new BufferedWriter(new FileWriter(outputFileName))) {
            fvbr.write(content);
        }
        System.out.println(outputFileName);
        System.out.println(content);
        return outputFileName;
    }





    public void test() throws Exception {
        String[] args = new String[1];


//        args[0] = "hw5/param/HW5-Train-1.param"; // PM2
//        QryEval.main(args);

//        args[0] = "hw5/param/HW5-Train-2.param"; // xQuAD
//        QryEval.main(args);
//
//
//        args[0] = "hw5/param/HW5-Train-2-1.param";  // xQuAD, without input initial file, self generate one.
//        QryEval.main(args);


        args[0] = "hw5/param/HW5-Train-23.param"; // PM2
        QryEval.main(args);
    }
}