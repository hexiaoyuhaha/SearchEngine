import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by hexiaoyu on 4/17/17.
 */
public class QryEvalDiversityAlgoTest {
    /**
     * Make sure to uncomment the Idx.getExternalDocid part in ScoreList class before running the following test case.
     * @throws Exception
     */

    @Test
    public void test() throws Exception {
//        diversifiedRankingxPM2()
        diversifiedRankingxQuAD();
    }


    public void diversifiedRankingxPM2() throws Exception {
        Map<Integer, double[]> rowWiseDocScores = new HashMap<>();
        double[][] data = {
                {0.7, 0.7, .2},
                {0.69, 0.8, 0.1},
                {0.68, 0.6, 0.3},
                {0.67, 0.2, 0.7},
                {0.66, 0.3, 0.8}
        };
        for (int i = 0; i < data.length; i++) {
            rowWiseDocScores.put(i, data[i]);
        }
        ScoreList scoreList = QryEvalDiversityAlgo.diversifiedRankingPM2(rowWiseDocScores, 2, 8, 0.6);
    }


    public void diversifiedRankingxQuAD() throws Exception {
        Map<Integer, double[]> rowWiseDocScores = new HashMap<>();
        double[][] data = {
                {0.7, 0.7, .2},
                {0.69, 0.8, 0.1},
                {0.68, 0.6, 0.3},
                {0.67, 0.2, 0.7},
                {0.66, 0.3, 0.8}
        };
        for (int i = 0; i < data.length; i++) {
            rowWiseDocScores.put(i, data[i]);
        }
        ScoreList scoreList = QryEvalDiversityAlgo.diversifiedRankingxQuAD(rowWiseDocScores, 2, 8, 0.4);
        System.out.println(scoreList);
    }

}