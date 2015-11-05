package components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * FKeyGrid - this class represents the function keys and the grid of associated customisable labels
 * 
 * @author steve
 * 
 * v. 0.7  - Initial implementation based on v. 0.9 of DasherQ
 *
 */
public class FKeyGrid extends JToolBar implements ActionListener {
	
	GridBagLayout grid;
	GridBagConstraints cons;
	String fKeyStrings[][];
	JLabel fKeyLabels[][], templateLabel1, templateLabel2;
	String templateTitle;
	
	Status status;
	BlockingQueue<Byte> fromKbdQ;
	
	public FKeyGrid( Status pStatus, BlockingQueue<Byte> pFromKbdQ ) {
		
		status = pStatus;
		fromKbdQ = pFromKbdQ;
		
		grid = new GridBagLayout();
		cons = new GridBagConstraints();
		setLayout( grid );
		
		cons.weightx = 1;
		cons.fill = GridBagConstraints.BOTH;
		cons.insets = new Insets( 1,1,1,1);
		
		templateLabel1 = new JLabel("", SwingConstants.CENTER );
		templateLabel1.setFont( new Font( "Arial", Font.PLAIN, 9 ) );
		templateLabel1.setBackground( Color.LIGHT_GRAY );
		templateLabel1.setOpaque( true );
		templateLabel2 = new JLabel("", SwingConstants.CENTER );
		templateLabel2.setFont( new Font( "Arial", Font.PLAIN, 9 ) );
		templateLabel2.setBackground( Color.LIGHT_GRAY );
		templateLabel2.setOpaque( true );	
		
		cons.gridx = 0; cons.gridy = 0;
		add( makeFKeyButton( "Loc Pr", "Local Print" ), cons );
		
		cons.gridx = 0; cons.gridy = 4;
		add( makeFKeyButton( "Brk", "Command-Break" ), cons );
		
		cons.gridx = 1; cons.gridy = 4;
		add( makeFKeyButton( "F1" ), cons );
		cons.gridx = 2; cons.gridy = 4;
		add( makeFKeyButton( "F2" ), cons );
		cons.gridx = 3; cons.gridy = 4;
		add( makeFKeyButton( "F3" ), cons );
		cons.gridx = 4; cons.gridy = 4;
		add( makeFKeyButton( "F4" ), cons );
		cons.gridx = 5; cons.gridy = 4;
		add( makeFKeyButton( "F5" ), cons );
		
		cons.gridx = 6; cons.gridy = 0;
		add( makeFKeyLabel( "<html>Ctrl-<br>Shift</html>" ), cons );
		cons.gridx = 6; cons.gridy = 1;
		add( makeFKeyLabel( "Ctrl" ), cons );
		cons.gridx = 6; cons.gridy = 2;
		add( makeFKeyLabel( "Shift" ), cons );
		
		cons.gridx = 6; cons.gridy = 4;
		add( templateLabel1, cons );
		
		cons.gridx = 7; cons.gridy = 4;
		add( makeFKeyButton( "F6" ), cons );
		cons.gridx = 8; cons.gridy = 4;
		add( makeFKeyButton( "F7" ), cons );
		cons.gridx = 9; cons.gridy = 4;
		add( makeFKeyButton( "F8" ), cons );
		cons.gridx = 10; cons.gridy = 4;
		add( makeFKeyButton( "F9" ), cons );
		cons.gridx = 11; cons.gridy = 4;
		add( makeFKeyButton( "F10" ), cons );
		
		cons.gridx = 12; cons.gridy = 0;
		add( makeFKeyLabel( "<html>Ctrl-<br>Shift</html>" ), cons );
		cons.gridx = 12; cons.gridy = 1;
		add( makeFKeyLabel( "Ctrl" ), cons );
		cons.gridx = 12; cons.gridy = 2;
		add( makeFKeyLabel( "Shift" ), cons );

		cons.gridx = 12; cons.gridy = 4;
		add( templateLabel2, cons );
		
		cons.gridx = 13; cons.gridy = 4;
		add( makeFKeyButton( "F11" ), cons );
		cons.gridx = 14; cons.gridy = 4;
		add( makeFKeyButton( "F12" ), cons );
		cons.gridx = 15; cons.gridy = 4;
		add( makeFKeyButton( "F13" ), cons );
		cons.gridx = 16; cons.gridy = 4;
		add( makeFKeyButton( "F14" ), cons );
		cons.gridx = 17; cons.gridy = 4;
		add( makeFKeyButton( "F15" ), cons );
		
		cons.gridx = 18; cons.gridy = 0;
		add( makeFKeyButton( "Hold" ), cons );
		cons.gridx = 18; cons.gridy = 1;
		add( makeFKeyButton( "Er Pg", "Erase Page" ), cons );
		cons.gridx = 18; cons.gridy = 2;
		add( makeFKeyButton( "CR" ), cons );
		cons.gridx = 18; cons.gridy = 3;
		add( makeFKeyButton( "ErEOL", "Erase to EOL"), cons );
		
		// now the blank labels ready for use later
		fKeyStrings = new String[4][15];
		fKeyLabels = new JLabel[4][15];

		for (int k = 0; k < 5; k++) {
			for (int r = 0; r < 4; r++ ) {
				cons.gridx = k + 1; cons.gridy = r;
				fKeyLabels[r][k] = new JLabel("", SwingConstants.CENTER );
				fKeyLabels[r][k].setFont( new Font( "Arial", Font.PLAIN, 9 ) );
				fKeyLabels[r][k].setBackground( Color.LIGHT_GRAY );
				fKeyLabels[r][k].setOpaque( true );
				add( fKeyLabels[r][k], cons );
			}
		}	
		for (int k = 5; k < 10; k++) {
			for (int r = 0; r < 4; r++ ) {
				cons.gridx = k+2; cons.gridy = r;
				fKeyLabels[r][k] = new JLabel("", SwingConstants.CENTER );
				fKeyLabels[r][k].setFont( new Font( "Arial", Font.PLAIN, 9 ) );
				fKeyLabels[r][k].setBackground( Color.LIGHT_GRAY );
				fKeyLabels[r][k].setOpaque( true );
				add( fKeyLabels[r][k], cons );
			}
		}
		for (int k = 10; k < 15; k++) {
			for (int r = 0; r < 4; r++ ) {
				cons.gridx = k+3; cons.gridy = r;
				fKeyLabels[r][k] = new JLabel("", SwingConstants.CENTER );
				fKeyLabels[r][k].setFont( new Font( "Arial", Font.PLAIN, 9 ) );
				fKeyLabels[r][k].setBackground( Color.LIGHT_GRAY );
				fKeyLabels[r][k].setOpaque( true );
				add( fKeyLabels[r][k], cons );
			}
		}
	}
	
