package var2;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import fileIO.ByteFile;
import fileIO.FileFormat;
import shared.Shared;
import shared.Tools;
import structures.ListNum;

public class VcfLoader {
	
	public VcfLoader(String fname_, ScafMap scafMap_, boolean vcfMode_){
		fname=fname_;
		scafMap=scafMap_;
		varMap=new VarMap(scafMap);
		threads=Tools.max(2, Shared.threads());
		inq=new ArrayBlockingQueue<ListNum<byte[]>>(threads);
		vcfMode=vcfMode_;
		ffin=FileFormat.testInput(fname, FileFormat.TXT, null, true, false);
	}
	
	public static VarMap loadVars(String fname, ScafMap scafMap){
		VcfLoader loader=new VcfLoader(fname, scafMap, false);
		ArrayList<ProcessThread> alpt=loader.spawnThreads();
		loader.waitForFinish(alpt);
		return loader.varMap;
	}
	
	public static VarMap loadVcf(String fname, ScafMap scafMap){
		VcfLoader loader=new VcfLoader(fname, scafMap, true);
		ArrayList<ProcessThread> alpt=loader.spawnThreads();
		loader.waitForFinish(alpt);
		return loader.varMap;
	}
	
	/** Spawn process threads */
	private ArrayList<ProcessThread> spawnThreads(){
		
		//Do anything necessary prior to processing
		
		//Determine how many threads may be used
		final int threads=this.threads+1;
		
		//Fill a list with ProcessThreads
		ArrayList<ProcessThread> alpt=new ArrayList<ProcessThread>(threads);
		for(int i=0; i<threads; i++){
			alpt.add(new ProcessThread(i, alpt));
		}
		if(verbose){outstream.println("Spawned threads.");}
		
		//Start the threads
		for(ProcessThread pt : alpt){
			pt.start();
		}
		if(verbose){outstream.println("Started threads.");}
		
		//Do anything necessary after processing
		return alpt;
	}
	
	private void waitForFinish(ArrayList<ProcessThread> alpt){
		//Wait for completion of all threads
		boolean allSuccess=true;
		for(ProcessThread pt : alpt){
			if(verbose){outstream.println("Waiting for thread "+pt.tid);}
			while(pt.getState()!=Thread.State.TERMINATED){
				try {
					//Attempt a join operation
					pt.join();
				} catch (InterruptedException e) {
					//Potentially handle this, if it is expected to occur
					e.printStackTrace();
				}
			}

			allSuccess&=pt.success;
		}
		
		//Track whether any threads failed
		if(!allSuccess){errorState=true;}
	}
	
	private Var loadVarLine(byte[] line){
		if(line==null || line.length<1){return null;}
		if(line[0]!='#'){
			return new Var(line, (byte)'\t');
		}else{
			String[] split=new String(line).split("\t");
			String a=split[0], b=(split.length>1 ? split[1] : null);
			assert(split.length>1) : new String(line);
			if(a.equalsIgnoreCase("#ploidy")){
				synchronized(varMap){varMap.ploidy=Integer.parseInt(b);}
			}else if(a.equalsIgnoreCase("#pairingRate")){
				synchronized(varMap){varMap.properPairRate=(float) Double.parseDouble(b);}
			}else if(a.equalsIgnoreCase("#totalQualityAvg")){
				synchronized(varMap){varMap.totalQualityAvg=(float) Double.parseDouble(b);}
			}else if(a.equalsIgnoreCase("#mapqAvg")){
				synchronized(varMap){varMap.totalMapqAvg=(float) Double.parseDouble(b);}
			}else if(a.equalsIgnoreCase("#readLengthAvg")){
				synchronized(varMap){varMap.readLengthAvg=(float) Double.parseDouble(b);}
			}
			synchronized(header){header.add(line);}
			return null;
		}
	}
	
	private Var loadVcfLine(byte[] line){
		if(line==null || line.length<1){return null;}
		if(line[0]!='#'){
			try {
				return Var.fromVCF(line, scafMap);
			} catch (Exception e) {
				System.err.println("Unable to parse VCF line: '"+new String(line)+"'");
				return null;
			}
		}else{
			String[] split=new String(line).split("=");
			if(split.length==2){
				String a=split[0], b=split[1];
				if(a.equalsIgnoreCase("##ploidy")){
					synchronized(varMap){varMap.ploidy=Integer.parseInt(b);}
				}else if(a.equalsIgnoreCase("##pairingRate")){
					synchronized(varMap){varMap.properPairRate=(float) Double.parseDouble(b);}
				}else if(a.equalsIgnoreCase("##totalQualityAvg")){
					synchronized(varMap){varMap.totalQualityAvg=(float) Double.parseDouble(b);}
				}else if(a.equalsIgnoreCase("##mapqAvg")){
					synchronized(varMap){varMap.totalMapqAvg=(float) Double.parseDouble(b);}
				}else if(a.equalsIgnoreCase("#readLengthAvg")){
					synchronized(varMap){varMap.readLengthAvg=(float) Double.parseDouble(b);}
				}
			}
			synchronized(header){header.add(line);}
			return null;
		}
	}
	
