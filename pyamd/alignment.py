import os
import sys
import time
import logging
import argparse
import subprocess


class Bwa:
    
    def __init__(self, bwa_path, out_path, ref_path):
        self.bwa_path = bwa_path
        self.out_path = '{0}/alignments'.format(out_path)
        self.ref_path = ref_path
        
        if not os.path.exists(self.out_path):
            os.mkdir(self.out_path)
        return

    def bwamem(self, rone_path, rtwo_path):
        sam_path = '{0}/output.sam'.format(self.out_path)
        bwcmd = [self.bwa_path, 'mem', '-t', '4', self.ref_path,
                rone_path, rtwo_path, '>', sam_path]
        print(' '.join(bwcmd))
        bwrun = subprocess.Popen(' '.join(bwcmd), shell=True)
        bwrun.wait()

        return(sam_path, bwrun.returncode)


class Bowtie:
    
    def __init__(self, bowtie_path, out_path, ref_path):
        self.bowtie_path =  bowtie_path
        self.out_path = '{0}/alignments'.format(out_path)
        self.ref_path = ref_path

        if not os.path.exists(self.out_path):
            os.mkdir(self.out_path)
        return

    def bowtie(self, rone_path, rtwo_path):
        sam_path = '{0}/output.sam'.format(self.out_path)
        bwcmd = [self.bowtie_path, '-x', self.ref_path, '-1',
                rone_path, '-2', rtwo_path, '--very-sensitive',
                '-p', '4', '-S', sam_path]
        print(' '.join(bwcmd))
        bwrun = subprocess.Popen(bwcmd, shell=False)
        bwrun.wait()

        return(sam_path, bwrun.returncode)


class BBMap:
    def __init__(self, bbmap_path, out_path, ref_path):
        self.bbmap_path = bbmap_path
        self.out_path = out_path
        self.ref_path =  ref_path

        if not os.path.exists(self.out_path):
            os.mkdir(self.out_path)

    def bbmap(self, rone_path, rtwo_path):
        sam_path = '{0}/output.sam'.format(self.out_path)
        bbcmd = [self.bbmap_path, 'ref={0}'.format(self.ref_path),
                'in={0}'.format(rone_path), 'in2={0}'.format(rtwo_path),
                'out={0}'.format(sam_path)]
        print(' '.join(bbcmd))
        bbrun = subprocess.Popen(bbcmd, shell=False)
        bbrun.wait()

        return(sam_path, bbrun.returncode)

class Snap:
    def __init__(self, snap_path, out_path, ref_path):
        self.snap_path = snap_path
        self.out_path = out_path
        self.ref_path = os.path.dirname(ref_path)
        
        if not os.path.exists(self.out_path):
            os.mkdir(self.out_path)


    def snap(self, rone_path, rtwo_path):
        sam_path = '{0}/output.sam'.format(self.out_path)
        scmd = [self.snap_path, 'paired', self.ref_path, rone_path, rtwo_path, 
                '-t', '4', '-o', '-sam', sam_path]
        print(' '.join(scmd))
        srun = subprocess.Popen(scmd, shell=False)
        srun.wait()
        
        return(sam_path, srun.returncode)

 