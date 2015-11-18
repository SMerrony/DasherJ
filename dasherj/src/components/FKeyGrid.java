package components;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * FKeyGrid - this class represents the function keys and the grid of associated customisable labels
 * 
 * @author steve
 * 
 * v. 0.9  - Move to JavaFX from Swing
 * v. 0.7  - Initial implementation based on v. 0.9 of DasherQ
 *
 */
public class FKeyGrid extends GridPane  {
	
	GridPane grid;
	String fKeyStrings[][];
	Label fKeyLabels[][], templateLabel1, templateLabel2;
	String templateTitle;
	FKeyHandler handler;
	
	Status status;
	Stage mStage;
	Scene scene;
	
	private final double MIN_BTN_WIDTH = 40.0;
	private final double MIN_BTN_HEIGHT = 20.0;
	private final double MIN_LABEL_HEIGHT = 30.0;
	
	public FKeyGrid( Status pStatus, FKeyHandler pHandler, final Stage mainStage, final Scene pScene ) {
		
		status = pStatus;
		mStage = mainStage; 
		scene = pScene;
		handler = pHandler;
		Button btn;
		
		grid = new GridPane();
		grid.setPadding( new Insets( 2,2,2,2 ) ); // Improve the look with a small pad inside the edge
		grid.setHgap( 1.0 ); // gap between labels/buttons
		grid.setVgap( 1.0 );
		grid.setMaxWidth( Double.MAX_VALUE );
		GridPane.setHgrow( grid, Priority.ALWAYS );
		
		templateLabel1 = new Label("");
		templateLabel1.setFont( Font.font( "Arial", FontWeight.BOLD, 9 ) );
		templateLabel1.setStyle( "-fx-background-color: lightgray;" );
		templateLabel1.setAlignment( Pos.CENTER );
		templateLabel1.setTextAlignment( TextAlignment.CENTER );
		//templateLabel1.setOpaque( true );
		templateLabel2 = new Label("");
		templateLabel2.setFont( Font.font( "Arial", FontWeight.BOLD, 9 ) );
		templateLabel2.setStyle( "-fx-background-color: lightgray;" );
		//templateLabel2.setOpaque( true );	
		
		grid.add( makeFKeyButton( "Loc Pr", "Local Print" ), 0, 0 );
		
		grid.add( makeFKeyButton( "Brk", "Command-Break" ), 0,4 );
		
		grid.add( btn = makeFKeyButton( "F1" ), 1,4 );
		//scene.addMnemonic( new Mnemonic( btn, new KeyCodeCombination( KeyCode.F1 )) );
		grid.add( makeFKeyButton( "F2" ), 2,4 );
		grid.add( makeFKeyButton( "F3" ), 3,4 );
		grid.add( makeFKeyButton( "F4" ), 4,4 );
		grid.add( makeFKeyButton( "F5" ), 5,4 );
		
		grid.add( makeFKeyLabel( "Ctrl-Shift" ), 6,0 );
		grid.add( makeFKeyLabel( "Ctrl" ), 6,1 );
		grid.add( makeFKeyLabel( "Shift" ), 6,2 );
		
		grid.add( templateLabel1, 6,4 );
		
		grid.add( makeFKeyButton( "F6" ), 7,4 );
		grid.add( makeFKeyButton( "F7" ), 8,4 );
		grid.add( makeFKeyButton( "F8" ), 9,4 );
		grid.add( makeFKeyButton( "F9" ), 10,4 );
		grid.add( makeFKeyButton( "F10" ), 11,4 );
		
		grid.add( makeFKeyLabel( "Ctrl-Shift" ), 12,0 );
		grid.add( makeFKeyLabel( "Ctrl" ), 12,1 );
		grid.add( makeFKeyLabel( "Shift" ), 12,2 );

		grid.add( templateLabel2, 12,4 );
		
		grid.add( makeFKeyButton( "F11" ), 13,4 );
		grid.add( makeFKeyButton( "F12" ), 14,4 );
		grid.add( makeFKeyButton( "F13" ), 15,4 );
		grid.add( makeFKeyButton( "F14" ), 16,4 );
		grid.add( makeFKeyButton( "F15" ), 17,4 );
		
		grid.add( makeFKeyButton( "Hold" ), 18,0 );
		grid.add( makeFKeyButton( "Er Pg", "Erase Page" ), 18,1 );
		grid.add( makeFKeyButton( "CR" ), 18,2 );
		grid.add( makeFKeyButton( "ErEOL", "Erase to EOL"), 18,3 );
		
		// now the blank labels ready for use later
		fKeyStrings = new String[4][15];
		fKeyLabels = new Label[4][15];

		for (int k = 0; k < 5; k++) {
			for (int r = 0; r < 4; r++ ) {
				fKeyLabels[r][k] = makeFKeyLabel( "" );
				grid.add( fKeyLabels[r][k], k+1, r );
			}
		}	
		for (int k = 5; k < 10; k++) {
			for (int r = 0; r < 4; r++ ) {
				fKeyLabels[r][k] = makeFKeyLabel("");
				grid.add( fKeyLabels[r][k], k+2, r );
			}
		}
		for (int k = 10; k < 15; k++) {
			for (int r = 0; r < 4; r++ ) {
				fKeyLabels[r][k] = makeFKeyLabel("");
				grid.add( fKeyLabels[r][k], k+3, r );
			}
		}
	}
	
	protected Button makeFKeyButton( String label, String tooltip ) {
		Button button = makeFKeyButton( label );
		button.setTooltip( new Tooltip( tooltip ) );
		return button;
	}

	protected Button makeFKeyButton( String label ) {
		Button button = new Button( label );
		button.setFont( Font.font( "Arial", FontWeight.BOLD, 9 ) );
		button.setMinWidth( MIN_BTN_WIDTH );
		button.setMinHeight( MIN_BTN_HEIGHT );
		button.setMaxWidth( Double.MAX_VALUE );
		button.addEventHandler( ActionEvent.ANY, handler );
		// We don't want focus on these keys or they will fire with <space> press
		button.setFocusTraversable( false );

		return button;
	}
	
	protected Label makeFKeyLabel( String text ) {
		Label label = new Label( text ); 
		label.setAlignment( Pos.CENTER );
		label.setTextAlignment( TextAlignment.CENTER );
		label.setFont( Font.font( "Arial", FontWeight.NORMAL, 8 ) );
		label.setMinWidth( MIN_BTN_WIDTH );
		label.setMaxWidth( Double.MAX_VALUE );
		label.setMinHeight( MIN_LABEL_HEIGHT);
		label.setStyle( "-fx-border-width: 1; -fx-border-color: DARKGRAY;" );
		return label;
	}

	public void loadTemplate() {
		
		FileChooser templateFileChooser = new FileChooser();
		templateFileChooser.setTitle( "Load Function Key Template" );
		templateFileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter( "DasherQ/J Template", "*.txt" ) );
		File templateFile = templateFileChooser.showOpenDialog( mStage );
		if (templateFile != null) {
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
			        		fKeyLabels[r][k].setText( template_line.replace( "\\", "\n" ) );
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