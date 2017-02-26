package dc_project1_bellman_ford;

/*
 * Team Members:
 * Sujal Patel (ssp150930)
 * Harshil Shah (hxs155030)
 * Sagar Mehta (sam150930)
 * 
 * This represents a message which has processId, message type, distance from the root, and debug character information.
 */
public class Message {

	// Type of messages
	public enum MessageType {
		READY, NEXT, EXPLORE, ACK, NACK, DONE;
	}
	// Process ID of sender
	private int processID;
	private MessageType mType;
	private int distance;
	private char debugCharacter;
	
	public Message(int PID, MessageType Mtype, int dist, char debugChar){
		this.processID = PID;
		this.mType = Mtype;
		this.distance = dist;
		this.debugCharacter = debugChar;
	}
	// getter/setter functions
	public int getProcessId() {
		return processID;
	}

	public void setProcessId(int processId) {
		this.processID = processId;
	}

	public MessageType getMessageType() {
		return mType;
	}

	public void setMessageType(MessageType mtype) {
		this.mType = mtype;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(int hops) {
		this.distance = hops;
	}

	public char getDebugChar() {
		return debugCharacter;
	}

	public void setDebugChar(char debugchar) {
		this.debugCharacter = debugchar;
	}

	@Override
	public String toString() {
		return "Message [processID=" + processID + ", mType=" + mType + ", distance=" + distance + ", debugCharacter="
				+ debugCharacter + "]";
	}
	// Debug function
	public String debug(){
		return "From: " + this.processID + " What: " + this.mType;
	}
}
