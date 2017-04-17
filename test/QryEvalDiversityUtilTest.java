import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by hexiaoyu on 4/16/17.
 */
public class QryEvalDiversityUtilTest {
    @Test
    public void sortByValue() throws Exception {
        test();
    }

    public void test() throws Exception {
        String[] args = new String[1];
        args[0] = "hw5/param/test-1.param";
        QryEval.main(args);
    }
}