import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by hexiaoyu on 4/2/17.
 */
public class Helper {
    public static void excuteCommandLine(String[] params) throws Exception {
        // runs svm_rank_learn from within Java to train the model
        // execPath is the location of the svm_rank_learn utility,
        // which is specified by letor:svmRankLearnPath in the parameter file.
        // FEAT_GEN.c is the value of the letor:c parameter.
        Process cmdProc = Runtime.getRuntime().exec(params);
//                new String[] { "hw4/svm_rank/svm_rank_learn", "-c", String.valueOf(0.001), "hw4/svm_rank/example3/train.dat", "hw4/svm_rank/example3/model"});

        // The stdout/stderr consuming code MUST be included.
        // It prevents the OS from running out of output buffer space and stalling.

        // consume stdout and print it out for debugging purposes
        BufferedReader stdoutReader = new BufferedReader(
                new InputStreamReader(cmdProc.getInputStream()));
        String line;
        while ((line = stdoutReader.readLine()) != null) {
            System.out.println(line);
        }
        // consume stderr and print it for debugging purposes
        BufferedReader stderrReader = new BufferedReader(
                new InputStreamReader(cmdProc.getErrorStream()));
        while ((line = stderrReader.readLine()) != null) {
            System.out.println(line);
        }

        // get the return value from the executable. 0 means success, non-zero
        // indicates a problem
        int retValue = cmdProc.waitFor();
        if (retValue != 0) {
            throw new Exception("SVM Rank crashed.");
        }
    }
}
