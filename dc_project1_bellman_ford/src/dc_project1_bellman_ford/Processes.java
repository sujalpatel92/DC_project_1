package dc_project1_bellman_ford;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.*;

import dc_project1_bellman_ford.Message.MessageType;

public class Processes implements Runnable{

	//process id
	private int ProcessId;
	private Processes Root;
	//Queues to be written to
	private BlockingQueue<Message> QMaster, QRound, QIn;
	private ArrayList<Edge> Edges;	
	private int DistanceToRoot;
	private int DistanceFromRoot;
	private int ExploreCount;
	private int ACKCount;
	private int NACKCount;
	private boolean ExploreCompleted;
	//List in which messages to send in next round are populated.
	private ArrayList<Message> SendList = new ArrayList<Message>();
	//List to keep check of child nodes in shortest path tree.
	private ArrayList<Processes> Child = new ArrayList<Processes>();
	private Processes Parent;
	
	
	public Processes(int processId) {
		this.ProcessId = processId;
		Edges = new ArrayList<Edge>();
		ExploreCompleted = false;
		this.ExploreCount = 0;
		this.ACKCount = 0;
		this.NACKCount = 0;
	}
	
	public void Initialize(){
		Message Msg;
		int Distance;
		this.ExploreCount = this.Edges.size() - 1;
		if(this == MasterProcess.RootProcess){
			Iterator<Edge> Iter = this.Edges.iterator();
			while(Iter.hasNext()){
				Edge E = Iter.next();
				Distance = DistanceFromRoot + E.getWeight();
				Msg = new Message(this.ProcessId, Message.MessageType.EXPLORE, Distance, 'I');
			}
		}
	}

	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
