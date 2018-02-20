/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jgi;

import dna.AminoAcid;
import java.util.ArrayList;
import java.util.Locale;

import fileIO.FileFormat;
import fileIO.ReadWrite;
import fileIO.ByteStreamWriter;
import java.util.Arrays;
import java.util.List;
import static jgi.Dedupe.maxNmer;
import static jgi.Dedupe.nmerIndex;
import shared.Parser;
import shared.PreParser;
import shared.Shared;
import shared.Timer;
import shared.Tools;
import stream.ByteBuilder;
import stream.ConcurrentReadInputStream;
import stream.Read;
import structures.ListNum;

/**
 *
 * @author syao
 * Last updated : 01102018
 */
public class TetramerFrequencies {
    public static void main(String[] args){
        
        System.out.println("Start Tetramer Frequencies analysis ...");
        
        Timer t=new Timer();

        //Create an instance of this class
        TetramerFrequencies x=new TetramerFrequencies(args);

        //Run the object
        x.process(t);

        //Close the print stream if it was redirected
        Shared.closeStream(x.outstream);
    }

    public TetramerFrequencies(String[] args){

        {//Preparse block for help, config files, and outstream
            PreParser pp=new PreParser(args, getClass(), false);
            args=pp.args;
            outstream=pp.outstream;
        }

        int k_=4;
        Parser parser=new Parser();
        for (String arg : args) {
            String[] split=arg.split("=");
            String a=split[0].toLowerCase();
            String b=split.length>1 ? split[1] : null;

            if (a.equals("out")){
                out1 = b;
            } else if (a.equals("s") || a.equals("step")){
                step = Integer.parseInt(b);
            } else if (a.equals("w") || a.equals("window")){
                winSize = Integer.parseInt(b);
            } else if (a.equals("k")){
                k_ = Integer.parseInt(b);
            } else if(parser.parse(arg, a, b)){
                //do nothing
            }
        }

        {//Process parser fields
            Parser.processQuality();

            maxReads=parser.maxReads;
            in1=parser.in1;
        }

        k=k_;
        ffin1=FileFormat.testInput(in1, FileFormat.FASTQ, null, true, true);
        
        
        // initialize the class member textstream writer here, so no need of keep results in memory
        if (out1==null || out1.equals("")){
            out1 = "stdout";
        }
            
        bsw = new ByteStreamWriter(out1, true, false, false);
        bsw.start();

        //create and write the output header
        List<String> head = new ArrayList<String>();
        head.add("scaffold");
        head.add("range");

        // the unit tetramer strings in header
        tetramerGen2(head);    
        bsw.println(String.join("\t", head));         
    }
    
    void process(Timer t){
		
        final ConcurrentReadInputStream cris;
        {
            cris=ConcurrentReadInputStream.getReadInputStream(maxReads, true, ffin1, null);
            cris.start();
        }
        boolean paired=cris.paired();

        long readsProcessed=0;
        {
            ListNum<Read> ln=cris.nextList();
            ArrayList<Read> reads=(ln!=null ? ln.list : null);

            if(reads!=null && !reads.isEmpty()){
                Read r=reads.get(0);
                assert((ffin1==null || ffin1.samOrBam()) || (r.mate!=null)==cris.paired());
            }

            while(reads!=null && reads.size()>0){
                if(verbose){outstream.println("Fetched "+reads.size()+" reads.");}
                    
                for (Read r1 : reads){
                    if (winSize > 0){
                        windowedTetramerProfile(r1.bases, r1.id);
                    } else {
                        readTetramerProfile(r1.bases, r1.id);
                    }
                    readsProcessed++;
                }

                cris.returnList(ln.id, ln.list.isEmpty());
                if(verbose){
                    outstream.println("Returned a list.");
                }
                ln=cris.nextList();
                reads=(ln!=null ? ln.list : null);
            }
            
            // close the output file
            bsw.poisonAndWait();

            if(ln!=null){
                cris.returnList(ln.id, ln.list==null || ln.list.isEmpty());
            }
        }
        
        ReadWrite.closeStream(cris);
        if(verbose){outstream.println("Finished.");}

        t.stop();
        outstream.println("Time:                         \t"+t);
        outstream.println("Reads Processed:    "+readsProcessed+" \t"+String.format(Locale.ROOT, "%.2fk reads/sec", (readsProcessed/(double)(t.elapsed))*1000000));
    }

    // TODO: optimize the algorithm ...
    private void windowedTetramerProfileOpt(byte[] bases, String header){
        int sidx = 0;
        int eidx = winSize;
        
        final int[] counts = new int[maxNmer+1];
        
        int leadlen = 0;
        int leadtmer = -1;
        int endlen = 0;
        int endtmer = -1;
        
        int tmer = 0;
        int len = 0;
        
        for (int i=0; i<= eidx; i++){
            int x = AminoAcid.baseToNumber[bases[i]];
            if (x < 0){
                tmer = 0;
                len = 0;
            } else {
                tmer = (leadtmer<<2)|x ;   // this can produce -1 vaue if any base in tetramer is N!
                len++;
                if (len >= k){
                    int idx = tmerIndex(tmer);
                    counts[idx]++; 
                    
                    if( leadtmer==-1){
                        leadtmer = tmer;   
                    } else if(i == eidx){
                        
                    }
                    
                    
                    len--;
                }   
            }
        }
           

       
    }
    
