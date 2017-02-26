package dc_project1_bellman_ford;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;


/**
 * Team Members:
 * Sujal Patel (ssp150930)
 * Harshil Shah (hxs155030)
 * Sagar Mehta (sam150930)
 * 
 * This is the individual process class which runs bellman ford algorithm. 
 */
public class Processes implements Runnable {

	// process id
	private int ProcessId;
	/*
	 * QMaster -> write READY message to this Q. 
	 * QRound -> Receive NEXT signal from Master process. 
	 * QIn -> Interprocess Q. 
	 * QDone -> To signal completion of tree building at your level. Work in Progress. Code still doesn't stop properly.
	 * QReadyToSend -> Write in this Queue Ready to let Master know you want to send the messages to link now.
	 */
	private BlockingQueue<Message> QMaster, QRound, QIn, QDone, QReadyToSend;
	private enum state{
		NONE, EXPLORE, NACK, DONE;
	}
	private ArrayList<Edge> Edges;
	private int DistanceFromRoot;
	private int ExploreCount;
	private boolean isRoot, isLeaf;
	private int ACKCount;
	private int NACKCount;
	private int doneCount;
	private boolean ExploreCompleted, exploreToSend, firstRound;
	// List in which messages to send in next round are populated.
	private HashMap<Processes, Message> SendList = new HashMap<Processes, Message>();
	private HashMap<Integer, state> stateList = new HashMap<Integer, state>();
	// List to keep check of child nodes in shortest path tree.
	private ArrayList<Integer> childID = new ArrayList<Integer>();
	private ArrayList<Integer> exploreIDs = new ArrayList<Integer>();
	
	private int ParentID;
	int RoundNo = 0;
	private boolean addReadyMsg = false;
	private boolean debugStatements = false;
	
	public Processes(int processId) {
		this.ProcessId = processId;
		Edges = new ArrayList<Edge>();
		ExploreCompleted = false;
		this.ExploreCount = 0;
		this.ACKCount = 0;
		this.NACKCount = 0;
		this.doneCount = 0;
		this.debugStatements = false;
	}

	public int getDistanceFromRoot() {
		return DistanceFromRoot;
	}

	public void setDistanceFromRoot(int distanceFromRoot) {
		DistanceFromRoot = distanceFromRoot;
	}

	public void Initialize() {
		
		this.ExploreCount = 0;
		this.exploreToSend = false;
		this.ParentID = Integer.MIN_VALUE;
		this.firstRound = true;
		this.debugStatements = true;
		this.isLeaf = false;
		if (this.ProcessId == MasterProcess.rootProcessID) {
			this.DistanceFromRoot = 0;
			this.isRoot = true;
		} else {
			this.DistanceFromRoot = Integer.MAX_VALUE;
			this.isRoot = false;
		}
		//initStateList();
	}

	public ArrayList<Edge> getEdges() {
		return Edges;
	}

