package dc_project1_bellman_ford;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.*;

import dc_project1_bellman_ford.Message.MessageType;

public class Processes implements Runnable {

	// process id
	private int ProcessId;
	//Root of the tree
	private Processes Root;
	/*
	 * QMaster -> write READY message to this Q.
	 * QRound -> Receive NEXT signal from Master process.
	 * QIn -> Interprocess Q.
	*/
	private BlockingQueue<Message> QMaster, QRound, QIn;
	
	
	private ArrayList<Edge> Edges;
	private int DistanceFromRoot;
	private int ExploreCount;
	private int ACKCount;
	private int NACKCount;
	private boolean ExploreCompleted, exploreToSend;
	// List in which messages to send in next round are populated.
	private HashMap<Processes, Message> SendList = new HashMap<Processes, Message>();
	// List to keep check of child nodes in shortest path tree.
	private ArrayList<Integer> childID = new ArrayList<Integer>();
	private int ParentID;
	int RoundNo = 0;

	public Processes(int processId) {
		this.ProcessId = processId;
		Edges = new ArrayList<Edge>();
		ExploreCompleted = false;
		this.ExploreCount = 0;
		this.ACKCount = 0;
		this.NACKCount = 0;
	}

	public void Initialize() {
		Message Msg;
		int Distance;
		this.ExploreCount = 0;
		this.exploreToSend = false;
		this.ParentID = Integer.MIN_VALUE;
		if (this.ProcessId == MasterProcess.RootProcess) {
			Processes neighbourProcess;
			this.DistanceFromRoot = 0;
			Iterator<Edge> Iter = this.Edges.iterator();
			while (Iter.hasNext()) {
				Edge E = Iter.next();
				neighbourProcess = E.getNeighbour(this);
				Distance = DistanceFromRoot + E.getWeight();
				Msg = new Message(this.ProcessId, Message.MessageType.EXPLORE, Distance, 'I');
				SendList.put(neighbourProcess, Msg);
			}
		}
		else{
			this.DistanceFromRoot = Integer.MAX_VALUE;
		}
	}

	public void setQMaster(BlockingQueue<Message> qMaster) {
		QMaster = qMaster;
	}

	public void setQRound(BlockingQueue<Message> qRound) {
		QRound = qRound;
	}

	public void setQIn(BlockingQueue<Message> qIn) {
		QIn = qIn;
	}
	
	public BlockingQueue<Message> getQMaster() {
		return QMaster;
	}

	public BlockingQueue<Message> getQRound() {
		return QRound;
	}

	public BlockingQueue<Message> getQIn() {
		return QIn;
	}

	public void writeToQIn(Message msg){
		QIn.add(msg);
	}

	public int getProcessId() {
		return ProcessId;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (true) {
			Message Msg = null;
			try {
				Msg = QRound.take();
				if (Msg.getMtype() == Message.MessageType.NEXT) {
					RoundNo++;
					System.out.println("Process:" + ProcessId + " Round:" + RoundNo);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			while(QIn.size() > 0){
				try {
					Msg = QIn.take();
					if(Msg.getMtype() == Message.MessageType.EXPLORE){
						//Relaxation
						if(this.DistanceFromRoot > (int)Msg.getHops()){
							this.DistanceFromRoot = (int) Msg.getHops();
							this.ParentID = Msg.getProcessId();
							Iterator<Edge> Iter = this.Edges.iterator();
							Processes neighbourProcess;
							int Distance;
							while (Iter.hasNext()) {
								Edge E = Iter.next();
								neighbourProcess = E.getNeighbour(this);
								if(neighbourProcess.getProcessId() == this.ParentID)
									continue;
								Distance = DistanceFromRoot + E.getWeight();
								Msg = new Message(this.ProcessId, Message.MessageType.EXPLORE, Distance, 'I');
								SendList.put(neighbourProcess, Msg);
								this.exploreToSend = true;
							}
							if(!this.exploreToSend){
								this.ExploreCompleted = true;
								Msg = new Message(this.ProcessId, Message.MessageType.ACK, Integer.MAX_VALUE, 'O');
								Processes ngbhr;
								Iterator<Edge> Iter2 = this.Edges.iterator();
								while(Iter2.hasNext()){
									Edge E = Iter2.next();
									ngbhr = E.getNeighbour(this);
									if(ngbhr.getProcessId() == ParentID){
										SendList.put(ngbhr, Msg);
									}
								}
							}
						}
						else{
							int senderID = Msg.getProcessId();
							Msg = new Message(this.ProcessId, Message.MessageType.NACK, Integer.MIN_VALUE, 'O');
							Processes ngbhr;
							Iterator<Edge> Iter = this.Edges.iterator();
							while(Iter.hasNext()){
								Edge E = Iter.next();
								ngbhr = E.getNeighbour(this);
								if(ngbhr.getProcessId() == senderID){
									SendList.put(ngbhr, Msg);
								}
							}
						}
					}
					if(Msg.getMtype() == Message.MessageType.ACK){
						this.ACKCount++;
						childID.add(Msg.getProcessId());
						if((this.ACKCount + this.NACKCount) == this.ExploreCount){
							this.ExploreCompleted = true;
							Msg = new Message(this.ProcessId, Message.MessageType.ACK, Integer.MAX_VALUE, 'O');
							Processes ngbhr;
							Iterator<Edge> Iter = this.Edges.iterator();
							while(Iter.hasNext()){
								Edge E = Iter.next();
								ngbhr = E.getNeighbour(this);
								if(ngbhr.getProcessId() == ParentID){
									SendList.put(ngbhr, Msg);
								}
							}
						}
					}
					if(Msg.getMtype() == Message.MessageType.NACK){
						this.NACKCount++;
						if((this.ACKCount + this.NACKCount) == this.ExploreCount){
							this.ExploreCompleted = true;
							Msg = new Message(this.ProcessId, Message.MessageType.ACK, Integer.MAX_VALUE, 'O');
							Processes ngbhr;
							Iterator<Edge> Iter = this.Edges.iterator();
							while(Iter.hasNext()){
								Edge E = Iter.next();
								ngbhr = E.getNeighbour(this);
								if(ngbhr.getProcessId() == ParentID){
									SendList.put(ngbhr, Msg);
								}
							}
						}
					}
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