	protected JButton makeFKeyButton( String label, String tooltip ) {
		JButton button = makeFKeyButton( label );
		button.setToolTipText( tooltip );
		return button;
	}

	protected JButton makeFKeyButton( String label ) {
		JButton button = new JButton( label );
		button.setFont( new Font( "Arial", Font.BOLD, 9 ) );
		button.setActionCommand( label );
		button.addActionListener( this );	
		return button;
	}
	
	protected JLabel makeFKeyLabel( String text ) {
		JLabel label = new JLabel( text, SwingConstants.CENTER ); 
		label.setFont( new Font( "Arial", Font.PLAIN, 9 ) );
		return label;
	}

	@Override
	public void actionPerformed( ActionEvent ae ) {
	
		byte modifier = 0;
		
		if (status.control_pressed && status.shift_pressed) { modifier = -80; }  // Ctrl-Shift
		if (status.control_pressed && !status.shift_pressed) { modifier = -64; } // Ctrl
		if (!status.control_pressed && status.shift_pressed) { modifier = -16; } // Shift
		
		// these are for the toolbar buttons - not the real function keys
		String cmd = ae.getActionCommand();
		switch (cmd) {
		case "Brk":
			fromKbdQ.offer( (byte) 2 );  // special CMD_BREAK indicator
			break;
		case "F1":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (113 + modifier) );
			break;
		case "F2":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (114 + modifier) );
			break;
		case "F3":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (115 + modifier) );
			break;
		case "F4":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (116 + modifier) );
			break;
		case "F5":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (117 + modifier) );
			break;
		case "F6":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (118 + modifier) );
			break;
		case "F7":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (119 + modifier) );
			break;
		case "F8":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (120 + modifier) );
			break;
		case "F9":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (121 + modifier) );
			break;
		case "F10":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (122 + modifier) );
			break;
		case "F11":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (123 + modifier) );
			break;
		case "F12":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (124 + modifier) );
			break;
		case "F13":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (125 + modifier) );
			break;
		case "F14":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (126 + modifier) );
			break;
		case "F15":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (112 + modifier) );
			break;
		case "Er Pg":
			fromKbdQ.offer( (byte) 12 );
			break;
		case "CR":
			fromKbdQ.offer( (byte) 13 );
			break;
		case "ErEOL":
			fromKbdQ.offer( (byte) 11 );
			break;
		case "Loc Pr":
			PrinterJob printJob = PrinterJob.getPrinterJob();
			//printJob.setPrintable( crt );  FIXME !!!
			boolean ok = printJob.printDialog();
			if (ok) {
				try {
					printJob.print();
				} catch (PrinterException pe) {
					
				}
			}
			break;	
		case "Hold":
			status.holding = !status.holding;
			break;
		default:
			System.out.printf( "DasherJ - Warning: Unknown ActionEvent (%s) received.\n", cmd );
			break;	
		}
	}

	public void loadTemplate() {
		
		final JFileChooser templateFileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter( "DasherQ/J Template", "txt" );
		templateFileChooser.setFileFilter( filter );
		int retVal = templateFileChooser.showOpenDialog( getTopLevelAncestor() );
		if (retVal == JFileChooser.APPROVE_OPTION) {
			File templateFile = templateFileChooser.getSelectedFile();
			try {
				InputStream templateStream  = new FileInputStream( templateFile.getAbsolutePath() );
				BufferedReader templateReader = new BufferedReader( new InputStreamReader( templateStream ) );

				templateTitle = templateReader.readLine();
				templateLabel1.setText( templateTitle );
				templateLabel2.setText( templateTitle );
				
				// clear the labels
			    for (int k = 0; k < 15; k++) {
			        for (int r = 3; r >= 0; r--) {
			            fKeyLabels[r][k].setText( "" );
			        }
			    }
			    
			    // read all labels in order from template file
			    String template_line;
			    for (int k = 0; k < 15; k++) {
			        for (int r = 3; r >= 0; r--) {
			        	if ((template_line = templateReader.readLine()) == null) {
			        		templateReader.close();
			        		return;
			        	}
			        	if (template_line.length() > 0) {
			        		fKeyLabels[r][k].setText( "<html>" +
			        								  template_line.replace( "\\", "<br>" ) +
			        								  "</html>" );
			        	}
			        }
			    }
			    templateReader.close();
			    
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block

			}
			
		}
	}
	
}