	private class ProcessThread extends Thread{
		
		ProcessThread(int tid_, ArrayList<ProcessThread> alpt_){
			tid=tid_;
			alpt=(tid==0 ? alpt_ : null);
		}
		
		@Override
		public void run(){
			//Do anything necessary prior to processing
			
			//Process the reads
			if(tid==0){
				processBytes();
			}else{
				makeVars();
			}
			
			//Indicate successful exit status
			success=true;
		}
		
		void processBytes(){
			processBytes0();
			
			success=true;
		}
		
		public final void processBytes0(){
			if(verbose){outstream.println("tid "+tid+" started processBytes.");}

//			ByteFile.FORCE_MODE_BF1=true;
			ByteFile.FORCE_MODE_BF2=true;
			ByteFile bf=ByteFile.makeByteFile(ffin);
			
			long number=0;
			
			ArrayList<byte[]> list=new ArrayList<byte[]>(LIST_SIZE);
			for(byte[] line=bf.nextLine(); line!=null; line=bf.nextLine()){
				assert(line!=null);
//				outstream.println("a");
				if(line[0]=='#'){
					if(vcfMode){loadVcfLine(line);}
					else{loadVarLine(line);}
				}else{
					list.add(line);
					if(list.size()>=LIST_SIZE){
						putBytes(new ListNum<byte[]>(list, number));
						number++;
						list=new ArrayList<byte[]>(LIST_SIZE);
					}
				}
			}
			if(verbose){outstream.println("tid "+tid+" ran out of input.");}
			if(list.size()>0){
				putBytes(new ListNum<byte[]>(list, number));
				number++;
				list=null;
			}
			if(verbose){outstream.println("tid "+tid+" done reading bytes.");}
			putBytes(POISON_BYTES);
			if(verbose){outstream.println("tid "+tid+" done poisoning.");}
			bf.close();
			if(verbose){outstream.println("tid "+tid+" closed stream.");}
		}
		
		final void putBytes(ListNum<byte[]> list){
			if(verbose){outstream.println("tid "+tid+" putting blist size "+list.size());}
			while(list!=null){
				try {
					inq.put(list);
					list=null;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(verbose){outstream.println("tid "+tid+" done putting blist");}
		}
		
		final ListNum<byte[]> takeBytes(){
			if(verbose){outstream.println("tid "+tid+" taking blist");}
			ListNum<byte[]> list=null;
			while(list==null){
				try {
					list=inq.take();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(verbose){outstream.println("tid "+tid+" took blist size "+list.size());}
			return list;
		}
		
		void makeVars(){
			if(verbose){outstream.println("tid "+tid+" started makeReads.");}
			
			ListNum<byte[]> list=takeBytes();
			ArrayList<Var> vars=new ArrayList<Var>(LIST_SIZE);
			while(list!=POISON_BYTES){
				vars.clear();
				if(vcfMode){
					for(byte[] line : list){
						assert(line[0]!='#') : new String(line);
						vars.add(loadVcfLine(line));
					}
				}else{
					for(byte[] line : list){
						assert(line[0]!='#') : new String(line);
						vars.add(loadVarLine(line));
					}
				}
				synchronized(varMap){
					for(Var v : vars){
						varMap.addUnsynchronized(v);
					}
				}
				list=takeBytes();
			}
			if(verbose){outstream.println("tid "+tid+" done making reads.");}

			putBytes(POISON_BYTES);
			if(verbose){outstream.println("tid "+tid+" done poisoning bytes.");}
			
//			putReads(POISON_READS);
//			if(verbose){outstream.println("tid "+tid+" done poisoning reads.");}
		}
		
		/*--------------------------------------------------------------*/

		final ArrayList<ProcessThread> alpt;
		final int tid;
		boolean success=false;
	}
	
	/*--------------------------------------------------------------*/
	/*----------------         Final Fields         ----------------*/
	/*--------------------------------------------------------------*/
	
	/** Primary input file */
	final String fname;
	final FileFormat ffin;
	
	final ArrayBlockingQueue<ListNum<byte[]>> inq;
	
	final int threads;
	
	ArrayList<byte[]> header=new ArrayList<byte[]>();
	
	/** Print status messages to this output stream */
	protected PrintStream outstream=System.err;
	
	final ScafMap scafMap;
	final VarMap varMap;
	final boolean vcfMode;
	
	boolean errorState=false;
	
	/*--------------------------------------------------------------*/
	/*----------------        Static Fields         ----------------*/
	/*--------------------------------------------------------------*/
	
	static final ListNum<byte[]> POISON_BYTES=new ListNum<byte[]>(null, -1);
	static final int LIST_SIZE=200;
	public static int DEFAULT_THREADS=3;
	static boolean verbose=false;
	
	
}
