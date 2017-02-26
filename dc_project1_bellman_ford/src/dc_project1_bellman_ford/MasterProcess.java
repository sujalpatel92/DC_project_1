package dc_project1_bellman_ford;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/*
 * Team Members:
 * Sujal Patel (ssp150930)
 * Harshil Shah (hxs155030)
 * Sagar Mehta (sam150930)
 * 
 * This class is the main class. It acts as master thread that synchronizes rounds.
 */

public class MasterProcess {

	//Need to check the usability of this variable.
	private int MasterProcessId;
	//masterQ -> The Q in which processes signal master they are ready for next round.
	//doneQ -> The Q with 1 capacity where in Root process with write Done message once converge cast is complete on Root.
	private BlockingQueue<Message> masterQ, doneQ;
	//readyToSendQ -> The Q to synchronize the sending of messages between processes so all the processes are at same level of execution of round.
	private BlockingQueue<Message> readyToSendQ;
	//number of processes.
	private int numProcesses;
	//The ID of the root process, so the processes can be initialized accordingly.
	public static int rootProcessID;
	
	//for debugging purposes.
	int RoundNo = 0;
	//To break off the while loop emulating the continuous run of system.
	boolean AlgorithmCompleted = false;
	// To send the NEXT message to all the processes.
	private ArrayList<BlockingQueue<Message>> ProcessRoundQ = new ArrayList<BlockingQueue<Message>>();
	// Input Q of processes to which other processes can write.
	private ArrayList<BlockingQueue<Message>> InterProcessQ = new ArrayList<BlockingQueue<Message>>();
	//Constructor
	public MasterProcess(int MProcessId, int[] ProcessIds) {
		this.MasterProcessId = MProcessId;
		this.numProcesses = ProcessIds.length;

		masterQ = new ArrayBlockingQueue<>(numProcesses);
		doneQ = new ArrayBlockingQueue<>(5);
		readyToSendQ = new ArrayBlockingQueue<>(numProcesses);

		Message ReadyMessage;
		BlockingQueue<Message> ProcessRQ, interProcessQueue;
		for (int i = 0; i < numProcesses; i++) {
			ReadyMessage = new Message(ProcessIds[i], Message.MessageType.READY, Integer.MIN_VALUE, 'X');
			masterQ.add(ReadyMessage);
			// Will only hold one NEXT message from master thread, but extra capacity of unseen contingencies.
			ProcessRQ = new ArrayBlockingQueue<>(5);
			// Expecting there will be only one message per process to the each process. Extra capacity to avoid Queue full error.
			interProcessQueue = new ArrayBlockingQueue<>(numProcesses*2);
			ProcessRoundQ.add(ProcessRQ);
			InterProcessQ.add(interProcessQueue);
		}
	}