    private void windowedTetramerProfile(byte[] bases, String header){
        int sidx = 0;
        int eidx = winSize;
        
        final int[] counts = new int[maxNmer+1];

        ByteBuilder bb=new ByteBuilder();
        while (eidx <= bases.length){
            //For each window ...
            tetramerCounter(bases, sidx, eidx, counts);
           
//            // build the report line : "SEQ_NAME START-END\t#\t#\t#  ..." : 
//            List<String> dataline = new ArrayList<String>();
//            dataline.add(header);
//            dataline.add(String.format("%d-%d", sidx+1, eidx));
//            for (int cnt: counts){
//                dataline.add(String.format("%d", cnt));
//            }
//            
//            bsw.println(String.join("\t", dataline));
            
            bb.clear();
            bb.append(header);
            bb.append(sidx+1);
            bb.append('\t');
            bb.append(eidx);
            for (int cnt: counts){
            	bb.append('\t');
            	bb.append(cnt);
            }
            bb.append('\n');
            bsw.print(bb.toBytes());
            
            // prep for next window ...
            sidx += step;
            eidx += step;
            Arrays.fill(counts, 0);
        }
    }
    
    public void readTetramerProfile(byte[] bases, String header){
        int sidx = 0;
        int eidx = winSize;
        
        final int[] counts = new int[maxNmer+1];
        
        tetramerCounter(bases, 0, bases.length, counts);

        // build the report line : "SEQ_NAME START-END\t#\t#\t#  ..." : 
        List<String> dataline = new ArrayList<String>();
        dataline.add(header);
        dataline.add(String.format("%d-%d", sidx+1, bases.length));
        for (int cnt: counts){
            dataline.add(String.format("%d", cnt));
        }

        bsw.println(String.join("\t", dataline));

        // prep for next window ...
        sidx += step;
        eidx += step;
        Arrays.fill(counts, 0);
    }
        
    // facter this out so we can work on reads
    public int[] tetramerCounter(byte[] bases, int startidx, int endidx, int[] counts){
        if (counts == null){
            counts = new int[maxNmer+1];
        }
        
        for (int i=startidx; i<=endidx-k; i++){ 
            int kmer = 0;   // the binary representation of the tetramer in the sliding window
            for (int j=0; j<k; j++){
                int x = AminoAcid.baseToNumber[bases[i+j]];
                kmer= (kmer<<2)|x ;   // this can produce -1 vaue if any base in tetramer is N!
            }

            // ignore tetramers containing "N" base
            if (kmer > -1){
                int idx = tmerIndex(kmer);
                counts[idx]++; 
            }
        }
        
        return counts;
    }
    
    private int tmerIndex(int tmer){
        int rtmer = AminoAcid.reverseComplementBinaryFast(tmer, k); 
        int mtmer = Tools.min(tmer, rtmer);
        int idx = -1;
        try{
            idx = nmerIndex[rtmer];
        } catch (ArrayIndexOutOfBoundsException e){
            System.out.printf("ArrayIndexOutOfBoundsException tmer=%d: rtmer=%d; \n", tmer, rtmer);
            System.exit(-1);
        }
        return idx;
    }
    
    
    public List<String> tetramerGen(List<String> tlist){
        if (tlist == null){
            tlist = new ArrayList<String>();
        }
   
        int[] bases = {0, 1, 2, 3}; // A C G T
        
        for (int b1 : bases){
            for (int b2 : bases){
                for (int b3 : bases){
                    for (int b4 : bases){
                        int tcode = b1;
                        tcode = (tcode<<2)|b2;
                        tcode = (tcode<<2)|b3;
                        tcode = (tcode<<2)|b4;
                        String tmer = AminoAcid.kmerToString(tcode, k).toLowerCase();
                        String rtmer = AminoAcid.kmerToString(AminoAcid.reverseComplementBinaryFast(tcode, k), k).toLowerCase(); 
                        
                        if (!tlist.contains(rtmer)){
                            tlist.add(tmer);
                        } 
                    }
                }
            } 
        }
        // total of 136 unique tetramers
               return tlist;
    }
    
    public List<String> tetramerGen2(List<String> tlist){
        if (tlist == null){
            tlist = new ArrayList<String>();
        }
        
        final int max=(1<<(2*k))-1;

        for(int i=0; i<=max; i++){
            final int a=i, b=AminoAcid.reverseComplementBinaryFast(i, k);
            int min=Tools.min(a, b);
            if(min==a){
                tlist.add(AminoAcid.kmerToString(a, k).toLowerCase());
            }
        }
        return tlist;
    }
    
    public void printTetramerFromCode(long code){
        System.out.println(AminoAcid.kmerToString(code, k));
    }
        
    public void printTetramers(byte[] bases, String header){
        int sidx = 0;
        int eidx = winSize;

        while (true){
            if (eidx > bases.length){
                break;
            }
            System.out.printf("%s %d-%d\n", header, sidx+1, eidx);

            // count tetramer here
            for (int i=sidx; i<eidx-k; i++){
                char[] tetramer = new char[k];
                for (int j=0; j<k; j++){
                    tetramer[j] = (char)bases[i+j];
                }
                System.out.println(new String(tetramer));
            }

            sidx += step;
            eidx += step;
        }
    }
    
    public static void printHelp(){
        List<String> helplist = new ArrayList<String>();
        helplist.add("Program Name : TetramerFrequencies v1.1");
        helplist.add("Usage : ");
        helplist.add(" -h : this page");
        helplist.add(" -s : step [500]");
        helplist.add(" -w : window size [2000]. If set to 0 the whole sequence is processed");
        System.out.println(String.join("\n", helplist));
    }

    /*--------------------------------------------------------------*/

    /*--------------------------------------------------------------*/
	
    private String in1 = null;
    private String out1 = null;
    private ByteStreamWriter bsw = null; // for output

    private final FileFormat ffin1;

    /*--------------------------------------------------------------*/

    private long maxReads=-1;
    private int step = 500;
    private int winSize = 2000;
    private final int k;

    /*--------------------------------------------------------------*/

    private java.io.PrintStream outstream=System.err;
    public static boolean verbose=false;
}
