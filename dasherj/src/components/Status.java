package components;


public class Status {
	
	public enum ConnectionType { DISCONNECTED, SERIAL_CONNECTED, TELNET_CONNECTED }
	
	public enum EmulationType { 
		D200 (200), 
		D210 (210),
		// D211 (211),
		// D220 (220),
		// D400 (400),
		;
	
		private int level;
		
		EmulationType( int level ) { this.setLevel(level); }
		public int getLevel() {	return level; }
		public void setLevel(int level) { this.level = level; }
	}
	
	public String serialPort;
	public String remoteHost;
	public boolean logging;
	public boolean control_pressed, shift_pressed, holding, dirty;
	
	public ConnectionType connection;
	
	public EmulationType emulation;
	
	public Status() {
		logging = false;
		connection = ConnectionType.DISCONNECTED;
		emulation = EmulationType.D200;
		dirty = false;
	}

}
