#!/usr/bin/sh

display_usage()
{
	echo "run_bbduk.sh <path to input fastq> "
	echo "The trimmed fastq will be produced in the same directory as the specified input"
}

if [ $# -eq 0 ]; then 			# if no arg = 0 dipslay usage 
	display_usage
	exit 1
fi

user_in=$1				# first argument (path to input fastq), must be provided
str="_trimmed.fastq"			# string modifier, add trimmed.fastq to output file  
user_out=$user_in$str			# use same output dir as specified input + add string modifier 

./bbduk.sh in=$user_in out=$user_out	# see sh bbduk.sh for usage 

# TODO: add additional logic to clean up output file and allow additional parameters to be configured
