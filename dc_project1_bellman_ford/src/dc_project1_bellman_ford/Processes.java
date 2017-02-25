package dc_project1_bellman_ford;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;

import dc_project1_bellman_ford.Message.MessageType;

public class Processes implements Runnable {

	// process id
	private int ProcessId;
	// Root of the tree
	private Processes Root;
	/*
	 * QMaster -> write READY message to this Q. QRound -> Receive NEXT signal
	 * from Master process. QIn -> Interprocess Q. QDone -> To signal completion
	 * of tree building at your level. Work in Progress. Code still doesn't stop
	 * properly.
	 */
	private BlockingQueue<Message> QMaster, QRound, QIn, QDone;

	private ArrayList<Edge> Edges;
	private int DistanceFromRoot;
	private int ExploreCount;
	private boolean isRoot;
	private int ACKCount;
	private int NACKCount;
	private int doneCount;
	private boolean ExploreCompleted, exploreToSend, firstRound;
	// List in which messages to send in next round are populated.
	private HashMap<Processes, Message> SendList = new HashMap<Processes, Message>();
	// List to keep check of child nodes in shortest path tree.
	private ArrayList<Integer> childID = new ArrayList<Integer>();
	private ArrayList<Integer> exploreSenderID = new ArrayList<Integer>();
	private int ParentID;
	int RoundNo = 0;
	private boolean addReadyMsg = false;

	public Processes(int processId) {
		this.ProcessId = processId;
		Edges = new ArrayList<Edge>();
		ExploreCompleted = false;
		this.ExploreCount = 0;
		this.ACKCount = 0;
		this.NACKCount = 0;
		this.doneCount = 0;
	}

