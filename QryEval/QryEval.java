/*
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.1.2.
 */
import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *  This software illustrates the architecture for the portion of a
 *  search engine that evaluates queries.  It is a guide for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 */
public class QryEval {

  //  --------------- Constants and variables ---------------------

  private static final String USAGE =
    "Usage:  java QryEval paramFile\n\n";

  private static final String[] TEXT_FIELDS =
    { "body", "title", "url", "inlink" };

  private static String trecEvalOutputPath;
  private static int trecEvalOutputLength = 100;

  //  --------------- Methods ---------------------------------------

  /**
   *  @param args The only argument is the parameter file name.
   *  @throws Exception Error accessing the Lucene index.
   */
  public static void main(String[] args) throws Exception {

    //  This is a timer that you may find useful.  It is used here to
    //  time how long the entire program takes, but you can move it
    //  around to time specific parts of your code.
    
    Timer timer = new Timer();
    timer.start ();

    //  Check that a parameter file is included, and that the required
    //  parameters are present.  Just store the parameters.  They get
    //  processed later during initialization of different system
    //  components.

    if (args.length < 1) {
      throw new IllegalArgumentException (USAGE);
    }

    Map<String, String> parameters = readParameterFile (args[0]);

    // 2017.2.2 add
    if (parameters.containsKey("trecEvalOutputLength")) {
      trecEvalOutputLength = Integer.parseInt(parameters.get("trecEvalOutputLength"));
    }
    trecEvalOutputPath = parameters.get ("trecEvalOutputPath");
    clearOutputPath(trecEvalOutputPath);

    //  Open the index and initialize the retrieval model.

    Idx.open (parameters.get ("indexPath"));
    RetrievalModel model = initializeRetrievalModel (parameters);

    //  Perform experiments.
    
    processQueryFile(parameters.get("queryFilePath"), model);

    //  Clean up.
    
    timer.stop ();
    System.out.println ("Time:  " + timer);
  }

  public static void clearOutputPath(String trecEvalOutputPath) throws FileNotFoundException {
    File f = new File(trecEvalOutputPath);
    if (f.exists() && !f.isDirectory()) {
      PrintWriter writer = new PrintWriter(f);
      writer.print("");
      writer.close();
    }
  }

