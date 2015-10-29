package components;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.util.concurrent.BlockingQueue;

public class KeyboardHandler implements KeyEventDispatcher {
	
	BlockingQueue<Byte> lFromKbdQ;
	Status status;

	public KeyboardHandler(BlockingQueue<Byte> fromKbdQ, Status pStatus) {
		lFromKbdQ = fromKbdQ;
		status = pStatus;
	}
	
	@Override
	public boolean dispatchKeyEvent( KeyEvent kev ) {
	
		switch (kev.getID() ) {
		case KeyEvent.KEY_PRESSED:
			keyPressed( kev );
			break;
		case KeyEvent.KEY_RELEASED:
			keyReleased( kev );
			break;
		case KeyEvent.KEY_TYPED:
			keyTyped( kev );
			break;
		default:
			System.out.printf( "KeyboardHandler: Warning - Unknown Key Event Type\n" );
			break;
		}
		
		
		return true;  // no further action required
	}
	
	private void keyPressed( KeyEvent kEv ) { 
		int kc = kEv.getKeyCode();
		switch( kc ) {
		// need modifiers for F-key buttons and cursor keys
		case KeyEvent.VK_CONTROL:
			status.control_pressed = true;
			break;
		case KeyEvent.VK_SHIFT:
			status.shift_pressed = true;
			break;
		}
	}

	private void keyReleased(KeyEvent arg0) {

		int kc = arg0.getKeyCode();
		
		switch( kc ) {
		/*case KeyEvent.VK_ESCAPE:
			lFromKbdQ.offer( (byte) 30 );
			break;*/
		// cursor keys
		case KeyEvent.VK_DOWN:
			if (status.shift_pressed) {
				lFromKbdQ.offer( (byte) 30 );
			}
			lFromKbdQ.offer( (byte) 26 );
			break;
		case KeyEvent.VK_END:
			lFromKbdQ.offer( (byte) 30 );	// Shift C3 on Dasher
			lFromKbdQ.offer( (byte) 90 );
			break;
		case KeyEvent.VK_HOME:
			lFromKbdQ.offer( (byte) 30 );	// Shift C1 on Dasher
			lFromKbdQ.offer( (byte) 88 );
			break;
		case KeyEvent.VK_LEFT:
			if (status.shift_pressed) {
				lFromKbdQ.offer( (byte) 30 );
			}
			lFromKbdQ.offer( (byte) 25 );
			break;
		case KeyEvent.VK_PAGE_DOWN:
			lFromKbdQ.offer( (byte) 30 );	// Shift C4 on Dasher
			lFromKbdQ.offer( (byte) 91 );
			break;
		case KeyEvent.VK_PAGE_UP:
			lFromKbdQ.offer( (byte) 30 );	// Shift C2 on Dasher
			lFromKbdQ.offer( (byte) 89 );
			break;
		case KeyEvent.VK_RIGHT:
			if (status.shift_pressed) {
				lFromKbdQ.offer( (byte) 30 );
			}
			lFromKbdQ.offer( (byte) 24 );
			break;
		case KeyEvent.VK_UP:
			if (status.shift_pressed) {
				lFromKbdQ.offer( (byte) 30 );
			}
			lFromKbdQ.offer( (byte) 23 );
			break;
			// modifiers
		case KeyEvent.VK_CONTROL:
			status.control_pressed = false;
			break;
		case KeyEvent.VK_SHIFT:
			status.shift_pressed = false;
			break;
		default:
			break;
		}
	}

	private void keyTyped(KeyEvent arg0) {
	
		char ch;
		
		ch = arg0.getKeyChar();
		
		// System.out.printf("Char captured: %s\n" , arg0.getKeyChar() );
		
/*		switch (ch) {
		case 10: // convert plain LF to CR/LF for NVT compliance
			lFromKbdQ.offer( (byte) 13 );
			if (status.connection == Status.ConnectionType.TELNET_CONNECTED) {
				lFromKbdQ.offer( (byte) 10 );
			}
			break;
		
		default:
			lFromKbdQ.offer( (byte) arg0.getKeyChar() );
		}
*/		
		lFromKbdQ.offer( (byte) arg0.getKeyChar() );
	}

}
