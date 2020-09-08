package il.ac.tau.cs.sw1.trivia;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class TriviaGUI {

	private static final int MAX_ERRORS = 3;
	private Shell shell;
	private Label scoreLabel;
	private Composite questionPanel;
	private Label startupMessageLabel;
	private Font boldFont;
	private String lastAnswer;
	
	// Currently visible UI elements.
	Label instructionLabel;
	Label questionLabel;
	private List<Button> answerButtons = new LinkedList<>();
	private Button passButton;
	private Button fiftyFiftyButton;
	
	//mine additions
	private List<Q> qList = new ArrayList<>();
	private boolean isFirstPass;
	private boolean isFirstFifty;
	private Q newQuestion;
	private int score;
	private int wrongGuessesNum;
	private int Qcnt;
	private Random random = new Random();
	private int randomInt;


	public void open() {
		createShell();
		runApplication();
	}

	/**
	 * Creates the widgets of the application main window
	 */
	private void createShell() {
		Display display = Display.getDefault();
		shell = new Shell(display);
		shell.setText("Trivia");

		// window style
		Rectangle monitor_bounds = shell.getMonitor().getBounds();
		shell.setSize(new Point(monitor_bounds.width / 3,
				monitor_bounds.height / 4));
		shell.setLayout(new GridLayout());

		FontData fontData = new FontData();
		fontData.setStyle(SWT.BOLD);
		boldFont = new Font(shell.getDisplay(), fontData);

		// create window panels
		createFileLoadingPanel();
		createScorePanel();
		createQuestionPanel();
	}

	/**
	 * Creates the widgets of the form for trivia file selection
	 */
	private void createFileLoadingPanel() {
		final Composite fileSelection = new Composite(shell, SWT.NULL);
		fileSelection.setLayoutData(GUIUtils.createFillGridData(1));
		fileSelection.setLayout(new GridLayout(4, false));

		final Label label = new Label(fileSelection, SWT.NONE);
		label.setText("Enter trivia file path: ");

		// text field to enter the file path
		final Text filePathField = new Text(fileSelection, SWT.SINGLE
				| SWT.BORDER);
		filePathField.setLayoutData(GUIUtils.createFillGridData(1));

		// "Browse" button
		final Button browseButton = new Button(fileSelection, SWT.PUSH);
		browseButton.setText("Browse");
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String filePath = GUIUtils.getFilePathFromFileDialog(shell);
				filePathField.setText(filePath);
			}
		});
		
		// "Play!" button
		final Button playButton = new Button(fileSelection, SWT.PUSH);
		playButton.setText("Play!");
		playButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					BufferedReader reader = new BufferedReader(new FileReader(filePathField.getText()));
					String line;
					while ((line = reader.readLine()) != null) {
						qList.add(new Q(line.split("\t")));
					}
						reader.close();
					} 
				catch (IOException e1) {}
				isFirstPass = true;
				isFirstFifty = true;
				wrongGuessesNum = 0;
				Qcnt = 0;
				score = 0;
				lastAnswer = "";
				scoreLabel.setText(String.valueOf(score));
				movingForward(true);
			}
		});
	}
	
	private void movingForward(Boolean isIncrement) {
		if (qList.size() > 0 && wrongGuessesNum < MAX_ERRORS) {
			randomInt = random.nextInt(qList.size());
			newQuestion = qList.remove(randomInt);
			if (isIncrement)
				Qcnt++;
			updateQuestionPanel(newQuestion.getQuestionText(), newQuestion.getPossibleAns());
		}
		else
			GUIUtils.showInfoDialog(shell, "GAME OVER", "Your final score is " + score + " after " + Qcnt + " questions.");
	}
	
	private class Q {
		private String question;
		private String correctAns;
		private List<String> possibleAns = new ArrayList<>();
		
		public Q(String[] line) {
			question = line[0];
			correctAns = line[1];
			for(int i=1; i<line.length; i++) {
				possibleAns.add(line[i]);
			}
		}
		
		public String getQuestionText() {
			return question;
		}
		
		public String getCorrectAns() {
			return correctAns;
		}
		
		public List<String> getPossibleAns(){
			Collections.shuffle(possibleAns);
			return possibleAns;
		}

		
	}
	
	/**
	 * Creates the panel that displays the current score
	 */
	private void createScorePanel() {
		Composite scorePanel = new Composite(shell, SWT.BORDER);
		scorePanel.setLayoutData(GUIUtils.createFillGridData(1));
		scorePanel.setLayout(new GridLayout(2, false));

		final Label label = new Label(scorePanel, SWT.NONE);
		label.setText("Total score: ");

		// The label which displays the score; initially empty
		scoreLabel = new Label(scorePanel, SWT.NONE);
		scoreLabel.setLayoutData(GUIUtils.createFillGridData(1));
	}

	/**
	 * Creates the panel that displays the questions, as soon as the game
	 * starts. See the updateQuestionPanel for creating the question and answer
	 * buttons
	 */
	private void createQuestionPanel() {
		questionPanel = new Composite(shell, SWT.BORDER);
		questionPanel.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
				true, true));
		questionPanel.setLayout(new GridLayout(2, true));

		// Initially, only displays a message
		startupMessageLabel = new Label(questionPanel, SWT.NONE);
		startupMessageLabel.setText("No question to display, yet.");
		startupMessageLabel.setLayoutData(GUIUtils.createFillGridData(2));
	}

	/**
	 * Serves to display the question and answer buttons
	 */
	private void updateQuestionPanel(String question, List<String> answers) {
		// Save current list of answers.
		List<String> currentAnswers = answers;
		Collections.shuffle(currentAnswers);
		answerButtons.clear(); //clearing buttons of previous question.
		
		// clear the question panel
		Control[] children = questionPanel.getChildren();
		for (Control control : children) {
			control.dispose();
		}

		// create the instruction label
		instructionLabel = new Label(questionPanel, SWT.CENTER | SWT.WRAP);
		instructionLabel.setText(lastAnswer + "Answer the following question:");
		instructionLabel.setLayoutData(GUIUtils.createFillGridData(2));

		// create the question label
		questionLabel = new Label(questionPanel, SWT.CENTER | SWT.WRAP);
		questionLabel.setText(question);
		questionLabel.setFont(boldFont);
		questionLabel.setLayoutData(GUIUtils.createFillGridData(2));

		// create the answer buttons
		for (int i = 0; i < 4; i++) {
			Button answerButton = new Button(questionPanel, SWT.PUSH | SWT.WRAP);
			answerButton.setText(currentAnswers.get(i));
			GridData answerLayoutData = GUIUtils.createFillGridData(1);
			answerLayoutData.verticalAlignment = SWT.FILL;
			answerButton.setLayoutData(answerLayoutData);
			answerButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if(qList.size() > 0 && wrongGuessesNum < MAX_ERRORS) {
						if (answerButton.getText().equals(newQuestion.getCorrectAns())) {
							score += 3;
							lastAnswer = "Correct! ";
							scoreLabel.setText(String.valueOf(score));
						}
						else {
							wrongGuessesNum++;
							score -= 2;
							lastAnswer = "Wrong... ";
							scoreLabel.setText(String.valueOf(score));
							}
						movingForward(true);
					}
				}
			});
			answerButtons.add(answerButton);
		}
		
		// create the "Pass" button to skip a question
		passButton = new Button(questionPanel, SWT.PUSH);
		passButton.setText("Pass");
		GridData data = new GridData(GridData.END, GridData.CENTER, true, false);
		data.horizontalSpan = 1;
		passButton.setLayoutData(data);
		passButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (isFirstPass)
					isFirstPass = false;
				else {
					score -= 1;
					scoreLabel.setText(String.valueOf(score));
				}
				movingForward(false);
			}
		});
		
		// create the "50-50" button to show fewer answer options
		fiftyFiftyButton = new Button(questionPanel, SWT.PUSH);
		fiftyFiftyButton.setText("50-50");
		data = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
		data.horizontalSpan = 1;
		fiftyFiftyButton.setLayoutData(data);
		fiftyFiftyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (isFirstFifty)
					isFirstFifty = false;
				else {
					score -= 1;
					scoreLabel.setText(String.valueOf(score));
				}
				Collections.shuffle(answerButtons);
				int DisabledCnt = 0;
				for (Button button : answerButtons) {
					if (DisabledCnt == 2)
						break;
					if (button.getText() != newQuestion.getCorrectAns()) {
						button.setEnabled(false);
						DisabledCnt++;
					}
				}
				fiftyFiftyButton.setEnabled(false);
			}
		});
		
		// enabling and disabling help buttons.
		if(score <= 0) {
			if (!isFirstPass)
				passButton.setEnabled(false);
			if (!isFirstFifty)
				fiftyFiftyButton.setEnabled(false);
		}
		else {
			if (!passButton.isEnabled())
				passButton.setEnabled(true);
			if (!fiftyFiftyButton.isEnabled())
				fiftyFiftyButton.setEnabled(true);
		}
		
		// two operations to make the new widgets display properly
		questionPanel.pack();
		questionPanel.getParent().layout();
	}

	/**
	 * Opens the main window and executes the event loop of the application
	 */
	private void runApplication() {
		shell.open();
		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		boldFont.dispose();
	}
}
