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
	private BlockingQueue<Message> MasterQ, DoneQ;

	private int NumProcesses;
	
	public static int RootProcess;
	
	int RoundNo = 0;
	
	boolean AlgorithmCompleted = false;
	//To send the NEXT message to all the processes.
	private ArrayList<BlockingQueue<Message>> ProcessRoundQ = new ArrayList<BlockingQueue<Message>>();
	//Input Q to which other processes can write. 
	private ArrayList<BlockingQueue<Message>> InterProcessQ = new ArrayList<BlockingQueue<Message>>();
	
	public MasterProcess(int MProcessId, int[] ProcessIds){
		this.MasterProcessId = MProcessId;
		this.NumProcesses = ProcessIds.length;
		
		MasterQ = new ArrayBlockingQueue<>(NumProcesses);
		DoneQ = new ArrayBlockingQueue<>(NumProcesses);
		
		Message ReadyMessage;
		BlockingQueue<Message> ProcessRQ, interProcessQueue;
		for(int i = 0; i < NumProcesses; i++){
			ReadyMessage = new Message(ProcessIds[i], Message.MessageType.READY, Integer.MIN_VALUE, 'X');
			MasterQ.add(ReadyMessage);
			ProcessRQ = new ArrayBlockingQueue<>(NumProcesses);
			//capacity changed here to solve queue full error temporarily.
			interProcessQueue = new ArrayBlockingQueue<>(NumProcesses*10);
			ProcessRoundQ.add(ProcessRQ);
			InterProcessQ.add(interProcessQueue);
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

	public BlockingQueue<Message> getDoneQ() {
		return DoneQ;
	}

	public ArrayList<BlockingQueue<Message>> getProcessRoundQ() {
		return ProcessRoundQ;
	}
	
	public boolean isAlgorithmCompleted(){
		if(checkAllDone())
				return true;
		return AlgorithmCompleted;
	}
	
	public boolean checkAllDone(){
		if(DoneQ.size() == NumProcesses)
			return true;
		return false;
	}
	
	public void StartSampleTest(){	
		while(!isAlgorithmCompleted()){
			if(CheckAllReady()){
				StartNewRound();
				RoundNo++;
			}
		}
	}
	
	
	
	public ArrayList<BlockingQueue<Message>> getInterProcessQ() {
		return InterProcessQ;
	}

	public static void main(String[] args){
		
		//static initialization for testing currently.
		int MasterProcessID = 0;
		int[] ids = {1,2,3,4,5};
		Edge e;
		int n = 5;
		MasterProcess mp = new MasterProcess(MasterProcessID, ids);
		Processes[] process = new Processes[n];
		mp.RootProcess = 3;
		for(int i = 0;i < n;i++){
			process[i] = new Processes(ids[i]);
		}
		
		for(int i=0; i<n;i++){
			process[i].setQIn(mp.getInterProcessQ().get(i));
			process[i].setQRound(mp.getProcessRoundQ().get(i));
		}
		
		e = new Edge(process[0],process[1],5);
		process[0].addEdge(e);
		process[1].addEdge(e);
		
		e = new Edge(process[0],process[4],9);
		process[0].addEdge(e);
		process[4].addEdge(e);
		
		e = new Edge(process[0],process[2],3);
		process[0].addEdge(e);
		process[2].addEdge(e);
		
		e = new Edge(process[0],process[3],4);
		process[0].addEdge(e);
		process[3].addEdge(e);
		
		e = new Edge(process[1],process[2],6);
		process[1].addEdge(e);
		process[2].addEdge(e);
		
		e = new Edge(process[1],process[4],1);
		process[1].addEdge(e);
		process[4].addEdge(e);
		
		e = new Edge(process[2],process[3],7);
		process[2].addEdge(e);
		process[3].addEdge(e);
		
		e = new Edge(process[2],process[4],2);
		process[2].addEdge(e);
		process[4].addEdge(e);
		
		e = new Edge(process[3],process[4],8);
		process[3].addEdge(e);
		process[4].addEdge(e);
		
		
		Thread[] T = new Thread[n];
		for(int i=0;i < n;i++){
			process[i].setQMaster(mp.getMasterQ());
			process[i].setQDone(mp.getDoneQ());
			T[i] = new Thread(process[i]);
			T[i].start();
		}
		//mp.StartSampleTest();
		//since code does not stop automatically, need to forcefully stop it.
		while(!mp.isAlgorithmCompleted() && mp.RoundNo < 100){
			if(mp.CheckAllReady()){
				mp.StartNewRound();
				mp.RoundNo++;
			}
		}
		for(int i = 0;i<n;i++){
			T[i].interrupt();
		}
		for(int i = 0; i < T.length; i++)
		{
			try {
				T[i].join();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		for(int i=0;i<n;i++){
			System.out.println("Process "+(i+1)+" parent's and child are:");
			process[i].printParentID();
			process[i].printChildID();
		}
	}
}
