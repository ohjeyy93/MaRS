#!/usr/bin/bash
#title		:setup_check.sh
#description	:this  will check for for necessary MaRS dependencies and install them if not present .. 
#author		:Eldin Talundzic
#date		:02/13/2018
#version	:0.0.1
#usage		:sh setup_check.sh
#notes		:I need to get a valentines gift!  
#=================================================================================================================================

echo "Now installing required dependencies... 

"
pip3 install pyvcf pysam matplotlib seaborn pandas numpy xlrd openpyxl 
pip3 list

echo "
In the above list now, you should see pyvcf, pysam, matplotlib, seaborn, pandas, numpy, xlrd and openpyxl.

If they are not in the list and/or you encounter any errors with installing pyvcf, pysam, matplotlib, seaborn, pandas, numpy or virtualenv: 

	try: pip3 install [name_of_package] 
 	type: pip3 -> to print usage 	

	"
