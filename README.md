-------------------------------------------------
FAST: Feature-Aware Student knowledge Tracing
------------------------------------------------

This is an implementation of FAST model (http://educationaldatamining.org/EDM2014/uploads/procs2014/long%20papers/84_EDM-2014-Full.pdf). This readme contains FAST's code usage, input file format and major output files. 

This code is only for RESEARCH purposes not for commercial purposes. It is still under development. 

Please email to us if you want to keep in touch with the latest release.
We sincerely welcome you to contact Yun Huang (huangyun.ai@gmail.com), or José P.González-Brenes (josepablog@gmail.com) for problems in the code or cooperation.
We thank Taylor Berg-Kirkpatrick (tberg@cs.berkeley.edu) and Jean-Marc Francois (jahmm) for part of their codes that FAST is developed based on.



---------
RUN FAST!
---------

* Open a terminal and go to the bin directory
* Type: java hmmfeatures/Run ++../confs/fast.conf

This should train and test FAST with sample data specified in the configuration file fast.conf, which defaults to using data from ../data/input/



-----------
INPUT FILES
-----------
FAST requires at least two files as input, a training and a testing. By default these are data/train0.txt and data/test0.txt
The input files should be delimited by tab(\t) or comma (,). 
Currently, only support naming in the format of prefix-X-surfix (e.g. train0.txt with prefix as "train", X as "0" and surfix as ".txt"). X should be the integer number according to #folds and #runs (and X can not be empty); the prefix can be specified by trainInFilePrefix or testInFilePrefix; the surfix can be specified by inFileSurfix (see following "CONFIGURING FAST!")

IMPORTANT:
* The input requires a line per observation. The input only requires that lines are sorted by time within a student. This means that the order of students or Knowledge Components (KCs)  doesn't matter as long as the input is sorted over time. 
* The train and test files should have the same number and same set of KCs (HMMs), i.e. the code won't predict for new KC(HMM) that it didn't train on.

-----------
MANDATORY COLUMNS:
-----------
* student COLUMN:
Integer or string.  
This column identifies sequences. 
All observations with the same "student" id will be placed in the same sequence.

* KC COLUMN:
Integer or string.
This column identifies an HMM model, and therefore is useful for training multiple KCs.
The model will learn parameters for each KC individually.
(You can put multiple KCs as features if you have multiple KCs per item/record, see sample2). 

* outcome COLUMN:
correct | incorrect
We only support binary HMMs.


-----------
OPTIONAL COLUMNS:
-----------
* FEATURE COLUMNS:
Feature columns should have prefix features_ or *features_.
    * Features must be numeric.  This is not a limitation, because string or categorical variables can be coded as binary features.  For example if you have a single feature that may take values red, green and blue, you could encode this as two different features (red = {0|1}, green={0,1}), or as three binary features (blue={0,1}).
	* Features that are marked with a star (*) have coefficients shared by both latent states (mastery and not mastery). See sample2
	* Features that do not have a star have a different coefficient for each latent state. 
	* By default, FAST adds bias feature to both hidden states.  Don't put bias(intercept) feature in the input (a feature always with value 1).
     If you want to change the configuration of bias features, please specify bias in configuration java class file (Opts.java). 
	* If some features are always 0 (never appear) in current KC but may have value 1 in other KCs, then put them as NULL for current KC's records (This is for the sake of computing gradient, if they are NULL then the code doesn't compute gradient for those features for current HMM, see sample1~3).
Although FAST currently has L2-regularization, yet in order to make coefficients more directly interpretable, and also speed up the training, sometimes doing some standardization or normalization of such features to map them to smaller values may help. Yet sometimes standardization or normalization is not suitable due to the feature value distribution (etc.) and will drop the performance. Please do some experimentation.

* problem COLUMN:
You can put the problem/item/question name/id here. Integer or string are both ok.

* step COLUMN:
It doesn't matter what you put here so far. However, it may help you to check the results if you use this column to put information identifying the order of the records.

* fold COLUMN:
By default, all values are 1.
If you have the kind of data split where some records from a student-skill sequence is used for training and remaining used for testing, then in the test0.txt file, please put the records used for training with fold COLUMN value -1. See sample3.




---------
CONFIGURING FAST!
---------
* See the details of configuration options: by command "java hmmfeatures/Run -help" or by src/hmmfeatures/Opts.java file
* We provided a sample configuration file in confs/fast.conf with some default values. However, you could add other options into the file according to your need.
* Here are the basic options:

	* basicModelName: FAST|KT  Would choose whether to run Knowledge Tracing or FAST (Now by default, when you run FAST, it only parameterizes the emission probabilities. We will release the version allowing parameterizing also transition probabilities soon).
	* modelName: It could be any string you like.
	* allowForget: True|False. If allowForget=false, then p(forget)=0, i.e. p(unknown|known)=0.
	
	* inDir: input files' directory. By default, training file: train0.txt; testing file: test0.txt.
	* outDir: output prediction and evaluation and log files' directory
	* (execPoolDir: for outputing execution info.)
	* (allModelComparisonOutDir: for output evaluation file comparing different models.)
	
	* trainInFilePrefix: the prefix of training set file(s).
	* testInFilePrefix: the prefix of testing set file(s).
	* inFileSurfix: the file surfix of training and testing set file(s).
	* testSingleFile: If testSingleFile=true, then numFolds and numRuns should set to 1, and FAST trains on "train0.txt" and test on "test0.txt" (if trainInFilePrefix=train, testInFilePrefix=test).
	* numFolds, numRuns: they are used to decide how many times FAST runs and are related to how FAST retrieves train and test files. By default, numRuns is always set to 1. 
       Test file shouldn't have new KCs (indicated by the "KC" column). Because in training process, each KC correspond to one HMM with its own set of parameters and they are used for testing corresponding KC (HMM).
	Test file should have the same feature columns (indicated by features_XXX or *features_XXX as train file.

		
		File is named by trainInFilePrefix(testInFilePrefix) + id + inFileSurfix. (id = current run id * numFolds + current fold id).
		For example, if numFolds=5, numRuns=1, then there should be 5 pairs of train and test files and should be named by train0.txt, train1.txt...train4.txt, and test0.txt, test1.txt ... test4.txt.



------------------
OUTPUT FILE
------------------

* "XXX.pred" FILE
The three columns actualLabel,predLabel, predProb are: actual student responses(correct or incorrect), predicted student responses and predicted probability of getting correct responses.
If the evaluation process outputs "ERROR: #files should be numFolds * numRuns!", it means there are more/less "XXX.pred" files than expected. You can solve this problem by removing (moving) files with ".pred" and rerun the code, or prepare the ".pred" files corresponding to numFolds * numRuns (e.g. if numFolds=2 and numRuns=1, then you should have "XXX_test0.pred", "XXX_test1.pred".)

* "XXX.eval" FILE and "evaluation.log" FILE
"XXX.eval" file includes the current evaluation while "evaluation.log" maintains the log of each time's evaluation.




------------------------------------------------
REFERENCES
------------------------------------------------
González-Brenes, José P., Yun Huang, and Peter Brusilovsky. General Features in Knowledge Tracing
to Model Multiple Subskills, Temporal Item Response Theory, and Expert Knowledge. (Nominated as Best Paper Award
in the 7th International Conference on Educational Data Mining 2014, first 2 authors contributed equally.)
