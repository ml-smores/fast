package edu.berkeley.nlp.util.optionparser;

import edu.berkeley.nlp.util.Logger;
import fig.basic.IOUtils;
import fig.exec.Execution;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: aria42
 * Date: Dec 8, 2008
 */
public abstract class Experiment {

  public static class ExperOpts {
    @Opt
    public boolean appendDate = false;

    private String _resultPoolDir;
    private String _resultDir = null;

    @Opt
    public void setResultDir(String resultDir) {
      _resultDir = new File(resultDir).getPath();
      File f = new File(resultDir);
      if (!f.exists()) {
        f.mkdirs();
      }
    }

    @Opt
    public void setResultPoolDir(String resultPoolDir) {
      setResultPoolDir(resultPoolDir,appendDate);
    }

    public void setResultPoolDir(String resultPoolDir, boolean appendDate) {
      _resultPoolDir = resultPoolDir;
      createExecDir(appendDate);
    }

    private void createExecDir(boolean appendDate) {
      File rootDir = new File(_resultPoolDir);

      if (appendDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
        Calendar c = Calendar.getInstance();
        String dateStr = sdf.format(c.getTime());
        rootDir = new File(rootDir, dateStr);
      }

      if (!rootDir.exists()) {
        rootDir.mkdirs();
      }      

      File[] files = rootDir.listFiles();
      int lastNum = 0;
      for (File file : files) {
        String fname = file.getName();
        Matcher matcher = Pattern.compile("(\\d+).exec").matcher(fname);
        if (matcher.matches()) {
         int num = Integer.parseInt(matcher.group(1));
         if (num >= lastNum) lastNum = num+1;
        }
      }
      File toCreate = new File(rootDir,"" + lastNum + ".exec");
      toCreate.mkdir();
      _resultDir = toCreate.getPath();
    }
              
  }

  public static ExperOpts experOpts = new ExperOpts();

  public static void setResultDir(String resultDir) {
    experOpts.setResultDir(resultDir);
  }

  public static String getResultDir() {
    return experOpts._resultDir;
  }

  public static void setResultPoolDir(String resultPoolDir, boolean appendDate) {
    experOpts.setResultPoolDir(resultPoolDir,appendDate);
  }

  /**
   *
   * @param args
   * @param experiment
   * @param useFig
   * @param ignoreUnknownFigOpts
   * @param prereqObjs Objects to Fill Options in Before tha min
   * experiment
   */
  public static void run(String[] args,
                         Runnable experiment,
                         boolean useFig,
                         boolean ignoreUnknownFigOpts,
                         Object...prereqObjs)
  {
    if (useFig) {
      Execution.ignoreUnknownOpts = ignoreUnknownFigOpts;
      Logger.setFig();
      Execution.init(args,experiment);
      experOpts._resultDir = Execution.getVirtualExecDir();
    }  

    // Register Args
    GlobalOptionParser.registerArgs(args,experiment.getClass());

    // Init Prereq Objs
    for (Object prereqObj : prereqObjs) {
      GlobalOptionParser.fillOptions(prereqObj);
    }    

    // Ensure Global Experiment Opts    
    GlobalOptionParser.fillOptions(experOpts);
    Logger.logs("Results: [%s]",getResultDir());
    // GlobalOptionParser.logOptions();

    // Write Conf Files
    writeConfFiles(args);

    // Write Logs
    writeLogs();

    if (!useFig) {
      Logger.startTrack("Starting Experiment: %s",experiment.getClass());
    }
                
    // Fill Experiment Opts
    GlobalOptionParser.fillOptions(experiment);

    // Run Experiment
    experiment.run();

    // Remind where reuslts are
    Logger.logs("Results: [%s]",getResultDir());

    if (useFig) Execution.finish();
    if (!useFig) Logger.endTrack();


  }

  public static void writeConfFiles(String[] args) {
    if (experOpts._resultDir != null) {
      List<File> confFiles = GlobalOptionParser.getConfFiles(args);
      for (File confFile : confFiles) {
        try {
          FileReader reader = new FileReader(confFile);
          FileWriter writer = new FileWriter(new File(experOpts._resultDir,confFile.getName()));
          IOUtils.copy(reader,writer);
          reader.close();
          writer.close();
        }
        catch (Exception e) {

        }
      }
    }
  }

  public static void writeLogs() {
    String outFile = experOpts._resultDir != null ?
        new File(experOpts._resultDir,"out.log").getAbsolutePath() :
        "out.log";
    String errFile = experOpts._resultDir != null ?
        new File(experOpts._resultDir,"err.log").getAbsolutePath() :
        "err.log";
    try {
      PrintStream outStr = new PrintStream(outFile,"UTF8");
      PrintStream errStr = new PrintStream(errFile,"UTF8");
      Logger.LogInterface gl = Logger.getGlobalLogger();
      Logger.SystemLogger fileLogger = new Logger.SystemLogger(outStr,errStr);
      Logger.setGlobalLogger(new Logger.CompoundLogger(gl,fileLogger));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
