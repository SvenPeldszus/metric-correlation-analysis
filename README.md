# metric-correlation-analysis

## About

Metric-Correlation-Analysis is an eclipse plugin for android app analysis.
It analyzes possible correlations between different quality and security metrics based on Android App Sourcecode.
To this end it integrates different tools, which calculate the metrics.
Metric-Correlation-Analysis consists of two main parts:
-	Calculation of Metrics is implemented in package metricTool
-	Calculation of some statistical outputs is implemented in package statistic


Integrated tools with associated metrics:

SourceMeter for the calculation of
	
-	Lines of Code per Class (LOCpC/Quality)
-	Weigthd Methods per Class (WMC/Quality)
-	Coupling between Objects (CBO/Quality)
-	Lack of Cohesion in Methods (LCOM/Quality)
-	Depth of Inheritence Tree (DIT/Quality)
-	Lines of Duplicate Code (LDC/Quality)

Hulk for the calculation of

-	Occurences of the Blob Anti-Pattern (BLOB/Quality)
-	Inappropriate Generosity with Accessibility of Types (IGAT/Security)
-	Inappropriate Generosity with Accessibility of Method (IGAM/Security)

AndroLyze for the calculation of

-	Minimal Permission Request (MPR)



## Installation

To be able to use Metric-Correlation-Analysis, you have to install several tools:

-	[SourceMeter](https://www.sourcemeter.com/)
-	[AndroLyze](https://androlyze.readthedocs.io/en/latest/install_manually.html)
-	Hulk as Eclipse-Plugin (Update-site: http://GRaViTY-Tool.github.io/updatesite/ )
-	MongoDB
-	Gradle
-	Git

Set several environment variables:
-	SOURCE_METER_JAVA: path to "SourceMeterJava" file in the directory of SourceMeter/Java
-	ANDROLYZE: path to the root directory of androlyze
-	MONGOD: path to "mongod" file in the "bin" directory of MongoDB

For usage on windows you have to copy ...\androlyze\androlyze\model\script\impl\CodePermissions.py to ...\androlyze (root-directory)



## Usage

In the main class metricTool\Executer you have to specify the class variable "String result_dir". 
The String should contain the path to a directory in your filesystem, where Metric-Correlation-Analysis should store all calculated files.
After running Metric-Correlation-Analysis you will find the file with all calculated metric values in "your_result_dir"\results.
The statistic output you will find in "your_result_dir"\statisticResults.

There are two options for using Metric-Correlation-Analysis:

-	You can store all projects you want to analyze in "your_result_dir"\Sourcecode.
	Metric-Correlation-Analysis will calculate the metrics for each of the projects in the specified directory.
	You just have to run metricTool\Executer as JUnit-Plugin
-	You can specify a CSV-file, which contains a list of git-urls, and store the path to that file in "File gitUrls" in the method "public void execute()"
	Then you have to replace the variable "src_location" in "mainProcess(File src_location, File result_file) with gitUrls.
	Metric-Correlation-Analysis will download all projects and calculate the metrics for each of the projects.

If you want to calculate some statistical output you have to run statistic\StatisticExecuter.
Before running you have to specify in the main-method the "File dataFile". This variable should contain the path to that file for which you want to calculate the statistics.

As Output Metric-Correlation-Analysis will generate:

-	a CSV-file with the result of Shapiro-Wilk-Test for each metric distribution
-	a CSV-file with a correlation matrix with Pearson Correlation Coefficient
-	a CSV-file with a correlation matrix with Spearman Correlation Coefficient
-	a box and whisker diagram from SourceMeter-results (Here you have to specify for which projects you want to generate the boxplots and store the specific SourceMeter results in "your_result_dir"\Boxplot.



