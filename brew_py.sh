#!/usr/bin/bash
#title		:sh brew_py_venv.sh
#description	:this will check that you have brew and python3 installed; required for MaRS
#author		:Eldin Talundzic
#date		:02/14/2018
#version	:0.0.1
#usage		:sh brew_py_venv.sh 
#notes		:
#=================================================================================================================================

echo "

Running MaRS pre-setup check for OSX system..."

display_m1()
{
	echo "
	
	You don't have homebrew. Need to install it now..
	
	(make sure to accept by pressing return when prompted)
	"
}

#Check if homebrew is installed, diplay above message and if messages execuates (&&) initiate install of brew

if [ -f /usr/local/bin/brew ]; then echo "You have homebrew!"; else display_m1 && /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"; fi 


display_m2()
{
	echo "
	
	You don't have python3 and/or pip3. Need to install it now..
	
	"
}

#Check if python3 and (-a) pip3 are installed 

if [ -f /usr/local/bin/python3 -a /user/local/bin/pip3 ]; then echo "You have python3 and pip3!"; else display_m2 && brew install python3; fi 

echo "
"
