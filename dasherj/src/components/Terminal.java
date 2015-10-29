package components;

import java.awt.Toolkit;
import java.util.concurrent.BlockingQueue;

/***
 * Abstraction of what is currently displayed on the terminal
 * Display behaviour (not keyboard) emulation happens here
 * 
 * @author steve
 * 
 * v.0.5 -  Add status.dirty = true to events where cursor is moved to improve responsiveness
 * 			Renamed from Screen to Terminal to more accurately reflect purpose
 * 			Replace * at startup with "OK" message
 * 		    Add self-test method
 * 		    Merge in v.0.3/4 changes
 *			Merge in improvements from C++ version 0.4
 */
public class Terminal implements Runnable {

	public static final int VISIBLE_ROWS = 24;
	public static final int VISIBLE_COLS = 80;


	public static final byte NULL			= (byte) 0;
	public static final byte PRINT_FORM		= (byte) 1;

	public static final byte BLINK_ENABLE 	= (byte) 3; // for the whole screen
	public static final byte BLINK_DISABLE 	= (byte) 4; // for the whole screen
	public static final byte READ_WINDOW_ADDR = (byte) 5; // REQUIRES RESPONSE

	public static final byte BELL 			= (byte) 7;
	public static final byte HOME			= (byte) 8; // window home
	public static final byte TAB 			= (byte) 9;
	public static final byte NL 			= (byte) 10;
	public static final byte ERASE_EOL 		= (byte) 11;
	public static final byte ERASE_WINDOW 	= (byte) 12; // implies window home too
	public static final byte CR 			= (byte) 13;
	public static final byte BLINK_ON 		= (byte) 14;
	public static final byte BLINK_OFF 		= (byte) 15;
	public static final byte WRITE_WINDOW_ADDR = (byte) 16;
	public static final byte PRINT_WINDOW	= (byte) 17;
	public static final byte ROLL_ENABLE  	= (byte) 18;
	public static final byte ROLL_DISABLE 	= (byte) 19;
	public static final byte UNDERSCORE_ON 	= (byte) 20;
	public static final byte UNDERSCORE_OFF = (byte) 21;

	public static final byte CURSOR_UP 		= (byte) 23;
	public static final byte CURSOR_RIGHT 	= (byte) 24;
	public static final byte CURSOR_LEFT 	= (byte) 25;
	public static final byte CURSOR_DOWN 	= (byte) 26;

	public static final byte DIM_ON 		= (byte) 28;
	public static final byte DIM_OFF 		= (byte) 29;

	public static final byte CMD			= (byte) 30;

	public static final byte SPACE 			= (byte) 32;

	public static final byte SELF_TEST	    = (byte) -2;  // Not standard, used to initiate self-test function, only in off-line mode

	public int cursorX, cursorY;
	public boolean roll_enabled, blinking_enabled, blinkState, protection_enabled;

	private Status status;

	Cell[][] display;

	private BlockingQueue<Byte> fromHostQ, fromKbdQ, logQ;

	private boolean inCommand, inExtendedCommand, readingWindowAddressX, readingWindowAddressY, blinking, dimmed, reversedVideo, underscored, protectd,
	inTelnetCommand, gotTelnetDo, gotTelnetWill;
	private int newXaddress, newYaddress;


	public Terminal (Status pStatus, BlockingQueue<Byte> pFromHostQ, BlockingQueue<Byte> pFromKbdQ, BlockingQueue<Byte> pLogQ) {

		status    = pStatus;
		fromHostQ = pFromHostQ;
		fromKbdQ  = pFromKbdQ;
		logQ      = pLogQ;

		cursorX = 0;
		cursorY = 0;
		roll_enabled = true;
		blinking_enabled = true;
		protection_enabled = false;
		inCommand = false; inExtendedCommand = false;
		inTelnetCommand = false; gotTelnetDo = false; gotTelnetWill = false;
		readingWindowAddressX = false;
		readingWindowAddressY = false;
		blinking = false;
		dimmed = false;
		reversedVideo = false;
		underscored = false;
		display = new Cell[VISIBLE_ROWS][VISIBLE_COLS];
		for (int y = 0; y < VISIBLE_ROWS; y++) {
			for (int x = 0; x < VISIBLE_COLS; x++) {
				display[y][x] = new Cell();
			}
		}

		display[12][39].charValue = 'O';
		display[12][40].charValue = 'K';
	}

