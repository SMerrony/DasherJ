package components;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class TelnetWriter implements Runnable {
	
	Socket sock;
	OutputStream w;
	BlockingQueue<Byte> fromKbdQ;
	
	public TelnetWriter( Socket pSock, BlockingQueue<Byte> pFromKbdQ ) {
		sock = pSock;
		fromKbdQ = pFromKbdQ;
		
		try {
			w = sock.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		
		Byte b = 0;
		boolean cont = true;
		
		while (cont) {
			try {
				b = fromKbdQ.take();
			} catch (InterruptedException e) {
				// e.printStackTrace();
				System.out.printf( "TelnetWriter: Interrupted - stopping\n" );
				return;
			}
			
			try {
				w.write(b);
				// System.out.printf( "Wrote to host socket: <%s> Hex: %x\n", b, b );
			} catch (IOException e) {
				//				e.printStackTrace();
			}
		}
	
	}
}
