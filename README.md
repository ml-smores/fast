
FAST: Feature-Aware Student knowledge Tracing
=============================================

This is the repository of FAST, an efficient toolkit for modeling time-changing student performance ([González-Brenes, Huang, Brusilovsky et al, 2014] (http://educationaldatamining.org/EDM2014/uploads/procs2014/long%20papers/84_EDM-2014-Full.pdf)). FAST is alterantive to the [BNT-SM toolkit] (http://www.cs.cmu.edu/~listen/BNT-SM/), a toolkit that requires the researcher to design a different different Bayes Net for each feature set they want to prototype.
The FAST toolkit is up to 300x faster than BNT-SM, and much simpler to use.

We presented the model in the 7th International Conference on Educational Data Mining (2014) (see [slides]  (http://www.cs.cmu.edu/~joseg/files/fast_presentation.pdf) ), where it was selected as one the top 5 paper submissions.



Technical Details 
-----------------
FAST learns per parameters for each skill using an HMM with Features ([Berg-Kirpatrick et al, 2010] (http://www.cs.berkeley.edu/~tberg/papers/naaclhlt2010.pdf)).




Running FAST
============

Quick Start
------------

1. Download the latest release [here] (https://github.com/ml-smores/fast/releases).
2. Decompress the file.  It includes sample data for getting you started quickly.
3. Open a terminal and type:  
``` java -jar fast-1.0.2-final.jar ++input/FAST+IRT.conf  ````

Congratulations! You just trained a student model (with IRT features) using state of the art technology.


Input File
==========
FAST requires a configuration file that specifies training and testing file(s). We now describe the format of these input files.

The input files should be delimited by tab(\t) or comma (,). 
Currently, the filenames must follow the pattern  ```prefix-X-suffix``` (e.g. train0.txt). X is a number from 0 to ```numFolds```. Details can be seen in ```CONFIGURING FAST!```.

IMPORTANT:
* The input requires a line per observation. The input only requires that lines are sorted by time within a student. This means that the order of students or Knowledge Components (KCs)  doesn't matter as long as the input is sorted over time. 
* The test file shouldn't have new KCs (HMMs), i.e. the code won't predict for the new KC(HMM) that it didn't train on
* The test file should have the same feature columns (indicated by features_XXX or *features_XXX) as train file.


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
(You can put multiple KCs as features if you have multiple KCs per item/record, see files with prefix "FAST+subskill"). 

* outcome COLUMN:
correct | incorrect
We only support binary HMMs.


OPTIONAL COLUMNS:
-----------
* FEATURE COLUMNS:
Feature columns should have prefix features_ or *features_.
    * Features must be numeric.  This is not a limitation, because string or categorical variables can be coded as binary features.  For example if you have a single feature that may take values red, green and blue, you could encode this as two different features (red = {0|1}, green={0,1}), or as three binary features (blue={0,1}).
	* Features that are marked with a star (*) have coefficients shared by both latent states (mastery and not mastery). See files with prefix "FAST+subskill".
	* Features that do not have a star have a different coefficient for each latent state. 
	* By default, FAST adds bias feature to both hidden states.  Don't put bias(intercept) feature in the input (a feature always with value 1).
     If you want to change the configuration of bias features, please specify bias in configuration java class file (Opts.java). 
	* If some features are always 0 (never appear) in current KC but may have value 1 in other KCs, then put them as NULL for current KC's records (This is for the sake of computing gradient, if they are NULL then the code doesn't compute gradient for those features for current HMM, see files with prefix "FAST").
Although FAST currently has L2-regularization, yet in order to make coefficients more directly interpretable, and also speed up the training, sometimes doing some standardization or normalization of such features to map them to smaller values may help. Yet sometimes standardization or normalization is not suitable due to the feature value distribution (etc.) and will drop the performance. Please do some experimentation.

* problem COLUMN:
You can put the problem/item/question name/id here. Integer or string are both ok.

* step COLUMN:
It doesn't matter what you put here so far. However, it may help you to check the results if you use this column to put information identifying the order of the records.

* fold COLUMN:
By default, all values are 1.
If you have the kind of data split where some beginning records from a student-skill sequence is used for training and remaining used for testing, then in the test0.txt file, please put the records used for training with fold COLUMN value -1. See files with prefix "FAST+item_split_seq".




CONFIGURING FAST!
================
* See the details of configuration options: by command "java hmmfeatures/Run -help" or by src/hmmfeatures/Opts.java file
* We provided some example configuration file in input/XXX.conf with some default values. However, you could add other options into the file according to your need.
* Here are the basic options:

	* basicModelName: FAST|KT  Would choose whether to run Knowledge Tracing or FAST (Now by default, when you run FAST, it only parameterizes the emission probabilities. We will release the version allowing parameterizing also transition probabilities soon).
	* modelName: It could be any string you like.
	* allowForget: True|False. If allowForget=false, then p(forget)=0, i.e. p(unknown|known)=0.
	
	* inDir: input files' directory. By default, training file: train0.txt; testing file: test0.txt.
	* outDir: output prediction and evaluation and log files' directory
	* allModelComparisonOutDir: for output evaluation file containing all models runned before for comparing different models.)
	
	* trainInFilePrefix: the prefix of training set file(s).
	* testInFilePrefix: the prefix of testing set file(s).
	* inFileSuffix: the file Suffix of training and testing set file(s).
	* testSingleFile: If testSingleFile=true, then numFolds should be set to 1; otherwise, FAST can automatically retrieve multiple train and test files according to specified numFolds.
	* numFolds: this is the number of train and test pairs, which is used for FAST automatically retrieving multiple train and test files. 
		
	File will be named by trainInFilePrefix(testInFilePrefix) + id + inFileSuffix with id equals to current fold id (starting from 0). For example, if numFolds=5, trainInFilePrefix=train, testInFilePrefix=test, and inFileSuffix=.txt, then there should be 5 pairs of train and test files in the inDir and they should be named by train0.txt, train1.txt...train4.txt, and test0.txt, test1.txt ... test4.txt.


OUTPUT FILE
===========

* "XXX.pred" FILE
The three columns actualLabel,predLabel, predProb are: actual student responses(correct or incorrect), predicted student responses and predicted probability of getting correct responses.
If the evaluation process outputs "ERROR: #files should be numFolds * numRuns!", it means there are more/less "XXX.pred" files than expected. You can solve this problem by removing (moving) files with ".pred" and rerun the code, or prepare the ".pred" files corresponding to numFolds * numRuns (e.g. if numFolds=2 and numRuns=1, then you should have "XXX_test0.pred", "XXX_test1.pred".)

* "XXX.eval" FILE and "evaluation.log" FILE
"XXX.eval" file includes the current evaluation while "evaluation.log" maintains the log of each time's evaluation.


Contact us
==========
We would love to hear your feedback.  Please [email us] (mailto:ml-smores@googlegroups.com)!

Thanks,
Yun, 
Jose, 
and Peter
