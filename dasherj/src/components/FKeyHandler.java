package components;

/**
 * Handle real and simulated events from the FKeyGrid
 * 
 * v0.9   Initial version (for JavaFX conversion)
 */
import java.util.concurrent.BlockingQueue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

public class FKeyHandler implements EventHandler<ActionEvent> {

	BlockingQueue<Byte> lFromKbdQ;
	Status status;
	
	public FKeyHandler(BlockingQueue<Byte> pFromKbdQ, Status pStatus) {
		lFromKbdQ = pFromKbdQ;
		status = pStatus;
	}
	
	@Override
	public void handle( ActionEvent ae ) {
		
		int modifier = 0;
		
		if (status.control_pressed && status.shift_pressed) { modifier = -80; }  // Ctrl-Shift
		if (status.control_pressed && !status.shift_pressed) { modifier = -64; } // Ctrl
		if (!status.control_pressed && status.shift_pressed) { modifier = -16; } // Shift

		String fk = ((Button) ae.getSource()).getText();
		switch(fk) {
		case "Brk":
			lFromKbdQ.offer( (byte) 2 ); // special CMD_BREAK indicator
			break;
		case "Er Pg":
			lFromKbdQ.offer( (byte) 12 );
			break;
		case "CR":
			lFromKbdQ.offer( (byte) 13 );
			break;	
		case "ErEOL":
			lFromKbdQ.offer( (byte) 11 );
			break;
		case "Loc Pr": // TODO Loc Pr
			break;
		case "Hold":
			status.holding = !status.holding;
			break;
		case "F1":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (113 + modifier) );
			break;
		case "F2":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (114 + modifier) );
			break;
		case "F3":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (115 + modifier) );
			break;
		case "F4":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (116 + modifier) );
			break;
		case "F5":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (117 + modifier) );
			break;
		case "F6":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (118 + modifier) );
			break;
		case "F7":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (119 + modifier) );
			break;
		case "F8":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (120 + modifier) );
			break;
		case "F9":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (121 + modifier) );
			break;
		case "F10":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (122 + modifier) );
			break;
		case "F11":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (123 + modifier) );
			break;
		case "F12":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (124 + modifier) );
			break;
		case "F13":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (125 + modifier) );
			break;
		case "F14":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (126 + modifier) );
			break;
		case "F15":
			lFromKbdQ.offer( (byte) 30 );
			lFromKbdQ.offer( (byte) (112 + modifier) );
			break;

		}
	}

}
