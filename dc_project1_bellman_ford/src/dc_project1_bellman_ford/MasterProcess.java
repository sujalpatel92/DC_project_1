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
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MasterProcess {

	private int MasterProcessId;
	// To signal start of new round to all processes.
	private BlockingQueue<Message> MasterQ, DoneQ;

	private int NumProcesses;

	public static int RootProcess;

	int RoundNo = 0;

	boolean AlgorithmCompleted = false;
	// To send the NEXT message to all the processes.
	private ArrayList<BlockingQueue<Message>> ProcessRoundQ = new ArrayList<BlockingQueue<Message>>();
	// Input Q to which other processes can write.
	private ArrayList<BlockingQueue<Message>> InterProcessQ = new ArrayList<BlockingQueue<Message>>();

	public MasterProcess(int MProcessId, int[] ProcessIds) {
		this.MasterProcessId = MProcessId;
		this.NumProcesses = ProcessIds.length;

		MasterQ = new ArrayBlockingQueue<>(NumProcesses);
		DoneQ = new ArrayBlockingQueue<>(NumProcesses);

		Message ReadyMessage;
		BlockingQueue<Message> ProcessRQ, interProcessQueue;
		for (int i = 0; i < NumProcesses; i++) {
			ReadyMessage = new Message(ProcessIds[i], Message.MessageType.READY, Integer.MIN_VALUE, 'X');
			MasterQ.add(ReadyMessage);
			ProcessRQ = new ArrayBlockingQueue<>(NumProcesses);
			// capacity changed here to solve queue full error temporarily.
			interProcessQueue = new ArrayBlockingQueue<>(NumProcesses * 10);
			ProcessRoundQ.add(ProcessRQ);
			InterProcessQ.add(interProcessQueue);
		}
	}

	public boolean CheckAllReady() {
		if (MasterQ.size() < NumProcesses) {
			return false;
		}
		int Count = 0;
		Message Msg;
		for (int i = 0; i < NumProcesses; i++) {
			try {
				Msg = MasterQ.take();
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
		if (Count == NumProcesses) {
			return true;
		}
		return false;
	}

	public void StartNewRound() {
		Iterator<BlockingQueue<Message>> Iter = ProcessRoundQ.iterator();
		BlockingQueue<Message> Q;
		Message Msg;
		MasterQ.clear();
		synchronized (this) {
			while (Iter.hasNext()) {
				Q = Iter.next();
				Q.clear();
				Msg = new Message(MasterProcessId, Message.MessageType.NEXT, Integer.MIN_VALUE, 'X');
				Q.add(Msg);
			}
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

	public boolean isAlgorithmCompleted() {
		if (checkAllDone())
			return true;
		return AlgorithmCompleted;
	}

	public boolean checkAllDone() {
		if (DoneQ.size() == NumProcesses)
			return true;
		return false;
	}

	public void StartSampleTest() {
		while (!isAlgorithmCompleted()) {
			if (CheckAllReady()) {
				StartNewRound();
				RoundNo++;
			}
		}
	}

	public ArrayList<BlockingQueue<Message>> getInterProcessQ() {
		return InterProcessQ;
	}

	public static void main(String[] args) {
		HashMap<Integer,ArrayList<Integer>> outputList= new HashMap<Integer,ArrayList<Integer>>();
		BufferedReader inputReader = null;
		try {
			if (args.length > 0 && args != null) {
				inputReader = new BufferedReader(new FileReader(new File(args[0])));
			} else {
				inputReader = new BufferedReader(new FileReader(new File("input.txt")));
			}

			int n = -1, leaderId = -1;
			String s = inputReader.readLine();
			ArrayList<String> input = new ArrayList<>();
			while (s != null) {
				if (!s.contains("#") && !s.isEmpty()) {
					input.add(s);
				}
				s = inputReader.readLine();
			}

			n = new Integer(input.get(0));

			int[] ids = new int[n];
			int[][] edgeWeights = new int[n][n];
			String[] processIds = input.get(1).split(" ");
			for (int i = 0; i < n; i++) {
				ids[i] = new Integer(processIds[i]);
			}

			leaderId = new Integer(input.get(2));
			String[][] neighbours = new String[n][n];
			for (int i = 3; i < n + 3; i++) {
				neighbours[i - 3] = input.get(i).trim().replace("  ", " ").split(" ");
			}

			int MasterProcessID = 0;
			// int[] ids = {1,2,3};
			MasterProcess mp = new MasterProcess(MasterProcessID, ids);
			if (n != -1)
				mp.NumProcesses = n;
			if (leaderId != -1)
				mp.RootProcess = leaderId;
			Processes[] process = new Processes[n];

			for (int i = 0; i < n; i++) {
				ArrayList<Edge> neighbourEdges = new ArrayList<Edge>();
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
				T[i] = new Thread(process[i]);
				T[i].start();
			}

			// mp.StartSampleTest();
			// since code does not stop automatically, need to forcefully stop
			// it.
			while (!mp.isAlgorithmCompleted() && mp.RoundNo < 100) {
				if (mp.CheckAllReady()) {
					mp.StartNewRound();
					mp.RoundNo++;
				}
			}
			for (int i = 0; i < n; i++) {
				//T[i].interrupt();
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
			for(int i = 0;i<n;i++){
				System.out.println("Process No. "+i+":");
				process[i].printParentID();
				if(outputList.containsKey(process[i].getParentID())){
					ArrayList<Integer> a = outputList.get(process[i].getParentID());
					a.add(i);
					outputList.replace(process[i].getParentID(), a);
				}else{
					ArrayList<Integer> a= new ArrayList<>();
					a.add(i);
					outputList.put(process[i].getParentID(), a);
				}
				process[i].printChildID();
			}
			

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
