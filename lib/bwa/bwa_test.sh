#!/usr/bin/sh

#display_usage()
#{
#	echo "run_bbduk.sh <path to input fastq> "
#	echo "The trimmed fastq will be produced in the same directory as the specified input"
#}

#if [ $# -eq 0 ]; then 			# if no arg = 0 dipslay usage 
#	display_usage
#	exit 1
#fi

./bwa index test_data/ref/K13_ref.fasta 	# create index using own ref
./bwa mem K13_ref.fasta