	void clearLine( int line ) {
		for (int cc = 0; cc < VISIBLE_COLS; cc++) {
			display[line][cc].clearToSpace();
		}
		inCommand = false;
		readingWindowAddressX = false;
		readingWindowAddressY = false;
		blinking = false;
		dimmed = false;
		reversedVideo = false;
		underscored = false;
	}

	void clearScreen() {
		for (int row = 0; row < VISIBLE_ROWS; row++){
			clearLine( row );
		}
	}

	void eraseUnprotectedToEndOfScreen() {
		// clear remainder of line
		for (int x = cursorX; x < VISIBLE_COLS; x++) {
			display[cursorY][x].clearToSpaceIfUnprotected();
		}
		// clear all lines below
		for (int y = cursorY + 1; y < VISIBLE_ROWS; y++) {
			for (int x = 0; x < VISIBLE_COLS; x++) {
				display[y][x].clearToSpaceIfUnprotected();
			}
		}
	}


	void scrollUp( int rows ) {
		for (int times = 0; times < rows; times++) {
			// move each char up a row
			for (int r = 1; r < VISIBLE_ROWS; r++) { 
				for (int c = 0; c < VISIBLE_COLS; c++) {
					display[r-1][c].copy( display[r][c] );
				}
			}
			// clear the bottom row
			clearLine( VISIBLE_ROWS - 1 );
		}
	}

	void selfTest( BlockingQueue<Byte> fromKbdQ ) {

		byte[] testlineHRule1 = "12345678901234567890123456789012345678901234567890123456789012345678901234567890".getBytes();
		byte[] testlineHRule2 = "         1         2         3         4         5         6         7         8".getBytes();
		byte[] testline1 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567489!\"$%^.".getBytes();
		byte[] testlineN = "Normal  : ".getBytes();
		byte[] testlineD = "Dim     : ".getBytes();
		byte[] testlineB = "Blink   : ".getBytes();
		byte[] testlineU = "Under   : ".getBytes();
		byte[] testlineR = "Reverse : ".getBytes();
		int c;

		fromKbdQ.offer( ERASE_WINDOW );

		for (c = 0; c < testlineHRule1.length; c++) {
			fromKbdQ.offer( testlineHRule1[c] );
		}
		fromKbdQ.offer( NL );
		for (c = 0; c < testlineHRule2.length; c++) {
			fromKbdQ.offer( testlineHRule2[c] );
		}
		fromKbdQ.offer( NL );

		for (c = 0; c < testlineN.length; c++) {
			fromKbdQ.offer( testlineN[c] );
		}
		for (c = 0; c < testline1.length; c++) {
			fromKbdQ.offer( testline1[c] );
		}
		fromKbdQ.offer( NL );

		for (c = 0; c < testlineD.length; c++) {
			fromKbdQ.offer( testlineD[c] );
		}
		fromKbdQ.offer( DIM_ON );
		for (c = 0; c < testline1.length; c++) {
			fromKbdQ.offer( testline1[c] );
		}
		fromKbdQ.offer( DIM_OFF );
		fromKbdQ.offer( NL );

		for (c = 0; c < testlineU.length; c++) {
			fromKbdQ.offer( testlineU[c] );
		}
		fromKbdQ.offer( UNDERSCORE_ON );
		for (c = 0; c < testline1.length; c++) {
			fromKbdQ.offer( testline1[c] );
		}
		fromKbdQ.offer( UNDERSCORE_OFF );
		fromKbdQ.offer( NL );

		for (c = 0; c < testlineB.length; c++) {
			fromKbdQ.offer( testlineB[c] );
		}
		fromKbdQ.offer( BLINK_ON );
		for (c = 0; c < testline1.length; c++) {
			fromKbdQ.offer( testline1[c] );
		}
		fromKbdQ.offer( BLINK_OFF );
		fromKbdQ.offer( NL );

		for (c = 0; c < testlineR.length; c++) {
			fromKbdQ.offer( testlineR[c] );
		}
		fromKbdQ.offer( CMD );
		fromKbdQ.offer( (byte) 'D' );
		for (c = 0; c < testline1.length; c++) {
			fromKbdQ.offer( testline1[c] );
		}
		fromKbdQ.offer( CMD );
		fromKbdQ.offer( (byte) 'E' );
		fromKbdQ.offer( NL );
	}

