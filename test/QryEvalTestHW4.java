import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by hexiaoyu on 4/3/17.
 */
public class QryEvalTestHW4 {
    String BASE = "hw4"; ///Users/hexiaoyu/Desktop/11642 search/QueryEval/
    String INPUT_DIR = BASE + "/input";
    String TEST_DIR = BASE + "/test";
    String OUTPUT_DIR = BASE + "/output";
    String PARAM_DIR = BASE + "/param";


    @Test
    public void main() throws Exception {
//        test();
//        exp1();
//        exp2();
        exp3();
    }

    public void test() throws Exception{
        String[] args = new String[1];
        String param = "/HW4-test-11.param";
            String paramFilePath = PARAM_DIR + param;
            args[0] = paramFilePath;
            QryEval.main(args);

    }


    public void exp1() throws Exception{
        String[] args = new String[1];
        for (String param: new String[]{"/base-bm25.param", "/base-indri.param", "/base-indri-seq.param"}) {
            String paramFilePath = PARAM_DIR + param;
            args[0] = paramFilePath;
            QryEval.main(args);
        }
    }

    public void exp2() throws Exception{
        String[] args = new String[1];
        for (String param: new String[]{"/letor-IRFusion.param", "/letor-base.param", "/letor-content-based.param", "/letor-all.param"}) {
            String paramFilePath = PARAM_DIR + param;
            args[0] = paramFilePath;
            QryEval.main(args);
        }
    }

    public void exp3() throws Exception{
        String[] args = new String[1];
        for (String param: new String[]{"/comb2.param", "/comb3.param", "/comb4.param"}) { //"/comb1.param",
            String paramFilePath = PARAM_DIR + param;
            args[0] = paramFilePath;
            QryEval.main(args);
        }
    }


    public String getParamPath() {
        String paramFilePath = BASE + "/param" + "/test.param";
        String outputString = "indexPath=/Users/hexiaoyu/Desktop/11642 search/index\n" +
                "queryFilePath=" + TEST_DIR + "/HW4-train-0.qry\n" +
                "trecEvalOutputPath=" + OUTPUT_DIR + "/HW4-train-0.teIn\n" +
                "retrievalAlgorithm=letor\n" +
                "BM25:k_1=1.2\n" +
                "BM25:b=0.75\n" +
                "BM25:k_3=0\n" +
                "Indri:mu=2500\n" +
                "Indri:lambda=0.4\n" +
                "letor:trainingQrelsFile=" + TEST_DIR + "/HW4-train-0.qrels\n" +
                "letor:trainingQueryFile=" + TEST_DIR + "/HW4-train-0.qry\n" +
                "letor:trainingFeatureVectorsFile=" + OUTPUT_DIR + "/HW4-train-0.LeToRTrain\n" +
                "letor:pageRankFile=" + INPUT_DIR + "/PageRankInIndex\n" +
                "letor:svmRankLearnPath=" + INPUT_DIR + "/svm_rank_learn\n" +
                "letor:svmRankClassifyPath=" + INPUT_DIR + "/svm_rank_classify\n" +
                "letor:svmRankParamC=0.001\n" +
                "letor:svmRankModelFile=" + OUTPUT_DIR + "/HW4-train-0.Model\n" +
                "letor:testingFeatureVectorsFile=" + OUTPUT_DIR + "/HW4-train-0.LeToRTest\n" +
                "letor:testingDocumentScores=" + OUTPUT_DIR + "/HW4-train-0.DocScore";

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(paramFilePath)))) {
            bw.write(outputString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return paramFilePath;
    }

}