	public boolean checkAllProcessesReady() {
		if (masterQ.size() < numProcesses) {
			return false;
		}
		int Count = 0;
		Message Msg;
		for (int i = 0; i < numProcesses; i++) {
			try {
				Msg = masterQ.take();
				if (Msg.getMtype() != Message.MessageType.READY) {
					return false;
				}
				if (Msg.getMtype() == Message.MessageType.READY) {
					Count++;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (Count == numProcesses) {
			return true;
		}
		return false;
	}
	
	public void startNewRound() {
		Iterator<BlockingQueue<Message>> iter = ProcessRoundQ.iterator();
		BlockingQueue<Message> q;
		Message msg;
		masterQ.clear();
		//Write the next message to appropriate queue of processes.
		synchronized (this) {
			while (iter.hasNext()) {
				q = iter.next();
				q.clear();
				msg = new Message(MasterProcessId, Message.MessageType.NEXT, Integer.MIN_VALUE, 'X');
				q.add(msg);
			}
		}
	}

	public BlockingQueue<Message> getMasterQ() {
		return masterQ;
	}

	public BlockingQueue<Message> getDoneQ() {
		return doneQ;
	}
	
	public BlockingQueue<Message> getReadyToSendQ() {
		return readyToSendQ;
	}

	public ArrayList<BlockingQueue<Message>> getProcessRoundQ() {
		return ProcessRoundQ;
	}

	public ArrayList<BlockingQueue<Message>> getInterProcessQ() {
		return InterProcessQ;
	}

	public boolean isAlgorithmCompleted() {
		if (checkRootProcessDone())
		{
			System.out.println("************ Algorithm Completed *****************");
			return true;
		}
		return AlgorithmCompleted;
	}
	
	public boolean checkRootProcessDone() {
		if (doneQ.size() > 0){
			try {
				Message msg = doneQ.take();
				if(msg.getProcessId() == rootProcessID)
					return true;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public boolean checkAllReadyToSend() {
		if(readyToSendQ.size() == numProcesses)
			return true;
		return false;
	}
	
	public void signalSend(){
		synchronized(this){
			readyToSendQ.clear();
		}
	}
	//for debug purposes. Need to get rid of it before submitting.
	public void startSampleTest() {
		while (!isAlgorithmCompleted()) {
			if (checkAllProcessesReady()) {
				startNewRound();
				RoundNo++;
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		HashMap<Integer,ArrayList<Integer>> outputList= new HashMap<Integer,ArrayList<Integer>>();
		BufferedReader inputReader = null;
		try {
			if (args.length > 0 && args != null) {
				inputReader = new BufferedReader(new FileReader(new File(args[0])));
			} else {
				inputReader = new BufferedReader(new FileReader(new File("input2.txt")));
			}

			// Parse the input file. Order of expected input is number of nodes, node ids, leader id, edge weight matrix.
			int n = -1, leaderId = -1;
			String s = inputReader.readLine();
			ArrayList<String> input = new ArrayList<>();
			while (s != null) {
				if (!s.contains("#") && !s.isEmpty()) {
					input.add(s);
				}
				s = inputReader.readLine();
			}

			//Saving number of nodes/processes
			n = new Integer(input.get(0));
			
			//Saving Node/Process Id
			int[] ids = new int[n];			
			String[] processIds = input.get(1).split(" ");
			for (int i = 0; i < n; i++) {
				ids[i] = new Integer(processIds[i]);
			}
			
			//Saving leader Id
			leaderId = new Integer(input.get(2));
			
			//Saving Edge weights
			String[][] neighbours = new String[n][n];
			for (int i = 3; i < n + 3; i++) {
				neighbours[i - 3] = input.get(i).trim().replace("  ", " ").split(" ");
			}

			int MasterProcessID = 0;
			MasterProcess mp = new MasterProcess(MasterProcessID, ids);
			if (n != -1)
				mp.numProcesses = n;
			if (leaderId != -1)
				MasterProcess.rootProcessID = leaderId;
			Processes[] process = new Processes[n];

			for (int i = 0; i < n; i++) {
				process[i] = new Processes(ids[i]);
			}

			for (int i = 0; i < n; i++) {
				ArrayList<Edge> neighbourEdges = new ArrayList<Edge>();
				for (int j = 0; j < n; j++) {
					if (!neighbours[i][j].equalsIgnoreCase("-1")) {
						Edge e = new Edge(process[i], process[j], Integer.parseInt(neighbours[i][j]));
						neighbourEdges.add(e);
					}
				}
				process[i].setEdges(neighbourEdges);

			}

			for (int i = 0; i < n; i++) {
				process[i].setQIn(mp.getInterProcessQ().get(i));
				process[i].setQRound(mp.getProcessRoundQ().get(i));

			}

			Thread[] T = new Thread[n];
			for (int i = 0; i < n; i++) {
				process[i].setQMaster(mp.getMasterQ());
				process[i].setQDone(mp.getDoneQ());
				process[i].setQReadyToSend(mp.getReadyToSendQ());
				T[i] = new Thread(process[i]);
				T[i].start();
			}

			// since code does not stop automatically, need to forcefully stop
			// it.
			while (!mp.isAlgorithmCompleted() && mp.RoundNo < 25) {
				if (mp.checkAllProcessesReady()) {
					mp.startNewRound();
					mp.RoundNo++;
					while(!mp.checkAllReadyToSend());
					mp.signalSend();
				}
			}
			for (int i = 0; i < n; i++) {
				T[i].stop();
			}
			for (int i = 0; i < T.length; i++) {
				try {
					T[i].join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
			//Printing nodes with parent and ditance from root.
			for(int i = 0;i<n;i++){
				if(i!=rootProcessID)
				System.out.println("Process No: "+i+" Parent: " +process[i].getParentID()+ " distance: " + process[i].getDistanceFromRoot());
	
				if(outputList.containsKey(process[i].getParentID())){
					ArrayList<Integer> a = outputList.get(process[i].getParentID());
					a.add(i);
					outputList.replace(process[i].getParentID(), a);
				}else{
					ArrayList<Integer> a= new ArrayList<>();
					a.add(i);
					outputList.put(process[i].getParentID(), a);
				}
			}
			
			//Printing adjacency list
			System.out.println("******************* Adjacency List *******************");
			for (Map.Entry<Integer, ArrayList<Integer>> entry : outputList.entrySet()) {
				if(entry.getKey()>=0){
					System.out.print(entry.getKey());
				    for(Integer i: entry.getValue()){
				    	System.out.print( " -> "+ i);
				    }
				    System.out.println();
				}
			    
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