	@Override
	public void run() {

		boolean skipChar;
		byte ch, willAction, doAction;

		try {

			while ( true ) {

				ch = fromHostQ.take();

				while (status.holding) { Thread.sleep( 100 ); }

				skipChar = false;

				// check for Telnet command
				if (status.connection == Status.ConnectionType.TELNET_CONNECTED && ch == TelnetClient.CMD_IAC) {
					if (inTelnetCommand) {
						// special case - host really wants to send 255 - let it through
						inTelnetCommand = false;
					} else {
						inTelnetCommand = true;
						skipChar = true;
						continue;
					}
				}

				// process Telnet command
				if (status.connection == Status.ConnectionType.TELNET_CONNECTED && inTelnetCommand) {
					// get command byte
					Byte telnetCmd = ch;
					switch (telnetCmd) {
					case TelnetClient.CMD_DO:
						gotTelnetDo = true;
						skipChar = true;
						break;
					case TelnetClient.CMD_WILL:
						gotTelnetWill = true;
						skipChar = true;
						break;
					case TelnetClient.CMD_AO:
					case TelnetClient.CMD_AYT:
					case TelnetClient.CMD_BRK:
					case TelnetClient.CMD_DM:
					case TelnetClient.CMD_DONT:
					case TelnetClient.CMD_EC: // ??? Erase Char - should we map this to BS/CurLeft?
					case TelnetClient.CMD_EL:
					case TelnetClient.CMD_GA:
					case TelnetClient.CMD_IP:
					case TelnetClient.CMD_NOP:
					case TelnetClient.CMD_SB: // should probably skip to SE...
					case TelnetClient.CMD_SE:
						skipChar = true;
						break;
					default:
						break;
					}
				}

				if (skipChar) continue;

				if (status.connection == Status.ConnectionType.TELNET_CONNECTED && gotTelnetDo) {				
					// whatever the host asks us to do we will refuse
					doAction = ch;
					fromKbdQ.offer( TelnetClient.CMD_IAC );
					fromKbdQ.offer( TelnetClient.CMD_WONT );
					fromKbdQ.offer( doAction );
					gotTelnetDo = false;
					inTelnetCommand = false;
					skipChar = true;
				}

				if (status.connection == Status.ConnectionType.TELNET_CONNECTED && gotTelnetWill) {
					// whatever the host offers to do we will decline
					willAction = ch;
					fromKbdQ.offer( TelnetClient.CMD_IAC );
					fromKbdQ.offer( TelnetClient.CMD_DONT );
					fromKbdQ.offer( willAction );
					gotTelnetWill = false;
					inTelnetCommand = false;
					skipChar = true;
				}

				if (skipChar) continue;

				// check for Self-Test command
				if (status.connection == Status.ConnectionType.DISCONNECTED && ch == SELF_TEST ) {
					selfTest( fromKbdQ );
					skipChar = true;
				}

				if (skipChar) { continue; }

				if (readingWindowAddressX) { 
					newXaddress = (int) ch & 0x7f;
					if (newXaddress >= VISIBLE_COLS) {
						System.out.printf( "Warning: host attempt to set cursor off screen at column %d\n", newXaddress );
						newXaddress = newXaddress - VISIBLE_COLS;
					}
					if (newXaddress == 127) {
						// special case - x stays the same - see D410 User Manual p.3-25
						newXaddress = cursorX;
					}
					readingWindowAddressX = false;
					readingWindowAddressY = true;
					skipChar = true;
					continue;
				} 			

				if (readingWindowAddressY) {
					newYaddress = (int) ch & 0x7f;
					cursorX = newXaddress;
					cursorY = newYaddress;
					if (newYaddress == 127) {
						// special case - y stays the same - see D410 User Manual p.3-25
						newYaddress = cursorY;
					}
					if (cursorY >= VISIBLE_ROWS) { 
						System.out.printf( "Warning: host attempt to set cursor off screen to row %d\n", cursorY );
						// see end of p.3-24 in D410 User Manual
						if (roll_enabled) {
							scrollUp( cursorY - (VISIBLE_ROWS - 1));
						}
						cursorY = cursorY - VISIBLE_ROWS; 
					}
					// System.out.printf("Terminal - moving cursor to Row %d,  Column %d\n", cursorY, cursorX);
					readingWindowAddressY = false;
					skipChar = true;
					continue;
				}

				// logging output chars
				if (status.logging) { logQ.offer( ch ); }

				// D200 commands
				if (inCommand) {
					switch (ch) {
					case 'C':	 	// REQUIRES RESPONSE
						// TODO: read model ID
						fromKbdQ.offer( (byte) 036 );
						fromKbdQ.offer( (byte) 0157 );
						fromKbdQ.offer( (byte) 043 );  // model report
						fromKbdQ.offer( (byte) 041 );  // D100/D200
						fromKbdQ.offer( (byte) 0b01001010 ); // see p.2-7 of D100/D200 User Manual
						fromKbdQ.offer( (byte) 003 );  // firmware code
						skipChar = true;
						break;
					case 'D':
						reversedVideo = true;
						skipChar = true;
						break;
					case 'E':
						reversedVideo = false;
						skipChar = true;
						break;
					default:
						// System.out.printf( "Screen: Warning - Unrecognised Break-CMD code '%s'\n", ch );
						break;
					}

					// D210 commands
					if (status.emulation.getLevel() >= 210 && ch == 'F') {
						inExtendedCommand = true;
						skipChar = true;
					}

					if (status.emulation.getLevel() >= 210 && inExtendedCommand) {
						switch (ch) {
						case 'F':
							eraseUnprotectedToEndOfScreen();
							skipChar = true;
							inExtendedCommand = false;
							break;
						}
					}

					//	                // D211 commands
					//					if (status.emulation.getLevel() >= 211) {
					//						switch (ch) {
					//						case 'F': // extended commands...
					//							Byte extCmd = fromHostQ.take();
					//							switch (extCmd) {
					//							case 'S': // Select Char set
					//								break;	
					//							}
					//							break;
					//						case 'N': // shift in
					//							break;
					//						case 'O': // shift out
					//							break;
					//						}
					//					}
					//
					//					// D400 commands
					//					if (status.emulation.getLevel() >= 400) {
					//						switch (ch) {
					//						case 'F': // extended commands...
					//							// get extended command byte
					//							Byte extCmd = fromHostQ.take();
					//							switch (extCmd) {
					//							case 'I': // delete line
					//								break;
					//							case '\\': // <134> delete line between margins
					//								break;
					//							case 'E': // Erase screen
					//								break;
					//							case ']': // Horizontal scroll disable
					//								break;
					//							case '^': // Horizontal scroll enable
					//								break;
					//							case 'H': // Insert line
					//								break;
					//							case '[': // Insert line between margins
					//								break;
					//							case 'a': // Print pass thru off
					//								break;
					//							case '\'': // Print pass thru on
					//								break;
					//							case 'W': // Protect disable
					//								break;
					//							case 'V': // Protect enable
					//								break;
					//							case 'O': // Read horizontal scroll offset
					//								break;
					//							case 'b': // Read screen address
					//								break;
					//							case 'A': // RESET
					//								break;
					//							case 'Z': // Restore normal margins
					//								break;
					//							case 'G': // Screen home
					//								break;
					//							case 'C': // Scroll left n
					//								break;
					//							case 'D': // Scroll right n
					//								break;
					//							case 'K': // Select compressed spacing
					//								break;
					//							case 'J': // Select normal spacing
					//								break;
					//							case 'Y': // Set alternate margins i,j,k
					//								break;
					//							case 'Q': // Set cursor type n
					//								break;
					//							case 'X': // Set margins i,j,k
					//								break;
					//							case 'T': // Set scroll rate n
					//								break;
					//							case 'B': // Set windows ....
					//								break;
					//							case '_': // Show columns i,j
					//								break;
					//							case '?': // Window bit dump ('5')
					//								break;
					//							case 'P': // Write screen address
					//								break;
					//							}
					//							break;
					//						case 'H': // scroll up
					//							break;
					//						case 'I': // scroll down
					//							break;
					//						case 'J': // insert char
					//							break;
					//						case 'K': // delete char
					//							break;
					//
					//
					//						}
					//					}

					inCommand = false;
					continue;
				}

				if (skipChar) { continue; }


				switch (ch) {
				case NULL:
					skipChar = true;
					break;
				case BELL:
					//System.out.print( '\07' );
					//System.out.flush();
					Toolkit.getDefaultToolkit().beep();
					skipChar = true;
					break;
				case BLINK_DISABLE:
					blinking_enabled = false;
					skipChar = true;
					break;
				case BLINK_ENABLE:
					blinking_enabled = true;
					skipChar = true;
					break;
				case BLINK_OFF:
					blinking = false;
					skipChar = true;
					break;
				case BLINK_ON:
					blinking = true;
					skipChar = true;
					break;
				case CURSOR_UP:
					if (cursorY > 0) { cursorY--; } else { cursorY = VISIBLE_ROWS - 1; }
					skipChar = true;
					status.dirty = true;
					break;
				case CURSOR_DOWN:
					if (cursorY < VISIBLE_ROWS - 2 ) { cursorY++; } else { cursorY = 0; }
					status.dirty = true;
					skipChar = true;
					break;	
				case CURSOR_RIGHT:
					if (cursorX < VISIBLE_COLS - 2) { cursorX++; } else { 
						cursorX = 0; 
						if (cursorY < VISIBLE_ROWS - 2 ) { cursorY++; } else { cursorY = 0; }
					}
					status.dirty = true;
					skipChar = true;
					break;
				case CURSOR_LEFT:
					if (cursorX > 0) { 
						cursorX--; 
					} else { 
						cursorX = VISIBLE_COLS - 1; 
						if (cursorY > 0) { cursorY--; } else { cursorY = VISIBLE_ROWS - 1; }
					}
					status.dirty = true;
					skipChar = true;
					break;
				case DIM_ON:
					dimmed = true;
					skipChar = true;
					break;
				case DIM_OFF:
					dimmed = false;
					skipChar = true;
					break;
				case HOME:
					cursorX = 0; cursorY = 0;
					status.dirty = true;
					skipChar = true;
					break;
				case ERASE_EOL:
					for (int col = cursorX; col < VISIBLE_COLS; col++) {
						display[cursorY][col].clearToSpace();
					}
					status.dirty = true;
					skipChar = true;
					break;
				case ERASE_WINDOW:
					clearScreen();
					cursorX = 0; cursorY = 0;
					status.dirty = true;
					skipChar = true;
					break;
				case ROLL_DISABLE:
					roll_enabled = false;
					skipChar = true;
					break;
				case READ_WINDOW_ADDR: 	 // REQUIRES RESPONSE - see D410 User Manual p.3-18
					fromKbdQ.offer( (byte) 31 );
					fromKbdQ.offer( (byte) cursorX );
					fromKbdQ.offer( (byte) cursorY );
					skipChar = true;
					break;
				case ROLL_ENABLE:
					roll_enabled = true;
					skipChar = true;
					break;
				case UNDERSCORE_ON:
					underscored = true;
					skipChar = true;
					break;
				case UNDERSCORE_OFF:
					underscored = false;
					skipChar = true;
					break;
				case WRITE_WINDOW_ADDR:
					readingWindowAddressX = true;
					skipChar = true;
					break;
				case CMD:
					inCommand = true;
					skipChar = true;
					break;				
				}

				if (skipChar) {
					continue;
				}

				// wrap due to hitting margin or new line?
				if (cursorX == VISIBLE_COLS || ch == NL) {
					if (cursorY == VISIBLE_ROWS - 1) { // hit bottom of screen
						if (roll_enabled) {
							this.scrollUp( 1 );
						} else {
							cursorY = 0;
							clearLine( cursorY );
						} 
					} else {
						cursorY++;
						if (!roll_enabled) {
							clearLine( cursorY );
						}
					}
					cursorX = 0;
				}

				// CR?
				if (ch == CR || ch == NL ) {
					cursorX = 0;
					continue;
				}

				// finally, put the character in the displayable character matrix
				// it will get picked up on next refresh by Crt
				if (ch > 0) {
					display[cursorY][cursorX].set(  ch, blinking, dimmed, reversedVideo, underscored, protectd );
				} else {
					System.out.printf( "Terminal: Warning - Ignoring character with code %d\n",  ch );
					continue;
				}

				cursorX++;

				status.dirty = true;


			}

		} 
		catch (InterruptedException ie) {

		}

	}
}
