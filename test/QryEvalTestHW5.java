import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by hexiaoyu on 4/16/17.
 */
public class QryEvalTestHW5 {

    @Test
    public void sortByValue() throws Exception {
        test();
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