package dc_project1_bellman_ford;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MasterProcess {
	
	private int MasterProcessId;
	//To signal start of new round to all processes.
	private BlockingQueue<Message> MasterQ;

	private int NumProcesses;
	public static Processes RootProcess;
	
	boolean AlgorithmCompleted = false;
	
	private ArrayList<BlockingQueue<Message>> ProcessRoundQ = new ArrayList<BlockingQueue<Message>>();
	
	public MasterProcess(int MProcessId, int[] ProcessIds){
		this.MasterProcessId = MProcessId;
		this.NumProcesses = ProcessIds.length;
		
		MasterQ = new ArrayBlockingQueue<>(NumProcesses);
		
		Message ReadyMessage;
		BlockingQueue<Message> ProcessRQ;
		for(int i = 0; i < NumProcesses; i++){
			ReadyMessage = new Message(ProcessIds[i], Message.MessageType.READY, Integer.MIN_VALUE, 'X');
			MasterQ.add(ReadyMessage);
			ProcessRQ = new ArrayBlockingQueue<>(NumProcesses);
			ProcessRoundQ.add(ProcessRQ);
		}
	}
	
	public boolean CheckAllReady(){
		if(MasterQ.size() < NumProcesses){
			return false;
		}
		int Count = 0;
		Message Msg;
		for(int i = 0; i < NumProcesses; i++){
			try {
				Msg = MasterQ.take();
				if(Msg.getMtype() != Message.MessageType.READY){
					return false;
				}
				if(Msg.getMtype() == Message.MessageType.READY){
					Count++;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(Count == NumProcesses){
			return true;
		}
		return false;
	}
	
	public void StartNewRound(){
		Iterator<BlockingQueue<Message>> Iter = ProcessRoundQ.iterator();
		BlockingQueue<Message> Q;
		Message Msg;
		
		while(Iter.hasNext()){
			Q = Iter.next();
			Msg = new Message(MasterProcessId, Message.MessageType.NEXT, Integer.MIN_VALUE, 'X');
			Q.add(Msg);
		}
	}
	
	public BlockingQueue<Message> getMasterQ() {
		return MasterQ;
	}

	public ArrayList<BlockingQueue<Message>> getProcessRoundQ() {
		return ProcessRoundQ;
	}
	
	public boolean isAlgorithmCompleted(){
		return AlgorithmCompleted;
	}
	
	public void StartSampleTest(){
		
	}
}
