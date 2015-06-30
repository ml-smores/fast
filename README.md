
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
``` java -jar fast-2.1.1-final.jar ++data/IRT_exp/FAST+IRT1.conf  ````

Congratulations! You just trained a student model (with IRT features) using state of the art technology.


Please see the [Wiki](https://github.com/ml-smores/fast/wiki/) for more information. 



Contact us
==========
We would love to hear your feedback.  Please [email us] (mailto:ml-smores@googlegroups.com)!

Thanks,
Yun, 
Jose, 
and Peter
