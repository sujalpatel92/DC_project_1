package dc_project1_bellman_ford;

/*
 * Team Members:
 * Sujal Patel (ssp150930)
 * Harshil Shah (hxs155030)
 * Sagar Mehta (sam150930)
 * 
 * This represents a message which has processId, message type, distance from the root, and direction
 * from which it is coming.
 */
public class Message {

	//type of messages
	public enum MessageType {
		LEADER, READY, NEXT, IN, OUT, EXPLORE, ACK, NACK, DONE;
	}
	private int ProcessId;
	private MessageType Mtype;
	private double Distance;
	private char FromDirection;
	private int RootId;
	
	public Message(int PID, MessageType Mtype, double Hops, char FrmD){
		this.ProcessId = PID;
		this.Mtype = Mtype;
		this.Distance = Hops;
		this.FromDirection = FrmD;
	}
	
	public int getProcessId() {
		return ProcessId;
	}

	public void setProcessId(int processId) {
		this.ProcessId = processId;
	}

	public MessageType getMtype() {
		return Mtype;
	}

	public void setMtype(MessageType mtype) {
		this.Mtype = mtype;
	}

	public double getHops() {
		return Distance;
	}

	public void setHops(double hops) {
		this.Distance = hops;
	}
	
	public void setHops(int hops) {
		this.Distance = hops;
	}

	public char getFromDirection() {
		return FromDirection;
	}

	public void setFromDirection(char fromDirection) {
		this.FromDirection = fromDirection;
	}
	
	public int getRootId() {
		return RootId;
	}

	public void setRootId(int rootId) {
		this.RootId = rootId;
	}

	public String toString() {
		return "Process ID:" + this.ProcessId + " Message Type:" + this.Mtype + " Hops:" + this.Distance + " From Direction:" + this.FromDirection + " RootID:" + this.RootId;
	}
	
	public String debug(){
		return "From: " + this.ProcessId + " What: " + this.Mtype;
	}
}