  /**
   *  Allocate the retrieval model and initialize it using parameters
   *  from the parameter file.
   *  @return The initialized retrieval model
   *  @throws IOException Error accessing the Lucene index.
   */
  private static RetrievalModel initializeRetrievalModel (Map<String, String> parameters)
    throws IOException {

    RetrievalModel model = null;
    String modelString = parameters.get ("retrievalAlgorithm").toLowerCase();

    if (modelString.equals("unrankedboolean")) {
      model = new RetrievalModelUnrankedBoolean();
    } else if (modelString.equals("rankedboolean")) {
      model = new RetrievalModelRankedBoolean();
    } else {
      throw new IllegalArgumentException
        ("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
    }
      
    return model;
  }

  /**
   * Print a message indicating the amount of memory used. The caller can
   * indicate whether garbage collection should be performed, which slows the
   * program but reduces memory usage.
   * 
   * @param gc
   *          If true, run the garbage collector before reporting.
   */
  public static void printMemoryUsage(boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc)
      runtime.gc();

    System.out.println("Memory used:  "
        + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
  }

  /**
   * //     This 3 should be treated as default queries
   //     brooks.title brothers.title clearance
   //     #AND(brooks.title brothers.title) clearance
   //     #AND(brooks brothers) #AND(like india)
   */
  public static Boolean needDefaultOperator(String qString) throws IllegalArgumentException {

    if (!qString.startsWith("#") || !qString.trim().endsWith(")")) {
      return true;
    } else {
      int firstLeftIdx = qString.indexOf("(");
      int pairRightIdx = -1;
      int count = 1;
      for (int i = firstLeftIdx + 1; i < qString.length(); i++) {
        if (qString.charAt(i) == '(') count++;
        if (qString.charAt(i) == ')') count--;
        if (count == 0) {
          pairRightIdx = i;
          break;
        }
      }
      if (pairRightIdx == -1) throw new IllegalArgumentException("Syntax Error: Missing closing paranthesis");
      if (pairRightIdx != qString.length() - 1) return true;
    }
    return false;
  }

  /**
   * Process one query.
   * @param qString A string that contains a query.
   * @param model The retrieval model determines how matching and scoring is done.
   * @return Search results
   * @throws IOException Error accessing the index
   */
  static ScoreList processQuery(String qString, RetrievalModel model)
    throws IOException {

    if (needDefaultOperator(qString)) {
      // Add "#or" for default parameter, qString: "forearm pain"
      String defaultOp = model.defaultQrySopName ();
      qString = defaultOp + "(" + qString + ")";
    }


    // automaticlly add
    Qry q = QryParser.getQuery (qString);

    // Show the query that is evaluated
    
    System.out.println("    --> " + q);
    
    if (q != null) {

      ScoreList r = new ScoreList ();
      
      if (q.args.size () > 0) {		// Ignore empty queries

        q.initialize (model); //creat all inverted list

        while (q.docIteratorHasMatch (model)) {
          int docid = q.docIteratorGetMatch ();
          double score;
          if (q instanceof QryIopNear) {
            if (model instanceof RetrievalModelUnrankedBoolean) {
              score = ((QryIopNear) q).getCurrentTf() > 0 ? 1 : 0;
            } else {
              score = ((QryIopNear) q).getCurrentTf();
            }
          } else {
            score = ((QrySop) q).getScore (model);
          }
          r.add (docid, score);
          q.docIteratorAdvancePast (docid);
        }
      }

      return r;
    } else
      return null;
  }

  /**
   *  Process the query file.
   *  @param queryFilePath
   *  @param model
   *  @throws IOException Error accessing the Lucene index.
   */
  static void processQueryFile(String queryFilePath,
                               RetrievalModel model)
      throws IOException {

    BufferedReader input = null;

    try {
      String qLine = null;
      input = new BufferedReader(new FileReader(queryFilePath));
      //  Each pass of the loop processes one query.

      while ((qLine = input.readLine()) != null) {
        int d = qLine.indexOf(':');

        if (d < 0) {
          throw new IllegalArgumentException
            ("Syntax error:  Missing ':' in query line.");
        }

        printMemoryUsage(false);

        String qid = qLine.substring(0, d);
        String query = qLine.substring(d + 1);

        System.out.println("Query " + qLine);

        ScoreList r = null;

        r = processQuery(query, model);

        if (r != null) {
//          printResults(qid, r);
          writeResultToFile(qid, r);
          System.out.println();
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      input.close();
    }
  }

  /**
   * Print the query results.
   * 
   * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
   * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param qid
   *          Original query.
   * @param result
   *          A list of document ids and scores
   * @throws IOException Error accessing the Lucene index.
   */
  static void printResults(String qid, ScoreList result) throws IOException {

    System.out.println(qid + ":  ");
    if (result.size() < 1) {
      System.out.println("\tNo results.");
    } else {
      if (result.size() <= 20) {
        for (int i = 0; i < result.size(); i++) {
          System.out.println("\t" + i + ":  " + Idx.getExternalDocid(result.getDocid(i)) + ", "
                  + result.getDocidScore(i));
        }
      } else {
        for (int i = 0; i < 5; i++) {
          System.out.println("\t" + i + ":  " + Idx.getExternalDocid(result.getDocid(i)) + ", " + result.getDocidScore(i));
        }
      }

    }
  }

  static void writeResultToFile(String qid, ScoreList result) throws IOException {
    String runId = "run-1";

    // Sort the ScoreList
    result.sort();
    System.out.println("write" + qid + " to file");

    try (BufferedWriter bw = new BufferedWriter(new FileWriter(trecEvalOutputPath, true));) {

      if (result.size() < 1) {
        String writeLine = String.format("%d\t%s\t%s\t%d\t%d\t%s", 10, "Q0", "dummy", 1, 0, runId);
        bw.write(writeLine);
      } else {
        int outputLen = min(trecEvalOutputLength, result.size());
//        int outputLen = result.size();
        for (int i = 0; i < outputLen; i++) {
          String formattedLine = String.format("%s\t%s\t%s\t%d\t%s\t%s\n", qid, "Q0", Idx.getExternalDocid(result.getDocid(i)), i, result.getDocidScore(i), runId);
          bw.write(formattedLine);
          if (i < 5) {
            System.out.print(formattedLine);
          }
        }
      }

    }

  }

  private static int min(int a, int b) {return a <= b? a: b;}

  /**
   *  Read the specified parameter file, and confirm that the required
   *  parameters are present.  The parameters are returned in a
   *  HashMap.  The caller (or its minions) are responsible for processing
   *  them.
   *  @return The parameters, in <key, value> format.
   */
  private static Map<String, String> readParameterFile (String parameterFileName)
    throws IOException {

    Map<String, String> parameters = new HashMap<String, String>();

    File parameterFile = new File (parameterFileName);

    if (! parameterFile.canRead ()) {
      throw new IllegalArgumentException
        ("Can't read " + parameterFileName);
    }

    Scanner scan = new Scanner(parameterFile);
    String line = null;
    do {
      line = scan.nextLine();
//      System.out.println(line);
      String[] pair = line.split ("=");
      parameters.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());

    scan.close();

    if (! (parameters.containsKey ("indexPath") &&
           parameters.containsKey ("queryFilePath") &&
           parameters.containsKey ("trecEvalOutputPath") &&
           parameters.containsKey ("retrievalAlgorithm"))) {
      throw new IllegalArgumentException
        ("Required parameters were missing from the parameter file.");
    }

    return parameters;
  }

}
