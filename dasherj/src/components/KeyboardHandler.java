package components;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.concurrent.BlockingQueue;

/***
 * This class takes keyboard input from the user, does any special handling required
 * for DASHER compatibility, then passes the data on to the fromKbdQ for transmission
 * to the host by either TelnetWriter or SerialWriter 
 * 
 * @author steve
 * 
 * v. 0.9 -  Map PC Alt-Gr to DASHER CMD
 * v. 0.7 -  Handle real function keys
 * v. 0.6 -  Fix sending of NewLines to be DASHER-compliant (and not doubled-up!)
 * 
 */

public class KeyboardHandler implements KeyEventDispatcher {
	
	BlockingQueue<Byte> lFromKbdQ;
	Status status;
	private int modifier;

	public KeyboardHandler(BlockingQueue<Byte> fromKbdQ, Status pStatus) {
		lFromKbdQ = fromKbdQ;
		status = pStatus;
		modifier = 0;
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
			// we are using KEY_RELEASED in all normal cases
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
		if (status.control_pressed && status.shift_pressed) { modifier = -80; }  // Ctrl-Shift
		if (status.control_pressed && !status.shift_pressed) { modifier = -64; } // Ctrl
		if (!status.control_pressed && status.shift_pressed) { modifier = -16; } // Shift
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
		case KeyEvent.VK_ENTER:
			lFromKbdQ.offer( (byte) 10 );
			break;
			
		// Function and emulated keys...	
		case KeyEvent.VK_CLEAR:
			lFromKbdQ.offer( (byte) 12 );
			break;
		case KeyEvent.VK_PAUSE:  // Dasher: HOLD
			status.holding = !status.holding;
			break;
		case KeyEvent.VK_PRINTSCREEN:
			PrinterJob printJob = PrinterJob.getPrinterJob();
			//printJob.setPrintable( SwingUtilities.getWindowAncestor( null ));  FIXME !!!
			boolean ok = printJob.printDialog();
			if (ok) {
				try {
					printJob.print();
				} catch (PrinterException pe) {
					pe.printStackTrace();
				}
			}
			break;	
		case KeyEvent.VK_ALT_GRAPH: // We are mapping Alt Gr to DASHER CMD
			lFromKbdQ.offer( (byte) 30 );
		case KeyEvent.VK_F16: // Dummy value for Break button
			lFromKbdQ.offer( (byte) 2 ); // special CMD_BREAK indicator
			break;			
		case KeyEvent.VK_F24: // Dummy value for CR button
			lFromKbdQ.offer( (byte) 13 );
			break;
		case KeyEvent.VK_F23: // Dummy value for Er EOL button
			lFromKbdQ.offer( (byte) 11 );
			break;
		case KeyEvent.VK_F1:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (113 + modifier) );
			break;
		case KeyEvent.VK_F2:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (114 + modifier) );
			break;
		case KeyEvent.VK_F3:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (115 + modifier) );
			break;
		case KeyEvent.VK_F4:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (116 + modifier) );
			break;
		case KeyEvent.VK_F5:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (117 + modifier) );
			break;
		case KeyEvent.VK_F6:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (118 + modifier) );
			break;
		case KeyEvent.VK_F7:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (119 + modifier) );
			break;
		case KeyEvent.VK_F8:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (120 + modifier) );
			break;
		case KeyEvent.VK_F9:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (121 + modifier) );
			break;
		case KeyEvent.VK_F10:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (122 + modifier) );
			break;
		case KeyEvent.VK_F11:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (123 + modifier) );
			break;
		case KeyEvent.VK_F12:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (124 + modifier) );
			break;
		case KeyEvent.VK_F13:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (125 + modifier) );
			break;
		case KeyEvent.VK_F14:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (126 + modifier) );
			break;
		case KeyEvent.VK_F15:
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (112 + modifier) );
			break;
			
		default:
			lFromKbdQ.offer( (byte) arg0.getKeyChar() );
			break;
		}
	}

}
