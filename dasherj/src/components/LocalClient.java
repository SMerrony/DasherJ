package components;

import java.util.concurrent.BlockingQueue;

public class LocalClient implements Runnable {
	
	public static final byte GO_ONLINE = (byte) -1;
	
	// the shared queues
	private BlockingQueue<Byte> fromHostQ, fromKeybdQ;
	
	public LocalClient( BlockingQueue<Byte> fromHostQ, BlockingQueue<Byte> fromKeybdQ ) {
		this.fromHostQ = fromHostQ;
		this.fromKeybdQ = fromKeybdQ;
	}

	
	@Override
	public void run() {
		
		Byte b;
		
		while (true) {
			try {
				b = fromKeybdQ.take();
				//fromHostQ.offer( fromKeybdQ.take() );
				if (b == GO_ONLINE) {
					System.out.printf( "LocalClient (Off-line) stopping at request\n");
					return;
				} else {
					fromHostQ.offer( b );
				}
			} catch (InterruptedException e) {
				System.out.printf( "LocalClient (Off-line) thread closing,\n" );
				return;
			}
		}
		
	}

}