	public void Initialize() {
		Message Msg;
		int Distance;
		this.ExploreCount = 0;
		this.exploreToSend = false;
		this.ParentID = Integer.MIN_VALUE;
		this.firstRound = true;
		if (this.ProcessId == MasterProcess.RootProcess) {
			this.DistanceFromRoot = 0;
			this.isRoot = true;
		} else {
			this.DistanceFromRoot = Integer.MAX_VALUE;
			this.isRoot = false;
		}
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

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Initialize();
		while (true) {
			Message Msg = null;
			try {
				// check for the start of next round
				while (!(QRound.size() > 0))
					;
				if (QRound.peek() != null)
					Msg = QRound.take();
				if (Msg.getMtype() == Message.MessageType.NEXT) {
					RoundNo++;
					String printStr = "Process: " + ProcessId + " Round: " + RoundNo + " EXPLORE: " + this.ExploreCount
							+ " ACK: " + this.ACKCount + " NACK: " + this.NACKCount + " DONE: " + this.doneCount;
					System.out.println(printStr);
					this.addReadyMsg = false;
					// send all the messages outwards at the start. Had to put
					// it up
					// here to solve certain synchronization issues.
					if (SendList.size() > 0) {
						Iterator iter = SendList.entrySet().iterator();
						while (iter.hasNext()) {
							Map.Entry<Processes, Message> pair = (Map.Entry<Processes, Message>) iter.next();
							Processes toSend = pair.getKey();
							Message toSendMsg = pair.getValue();
							if (toSendMsg.getMtype() == Message.MessageType.EXPLORE)
								this.ExploreCount++;
							toSend.writeToQIn(toSendMsg);
							System.out.println("********** To: " + toSend.getProcessId() + " " + toSendMsg.debug());
							iter.remove();
						}
					}
					if (this.isRoot && this.firstRound) {
						Processes neighbourProcess;
						Iterator<Edge> Iter = this.Edges.iterator();
						while (Iter.hasNext()) {
							Edge E = Iter.next();
							neighbourProcess = E.getNeighbour(this);
							int Distance = DistanceFromRoot + E.getWeight();
							Msg = new Message(this.ProcessId, Message.MessageType.EXPLORE, Distance, 'I');
							SendList.put(neighbourProcess, Msg);
						}
						this.firstRound = false;
					} else
						this.firstRound = false;
					while (QIn.size() > 0) {
						try {
							Msg = QIn.take();
							if (Msg.getMtype() == Message.MessageType.EXPLORE) {
								// Relaxation step for Bellman-Ford Algorithm
								exploreSenderID.add(Msg.getProcessId());
								if (this.DistanceFromRoot > (int) Msg.getHops()) {
									this.DistanceFromRoot = (int) Msg.getHops();
									this.ParentID = Msg.getProcessId();
									Iterator<Edge> Iter = this.Edges.iterator();
									Processes neighbourProcess;
									int Distance;
									while (Iter.hasNext()) {
										Edge E = Iter.next();
										neighbourProcess = E.getNeighbour(this);
										if (neighbourProcess.getProcessId() == this.ParentID
												|| neighbourProcess.getProcessId() == MasterProcess.RootProcess)
											continue;
										Distance = DistanceFromRoot + E.getWeight();
										Msg = new Message(this.ProcessId, Message.MessageType.EXPLORE, Distance, 'I');
										SendList.put(neighbourProcess, Msg);
										this.exploreToSend = true;
										this.ExploreCompleted = false;
									}
									// In case the node has not more outgoing
									// neighbors
									// to send to
									if (!this.exploreToSend && ExploreCount == 0) {
										this.ExploreCompleted = true;
										Msg = new Message(this.ProcessId, Message.MessageType.ACK, Integer.MAX_VALUE,
												'O');
										Message msg2 = new Message(this.ProcessId, Message.MessageType.DONE,
												Integer.MAX_VALUE, 'D');
										Processes ngbhr;
										Iterator<Edge> Iter2 = this.Edges.iterator();
										while (Iter2.hasNext()) {
											Edge E = Iter2.next();
											ngbhr = E.getNeighbour(this);
											if (ngbhr.getProcessId() == ParentID) {
												SendList.put(ngbhr, Msg);
												SendList.put(ngbhr, msg2);
												for (int i = 0; i < exploreSenderID.size(); i++) {
													if (exploreSenderID.get(i) == ParentID) {
														exploreSenderID.remove(i);
													}
												}
											}
										}
									}
								}
								// NACK for not helpful relaxation
								else {
									int senderID = Msg.getProcessId();
									Msg = new Message(this.ProcessId, Message.MessageType.NACK, Integer.MIN_VALUE, 'O');
									Processes ngbhr;
									Iterator<Edge> Iter = this.Edges.iterator();
									while (Iter.hasNext()) {
										Edge E = Iter.next();
										ngbhr = E.getNeighbour(this);
										if (ngbhr.getProcessId() == senderID) {
											SendList.put(ngbhr, Msg);
											for (int i = 0; i < exploreSenderID.size(); i++) {
												if (exploreSenderID.get(i) == senderID) {
													exploreSenderID.remove(i);
												}
											}
										}
									}
								}
							}
							// receiving DONE
							if (Msg.getMtype() == Message.MessageType.DONE && !ExploreCompleted) {
								this.doneCount++;
								if ((this.doneCount + this.NACKCount) == this.ExploreCount) {
									this.ExploreCompleted = true;
									Msg = new Message(this.ProcessId, Message.MessageType.DONE, Integer.MAX_VALUE, 'O');
									Processes ngbhr;
									Iterator<Edge> Iter = this.Edges.iterator();
									while (Iter.hasNext()) {
										Edge E = Iter.next();
										ngbhr = E.getNeighbour(this);
										if (ngbhr.getProcessId() == ParentID) {
											SendList.put(ngbhr, Msg);
											for (int i = 0; i < exploreSenderID.size(); i++) {
												if (exploreSenderID.get(i) == ParentID) {
													exploreSenderID.remove(i);
												}
											}
										}
									}
									if (exploreSenderID.size() > 0) {
										for (int id : exploreSenderID) {
											Msg = new Message(this.ProcessId, Message.MessageType.NACK,
													Integer.MIN_VALUE, 'O');
											Processes ngbhr2;
											Iterator<Edge> Iter2 = this.Edges.iterator();
											while (Iter2.hasNext()) {
												Edge E2 = Iter2.next();
												ngbhr2 = E2.getNeighbour(this);
												if (ngbhr2.getProcessId() == id) {
													SendList.put(ngbhr2, Msg);
												}
											}
										}
										exploreSenderID.clear();
									}
								}
							}
							// receiving ACK
							if (Msg.getMtype() == Message.MessageType.ACK && !ExploreCompleted) {
								this.ACKCount++;
								childID.add(Msg.getProcessId());
								// if ((this.ACKCount + this.NACKCount) ==
								// this.ExploreCount) {
								// this.ExploreCompleted = true;
								// Msg = new Message(this.ProcessId,
								// Message.MessageType.ACK, Integer.MAX_VALUE,
								// 'O');
								// Processes ngbhr;
								// Iterator<Edge> Iter = this.Edges.iterator();
								// while (Iter.hasNext()) {
								// Edge E = Iter.next();
								// ngbhr = E.getNeighbour(this);
								// if (ngbhr.getProcessId() == ParentID) {
								// SendList.put(ngbhr, Msg);
								// }
								// }
								// if (exploreSenderID.size() > 0) {
								// for (int id : exploreSenderID) {
								// Msg = new Message(this.ProcessId,
								// Message.MessageType.NACK, Integer.MIN_VALUE,
								// 'O');
								// Processes ngbhr2;
								// Iterator<Edge> Iter2 = this.Edges.iterator();
								// while (Iter2.hasNext()) {
								// Edge E2 = Iter2.next();
								// ngbhr2 = E2.getNeighbour(this);
								// if (ngbhr2.getProcessId() == id) {
								// SendList.put(ngbhr2, Msg);
								// }
								// }
								// }
								// exploreSenderID.clear();
								// }
								// Msg = new Message(this.ProcessId,
								// Message.MessageType.DONE, Integer.MIN_VALUE,
								// 'D');
								// synchronized (this) {
								// QDone.add(Msg);
								// }
								// }
							}
							// receiving NACK
							if (Msg.getMtype() == Message.MessageType.NACK && !ExploreCompleted) {
								this.NACKCount++;
								if ((this.doneCount + this.NACKCount) == this.ExploreCount) {
									this.ExploreCompleted = true;
									Msg = new Message(this.ProcessId, Message.MessageType.DONE, Integer.MAX_VALUE, 'O');
									Processes ngbhr;
									Iterator<Edge> Iter = this.Edges.iterator();
									while (Iter.hasNext()) {
										Edge E = Iter.next();
										ngbhr = E.getNeighbour(this);
										if (ngbhr.getProcessId() == ParentID) {
											SendList.put(ngbhr, Msg);
											for (int i = 0; i < exploreSenderID.size(); i++) {
												if (exploreSenderID.get(i) == ParentID) {
													exploreSenderID.remove(i);
												}
											}
										}
									}
									if (exploreSenderID.size() > 0) {
										for (int id : exploreSenderID) {
											Msg = new Message(this.ProcessId, Message.MessageType.NACK,
													Integer.MIN_VALUE, 'O');
											Processes ngbhr2;
											Iterator<Edge> Iter2 = this.Edges.iterator();
											while (Iter2.hasNext()) {
												Edge E = Iter2.next();
												ngbhr2 = E.getNeighbour(this);
												if (ngbhr2.getProcessId() == id) {
													SendList.put(ngbhr2, Msg);
												}
											}
										}
										exploreSenderID.clear();
									}
									// Msg = new Message(this.ProcessId,
									// Message.MessageType.DONE,
									// Integer.MIN_VALUE, 'D');
									// synchronized (this) {
									// QDone.add(Msg);
									// }
								}
							}
							if (this.isRoot && this.ExploreCompleted) {
								Msg = new Message(this.ProcessId, Message.MessageType.DONE, Integer.MAX_VALUE, 'D');
								QDone.add(Msg);
							}

						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

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
