# Master Theses Kirsten Scherer Auberson
This project contains the entire source code and directory structure for the code supporting my Master Thesis

## Directory Structure
 
```
MasterThesisKSA                                          -> The project's root directory: Contains a README file and the main    
│   README.md                                               configuration files and scripts used to deploy and execute the project
│   run.sh                                                  
│
└───data
│   └───raw                                              -> Raw data: This is the unmodified data as downloaded from stackexchange
│   │   └───stackexchange
│   │       │   aviation.stackexchange.com.7z           
│   │       │   aviation.meta.stackexchange.com.7z     
│   │       │   ...
│   │      
│   └───intermediate                                     -> Intermediate data: Data derived from the raw data by the program and
│   │                                                       used internally
│   └───processed                                        -> Processed data: This is the final form fo the data, as fed to the  
│                                                           classifier for training
│                                    
└───models                                               -> The trained, final models as output by the classifier after training
│       
│                  
└───references                                           -> Various reference files used in this project, e.g. Data Dictionaries
│              
│              
└───reports                                              -> Intermediate reports used during development
│       
│                                   
└───src                                                  -> Source code of the Proof-of-Concept implementation
│   │
│   └───package1                                         -> The source will be subdivided in packages, the exact structure will
│       │   file1.py                                        depend on the language used
│       │   file2.py
│       │   ...
│   
└───target                                               -> Compiled, executable code of the Proof-of-Concept implementation
│   │   package1.whl                                        The structure and contents will depend on the language used.
│   │   ...
│   
```
# MT2018_PoC_ConceptDrift
