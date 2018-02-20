# 2/15/2018 

# MaRS (Malaria Resistance Surveillance) - OSX version 

The emergence of resistance to all currently available antimalarial drugs in multiple regions of the world represents a current global public health challenge. In order to monitor and address this situation, faster and more effective surveillance tools are required to track and monitor the emergence and evolution of drug resistance in malaria. The Malaria Resistance Surveillance (MaRS) project aims to address this challenge by collating and mapping genetic polymorphisms associated with drug resistance in malaria around the world. The project achieves this by employing a targeted amplicon deep sequencing (TADS) approach [Lab Protocol](https://github.com/CDCgov/MaRS/tree/master/lab_sop) to detect single nucleotide polymorphisms on all major malaria drug resistance genes associated genes in samples sourced from travelers returning to the US from overseas, as well as samples actively collected in collaboration with partners from other countries.

Data for this project can be found at the following link [NCBI BioProject](https://www.ncbi.nlm.nih.gov/bioproject/?term=PRJNA428490). Collaborators are encouraged to submit their own data using this [NCBI BioProject](https://www.ncbi.nlm.nih.gov/bioproject/?term=PRJNA428490)

The Malaria Resistance Surveillance or MaRS analysis pipline, is an attempt at standardizing the workflow for identifying both known and new polymorhisms in *P.falciparum* genes associated with drug resistance.


# Setup

## Getting started

1. Download git repository:

> Clone the `osx_MaRSv1.1.2` branch of this repository to desired working directory.
```sh
git clone -b osx_MaRSv1.1.2  https://github.com/CDCgov/MaRS.git
```

2. Check that you have homebrew and python3.  

> Script below will check if you have homebrew and python3. If not it will install [homebrew](https://brew.sh) and python3. Run: 
```sh
sh brew_py.sh
```

3. Install [virtualenv](https://virtualenv.pypa.io/en/stable/) and initate a local virtual environment. 
> Before installing virtualenv you need to add the following path: `PATH="/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"` to your `.bash_profile`. For example, use `vim ~/.bash_profile` to edit and/or create a new *.bash_profile* file. Copy the above path into the file and save `:wq` if using vim.  

```sh
pip3 install virtualenv 
virtualenv mars_venv		# create required dependencies for venv, put them in dir mars_venv 
source mars_venv/bin/activate	# activate your virtual environment
```
> If succesfuly activated, you should see now `(mars_venv)` in front of your terminal username. 

4. Install required dependencies for MaRS. Run:
```sh
sh setup_dep.sh
``` 

5. Time to run MaRS! 

> ...
```sh
sh run.sh <path to experiment folder> <path to output folder>
```







