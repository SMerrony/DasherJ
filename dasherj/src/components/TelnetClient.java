package components;

import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.io.*;

public class TelnetClient {
	
	Socket sock;
	Thread telnetListenerThread, telnetWriterThread;
	public boolean connected;
	
	static final byte CMD_SE = (byte) 240;
	static final byte CMD_NOP = (byte) 241;
	static final byte CMD_DM = (byte) 242;
	static final byte CMD_BRK = (byte) 243;
	static final byte CMD_IP = (byte) 244;
	static final byte CMD_AO = (byte) 245;
	static final byte CMD_AYT = (byte) 246;
	static final byte CMD_EC = (byte) 247;
	static final byte CMD_EL = (byte) 248;
	static final byte CMD_GA = (byte) 249;
	static final byte CMD_SB = (byte) 250;
	static final byte CMD_WILL = (byte) 251;
	static final byte CMD_WONT = (byte) 252;
	static final byte CMD_DO = (byte) 253;
	static final byte CMD_DONT = (byte) 254;
	static final byte CMD_IAC = (byte) 255;
	
	static final byte OPT_BIN = (byte) 0;
	static final byte OPT_ECHO = (byte) 1;
	static final byte OPT_RECON = (byte) 2;
	static final byte OPT_SGA = (byte) 3;
	static final byte OPT_STATUS = (byte) 5;
	static final byte OPT_COLS = (byte) 8;
	static final byte OPT_ROWS = (byte) 9;
	static final byte OPT_EASCII = (byte) 17;
	static final byte OPT_LOGOUT = (byte) 18;
	static final byte OPT_TTYPE = (byte) 24;
	static final byte OPT_NAWS = (byte) 31; // window size
	static final byte OPT_TSPEED = (byte) 32;
	static final byte OPT_XDISP = (byte) 35;
	static final byte OPT_NEWENV = (byte) 39;
	
	static final byte WILL_NAWS[] = {CMD_IAC, CMD_WILL, OPT_NAWS};
	static final byte WILL_TSPEED[] = {CMD_IAC, CMD_WILL, OPT_TSPEED};
	static final byte WILL_TTYPE[] = {CMD_IAC, CMD_WILL, OPT_TTYPE};
	static final byte DO_ECHO[] = {CMD_IAC, CMD_DO, OPT_ECHO};
	
	// the shared queues
	BlockingQueue<Byte> lFromHostQ, lFromKeybdQ;
	
	public TelnetClient(BlockingQueue<Byte> fromHostQ, BlockingQueue<Byte> fromKeybdQ) {
		lFromHostQ = fromHostQ;
		lFromKeybdQ = fromKeybdQ;
	}

	public boolean open( String host, Integer port ) {
		
		try {
			sock = new Socket( host, port );
			connected = true;
		} catch (UnknownHostException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		
		// start host listener thread
		(telnetListenerThread = new Thread( new TelnetListener(sock, lFromHostQ))).start();
		telnetListenerThread.setName( "TelnetListenerThread" );
		// start host writer thread
		(telnetWriterThread = new Thread( new TelnetWriter( sock, lFromKeybdQ ) ) ).start();
		telnetWriterThread.setName( "TelnetWriterThread" );
		
		return true;
		
	}
	
	public void close() {
		try {
			telnetListenerThread.interrupt();
			telnetWriterThread.interrupt();
			sock.close();
			connected = false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
