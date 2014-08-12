-------------------------------------------------
FAST -- Feature-Aware Student knowledge Tracing
------------------------------------------------
latest update of readme: 08/12/2014

This is a readme for FAST's code usage, input file format and major output files. 

This code is only for research purposes not for commercial purposes. It is still under improvement. 

Please email to us if you want to keep in touch with the latest release.
We sincerely welcome you to contact Yun Huang (huangyun.ai@gmail.com), or José P.González-Brenes (josepablog@gmail.com) for problems in the code or cooperation.
We thank Taylor Berg-Kirkpatrick (tberg@cs.berkeley.edu) and Jean-Marc Francois (jahmm) for part of their codes that FAST is developed based on.



----------------------------------------------
Following is the readme for using the code.
----------------------------------------------

-- RUN FAST
* Go to ./bin/, type command: java hmmfeatures/Run ++../confs/fast.conf
* This includes training and testing.
* However, you should make sure you configure the options correctly and prepare the input files correctly before the code can run. See following details.

-- CONGIRUATION
* See the details of configuration options: by command "java hmmfeatures/Run -help" or by src/hmmfeatures/Opts.java file
* Configuration file:  confs/fast.conf
  We provided a sample configuration file with the major options. However, you could add other options into the file according to your need.
* Here are the basic options:

	* inDir: input files' directory
	* outDir: output prediction and evaluation and log files' directory
	* (execPoolDir: for outputing execution info.)
	* (allModelComparisonOutDir: for output evaluation file comparing different models.)
	
	* trainInFilePrefix: the prefix of training set file(s).
	* testInFilePrefix: the prefix of testing set file(s).
	* inFileSurfix: the file surfix of training and testing set file(s).
	* testSingleFile: If testSingleFile=true, then numFolds and numRuns should set to 1, and FAST trains on "train0.txt" and test on "test0.txt" (if trainInFilePrefix=train, testInFilePrefix=test).
	* numFolds, numRuns: they are used to decide how many times FAST runs and are related to how FAST retrieves train and test files. By default, numRuns is always set to 1.
		
		File is named by trainInFilePrefix(testInFilePrefix) + id + inFileSurfix. (id = current run id * numFolds + current fold id).
		For example, if numFolds=5, numRuns=1, then there should be 5 pairs of train and test files and should be named by train0.txt, train1.txt...train4.txt, and test0.txt, test1.txt ... test4.txt.
		
	* basicModelName: should be "FAST" or "KT"
	* modelName: could be any string you like

-- USE THE EVALUATION CODE
For successfully using the evaluation code within FAST, you should make sure the number of files ended with ".pred" surfix generated in the outDir correspond to the numFolds and numRuns that you specified (e.g. for numFolds=1, numFolds=2, there should be only two files in outDir ended with ".pred".



----------------------------------------------
Following is the readme for input file format.
----------------------------------------------


-- FILE NAMES:
Details are explained in the above CONGIRUATION.
By default, training file: train0.txt; testing file: test0.txt.
Currently, only support naming in the format of trainX.txt or testX.txt, X should be the integer number according to #folds and #runs (and X can not be empty).

-- ORDER OF THE RECORDS:
Input should already be ordered by time within one user. However, users' order doesn't matter. Same user same KC's records don't need to be put together as long as the old records of this user's this KC are put later.

-- RELATION BETWEEN TRAIN AND TEST FILES:
You shouldn't have new KCs in the test file. Because in training process, each KC correspond to one HMM and has its only set of parameters.

-- About features with string as values: 
Currently, FAST doesn't support string as feature values, and only support numerical variables. Hopefully in the future FAST could solve that.

-- About features that look like categorical variables:
Please make sure that categorical variables are coded by dummy. Currently FAST doesn't have inner mechanism to transfer categorical variables into dummies, so the input file takes care of that.

-- About features that have big values, or range widely:
Although FAST currently has L2-regularization, yet in order to make coefficients more directly interpretable, and also speed up the training, sometimes doing some standardization or normalization of such features to map them to smaller values may help. Yet sometimes standardization or normalization is not suitable due to the feature value distribution (etc.) and will drop the performance. Please do some experimentation.


-- student COLUMN:
You can put either integer or string here.

-- problem COLUMN:
You can put the problem/item/question name/id here. Integer or string are both ok.

-- step COLUMN:
It doesn't matter what you put here so far. However, it may help you to check the results if you use this column to put information identifying the order of the records.

-- outcome COLUMN:
You can only put "correct" or "incorrect" here.

-- fold COLUMN:
By default, all values are 1.
If you have the kind of data split where some records from a student-skill sequence is used for training and remaining used for testing, then in the test0.txt file, please put the records used for training with fold COLUMN value -1. See sample3.

-- KCs COLUMN:
KCs field is the skill current record requires (only support one KC one item/record) now, but you can put multiple KCs as features if you have multiple KCs per item/record, see sample2). All records within the same "KCs" value belong to one HMM and are trained together.

-- FEATURE COLUMNS:
By default, feature fields should have prefix features_.
	* If some features are always 0 (never appear) in current KC but may have value 1 in other KCs, then put them as NULL for current KC's records (This is for the sake of computing gradient, if they are NULL then the code doesn't compute gradient for those features for current HMM, see sample1~3).
	* If you have some features that you want to share by both "known" and "unknown" states (which means the feature value and coefficient is the same no matter a student mastered or not mastered a skill), then you can put *features_ as prefix. See sample2.

-- BIAS (INTERCEPT) FEATURE:
By default, FAST adds bias feature to both hidden states. Don't put bias(intercept) feature in the input (a feature always with value 1). If you want to change the configuration of bias features, please specify bias in configuration java class file (Opts.java). 



------------------------------------------------
Following is the readme for major output files.
------------------------------------------------

-- "XXX.pred" FILE
The three columns actualLabel,predLabel, predProb are: actual student responses(correct or incorrect), predicted student responses and predicted probability of getting correct responses.

-- "XXX.eval" FILE and "evaluation.log" FILE
"XXX.eval" file includes the current evaluation while "evaluation.log" maintains the log of each time's evaluation.