	public void setEdges(ArrayList<Edge> edges) {
		Edges = edges;
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

	public BlockingQueue<Message> getQDone() {
		return QDone;
	}

	public void setQDone(BlockingQueue<Message> qDone) {
		QDone = qDone;
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

	public void writeToQIn(Message msg) {
		QIn.add(msg);
	}

	public int getProcessId() {
		return ProcessId;
	}

	public int getParentID() {
		return ParentID;
	}

	public void setParentID(int parentID) {
		ParentID = parentID;
	}

	public BlockingQueue<Message> getQReadyToSend() {
		return QReadyToSend;
	}

	public void setQReadyToSend(BlockingQueue<Message> qReadyToSend) {
		QReadyToSend = qReadyToSend;
	}

	public void addEdge(Edge e) {
		this.Edges.add(e);
	}

	public void printParentID() {
		System.out.println(this.ParentID);
	}

	public void printChildID() {
		for (int i = 0; i < this.childID.size(); i++) {
			System.out.println(childID.get(i));
		}
	}
	
	public void initStateList()
	{
		for(Edge e : this.Edges){
			int nbrID = e.getNeighbour(this).getProcessId();
			state s = state.NONE;
			stateList.put(nbrID, s);
		}
	}
	
	public void resetStateList(state s)
	{
		for(Entry<Integer, state>e : this.stateList.entrySet()){
			
			e.setValue(s);
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Initialize();
		while (true) {
			Message Msg = null;
			try {
				// check for the start of next round
				while (!(QRound.size() > 0));
				if (QRound.peek() != null)
					Msg = QRound.take();
				if (Msg.getMtype() == Message.MessageType.NEXT) {
					
					RoundNo++;
					
					this.addReadyMsg = false;
					
					if (this.isRoot && this.firstRound) {
						Processes neighbourProcess;
						Iterator<Edge> Iter = this.Edges.iterator();
						while (Iter.hasNext()) {
							Edge E = Iter.next();
							neighbourProcess = E.getNeighbour(this);
							int Distance = DistanceFromRoot + E.getWeight();
							Msg = new Message(this.ProcessId, Message.MessageType.EXPLORE, Distance, 'I');
							SendList.put(neighbourProcess, Msg);
							stateList.put(neighbourProcess.getProcessId(), state.EXPLORE);
						}
						this.firstRound = false;
					} else
						this.firstRound = false;
					this.exploreToSend = false;
					while (QIn.size() > 0) {
						
						try {
							Msg = QIn.take();
							if (Msg.getMtype() == Message.MessageType.EXPLORE) {
								// Relaxation step for Bellman-Ford Algorithm
								exploreIDs.add(Msg.getProcessId());
								if (this.DistanceFromRoot > (int) Msg.getHops()) {
									this.DistanceFromRoot = (int) Msg.getHops();
									this.ParentID = Msg.getProcessId();
									
//									Iterator<Edge> Iter = this.Edges.iterator();
//									Processes neighbourProcess;
//									int Distance;
//									while (Iter.hasNext()) {
//										Edge E = Iter.next();
//										neighbourProcess = E.getNeighbour(this);
//										if (neighbourProcess.getProcessId() == this.ParentID
//												|| neighbourProcess.getProcessId() == MasterProcess.rootProcessID)
//											continue;
//										Distance = DistanceFromRoot + E.getWeight();
//										Msg = new Message(this.ProcessId, Message.MessageType.EXPLORE, Distance, 'I');
//										SendList.put(neighbourProcess, Msg);
									this.exploreToSend = true;
									this.ExploreCompleted = false;
//									}
									// In case the node has not more outgoing
									// neighbors
									// to send to
//									if (!this.exploreToSend && ExploreCount == 0) {
//										this.ExploreCompleted = true;
//										Msg = new Message(this.ProcessId, Message.MessageType.ACK, Integer.MAX_VALUE,
//												'O');
//										Message msg2 = new Message(this.ProcessId, Message.MessageType.DONE,
//												Integer.MAX_VALUE, 'D');
//										Processes ngbhr;
//										Iterator<Edge> Iter2 = this.Edges.iterator();
//										while (Iter2.hasNext()) {
//											Edge E = Iter2.next();
//											ngbhr = E.getNeighbour(this);
//											if (ngbhr.getProcessId() == ParentID) {
//												SendList.put(ngbhr, Msg);
//												SendList.put(ngbhr, msg2);
//											}
//										}
//									}
								}
								// NACK for not helpful relaxation
//								else {
//									int senderID = Msg.getProcessId();
//									Msg = new Message(this.ProcessId, Message.MessageType.NACK, Integer.MIN_VALUE, 'O');
//									Processes ngbhr;
//									Iterator<Edge> Iter = this.Edges.iterator();
//									while (Iter.hasNext()) {
//										Edge E = Iter.next();
//										ngbhr = E.getNeighbour(this);
//										if (ngbhr.getProcessId() == senderID) {
//											SendList.put(ngbhr, Msg);
//										}
//									}
//								}
							}
							// receiving DONE
							if (Msg.getMtype() == Message.MessageType.DONE && !ExploreCompleted) {
//								this.doneCount++;
//								if ((this.doneCount + this.NACKCount) == this.ExploreCount) {
//									this.ExploreCompleted = true;
//									Msg = new Message(this.ProcessId, Message.MessageType.DONE, Integer.MAX_VALUE, 'O');
//									Processes ngbhr;
//									Iterator<Edge> Iter = this.Edges.iterator();
//									while (Iter.hasNext()) {
//										Edge E = Iter.next();
//										ngbhr = E.getNeighbour(this);
//										if (ngbhr.getProcessId() == ParentID) {
//											SendList.put(ngbhr, Msg);
//										}
//									}
//								}
								int nbrid = Msg.getProcessId();
								this.stateList.replace(nbrid, state.DONE);
								
							}
							// receiving ACK
//							if (Msg.getMtype() == Message.MessageType.ACK && !ExploreCompleted) {
//								this.ACKCount++;
//								childID.add(Msg.getProcessId());
//							}
							// receiving NACK
							if (Msg.getMtype() == Message.MessageType.NACK && !ExploreCompleted) {
//								this.NACKCount++;
//								if ((this.doneCount + this.NACKCount) == this.ExploreCount) {
//									this.ExploreCompleted = true;
//									Msg = new Message(this.ProcessId, Message.MessageType.DONE, Integer.MAX_VALUE, 'O');
//									Processes ngbhr;
//									Iterator<Edge> Iter = this.Edges.iterator();
//									while (Iter.hasNext()) {
//										Edge E = Iter.next();
//										ngbhr = E.getNeighbour(this);
//										if (ngbhr.getProcessId() == ParentID) {
//											SendList.put(ngbhr, Msg);
//										}
//									}
//								}
								int nbrid = Msg.getProcessId();
								this.stateList.replace(nbrid, state.NACK);
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					// Now processing messages
					//sending nacks to unhelpful explore
					if(this.exploreIDs.size() > 0){
						for(int id : this.exploreIDs){
							if(id != this.ParentID){
								Msg = new Message(this.ProcessId, Message.MessageType.NACK, Integer.MAX_VALUE, 'N');
								Processes ngbhr;
								Iterator<Edge> Iter = this.Edges.iterator();
								while (Iter.hasNext()) {
									Edge E = Iter.next();
									ngbhr = E.getNeighbour(this);
									if (ngbhr.getProcessId() == id) {
										SendList.put(ngbhr, Msg);
									}
								}
							}
						}
					}
					//Send Explores
					if(this.exploreToSend){
						
						Processes ngbhr;
						Iterator<Edge> Iter = this.Edges.iterator();
						if(stateList.size() > 0)
							stateList.clear();
						while (Iter.hasNext()) {
							Edge E = Iter.next();
							Msg = new Message(this.ProcessId, Message.MessageType.EXPLORE, (this.DistanceFromRoot + E.getWeight()) , 'E');
							ngbhr = E.getNeighbour(this);
							if (ngbhr.getProcessId() != this.ParentID && ngbhr.getProcessId() != MasterProcess.rootProcessID) {
								SendList.put(ngbhr, Msg);
								stateList.put(ngbhr.getProcessId(), state.EXPLORE);
							}
						}
//						resetStateList(state.EXPLORE);
					}
					//send Done
					boolean doneFlag = false;
					if(this.stateList.size() == 0 && this.ParentID != Integer.MIN_VALUE)
						doneFlag = true;
					else{
						for(Entry<Integer, state>e : this.stateList.entrySet()){
							if((e.getValue() == state.NACK || e.getValue() == state.DONE))
							{
								doneFlag = true;
							}
							else
							{
								doneFlag = false;
								break;
							}
						}
					}
					
					if(doneFlag){
						if(this.isRoot){
							Msg = new Message(this.ProcessId, Message.MessageType.DONE, Integer.MAX_VALUE, 'D');
							QDone.add(Msg);
						}
						Processes ngbhr;
						Iterator<Edge> Iter = this.Edges.iterator();
						while (Iter.hasNext()) {
							Edge E = Iter.next();
							Msg = new Message(this.ProcessId, Message.MessageType.DONE, Integer.MIN_VALUE , 'D');
							ngbhr = E.getNeighbour(this);
							if (ngbhr.getProcessId() == this.ParentID) {
								SendList.put(ngbhr, Msg);
							}
						}
					}
					//Signal Ready to send and wait.
					Message readyToSendMsg = new Message(this.ProcessId, Message.MessageType.READY, Integer.MIN_VALUE, 'R');
					synchronized (this) {
						QReadyToSend.add(readyToSendMsg);
					}
					while(QReadyToSend.size() != 0);
					// send all the messages outwards at the start. Had to put
					// it up
					// here to solve certain synchronization issues.
					if (SendList.size() > 0) {
						Iterator<Entry<Processes, Message>> iter = SendList.entrySet().iterator();
						while (iter.hasNext()) {
							Map.Entry<Processes, Message> pair = (Map.Entry<Processes, Message>) iter.next();
							Processes toSend = pair.getKey();
							Message toSendMsg = pair.getValue();
//							if (toSendMsg.getMtype() == Message.MessageType.EXPLORE)
//								this.ExploreCount++;
							toSend.writeToQIn(toSendMsg);
							if(this.debugStatements)
								System.out.println("*Round NO.: " + this.RoundNo + " To: " + toSend.getProcessId() + " " + toSendMsg.debug() + "\n");
							iter.remove();
						}
					}
//					if(this.debugStatements){
//						String printStr = "Process: " + ProcessId + " Round: " + RoundNo + " EXPLORE: " + this.ExploreCount
//								+ " ACK: " + this.ACKCount + " NACK: " + this.NACKCount + " DONE: " + this.doneCount;
//						System.out.println(printStr);	
//					}
					//Exit
//					if (this.isRoot && this.ExploreCompleted) {
//						Msg = new Message(this.ProcessId, Message.MessageType.DONE, Integer.MAX_VALUE, 'D');
//						QDone.add(Msg);
//					}
					
					// Signal READY for next round
					Message readyMSG = new Message(this.ProcessId, Message.MessageType.READY, Integer.MIN_VALUE, 'R');
					synchronized (this) {
						if (!this.addReadyMsg) {
							QMaster.add(readyMSG);
							this.addReadyMsg = true;
						}

					}

				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// Check for all incoming messages.

		}
	}

}
