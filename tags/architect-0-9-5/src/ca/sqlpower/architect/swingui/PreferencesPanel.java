package ca.sqlpower.architect.swingui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import ca.sqlpower.architect.CoreUserSettings;
import ca.sqlpower.architect.ddl.DDLUserSettings;
import ca.sqlpower.architect.etl.ETLUserSettings;
import ca.sqlpower.architect.qfa.QFAUserSettings;

public class PreferencesPanel extends JPanel implements ArchitectPanel {

	/**
	 * The settings we're editing
	 */
	protected CoreUserSettings us;

	protected JTextField plIniName;
	protected JButton plIniButton;

	protected JTextField powerLoaderEngine;
	protected JButton powerLoaderEngineButton;

	protected JTextField etlLogFileName;
	protected JButton etlLogFileButton;

	protected JTextField ddlLogFileName;
	protected JButton ddlLogFileButton;

	protected JRadioButton playPenAntialiasOn;
	protected JRadioButton playPenAntialiasOff;

    protected JRadioButton exceptionReportOn;
    protected JRadioButton exceptionReportOff;

    protected JRadioButton showWelcomeOn;
    protected JRadioButton showWelcomeOff;


	public PreferencesPanel(CoreUserSettings us) {
		this.us = us;
		setup();
		revertToUserSettings();
	}

	public void setup() {
		setLayout(new FormLayout(5,5));
		// line 1
		add(new JLabel("Power*Loader PL.INI File"));
		JPanel plIniPanel = new JPanel();
		plIniPanel.setLayout(new BorderLayout());
		plIniPanel.add(plIniName = new JTextField("",35), BorderLayout.WEST);
		plIniPanel.add(plIniButton = new JButton(), BorderLayout.EAST);
		plIniButton.setAction(new ChooseFileAction(plIniName,ASUtils.INI_FILE_FILTER,"Browse..."));
		add(plIniPanel);
		// line 2
		add(new JLabel("Power*Loader Engine"));
		JPanel plEnginePanel = new JPanel();
		plEnginePanel.setLayout(new BorderLayout());
		plEnginePanel.add(powerLoaderEngine = new JTextField("",35), BorderLayout.WEST);
		plEnginePanel.add(powerLoaderEngineButton = new JButton(), BorderLayout.EAST);
		powerLoaderEngineButton.setAction(new ChooseFileAction(powerLoaderEngine,ASUtils.EXE_FILE_FILTER,"Browse..."));
		add(plEnginePanel);
		// line 3
		add(new JLabel("ETL Log File"));
		JPanel etlLogFilePanel = new JPanel();
		etlLogFilePanel.setLayout(new BorderLayout());
		etlLogFilePanel.add(etlLogFileName = new JTextField("",35), BorderLayout.WEST);
		etlLogFilePanel.add(etlLogFileButton = new JButton(), BorderLayout.EAST);
		etlLogFileButton.setAction(new ChooseFileAction(etlLogFileName,ASUtils.LOG_FILE_FILTER,"Browse..."));
		add(etlLogFilePanel);

		// line 4
		add(new JLabel("Forward Engineering Log File"));
		JPanel ddlLogFilePanel = new JPanel();
		ddlLogFilePanel.setLayout(new BorderLayout());
		ddlLogFilePanel.add(ddlLogFileName = new JTextField("",35), BorderLayout.WEST);
		ddlLogFilePanel.add(ddlLogFileButton = new JButton(), BorderLayout.EAST);
		ddlLogFileButton.setAction(new ChooseFileAction(ddlLogFileName,ASUtils.LOG_FILE_FILTER,"Browse..."));
		add(ddlLogFilePanel);

		// line 5
		add(new JLabel("Antialiased Rendering in PlayPen"));
		JPanel playPenAntialiasPanel = new JPanel();
		playPenAntialiasPanel.setLayout(new FlowLayout());
		ButtonGroup playPenAntialiasGroup = new ButtonGroup();
		playPenAntialiasGroup.add(playPenAntialiasOn = new JRadioButton("On"));
		playPenAntialiasGroup.add(playPenAntialiasOff = new JRadioButton("Off"));
		playPenAntialiasPanel.add(playPenAntialiasOn);
		playPenAntialiasPanel.add(playPenAntialiasOff);
		add(playPenAntialiasPanel);

        //line 6
        add(new JLabel("Error Reporting"));
        JPanel exceptionReportPanel = new JPanel();
        exceptionReportPanel.setLayout(new FlowLayout());
        ButtonGroup exceptionReportGroup = new ButtonGroup();
        exceptionReportGroup.add(exceptionReportOn = new JRadioButton("On"));
        exceptionReportGroup.add(exceptionReportOff = new JRadioButton("Off"));
        exceptionReportPanel.add(exceptionReportOn);
        exceptionReportPanel.add(exceptionReportOff);
        add(exceptionReportPanel);
        //line 7
        add(new JLabel("Show Welcome Screen"));
        JPanel showWelcomePanel = new JPanel();
        showWelcomePanel.setLayout(new FlowLayout());
        ButtonGroup showWelcomeGroup = new ButtonGroup();
        showWelcomeGroup.add(showWelcomeOn = new JRadioButton("Yes"));
        showWelcomeGroup.add(showWelcomeOff = new JRadioButton("No"));
        showWelcomePanel.add(showWelcomeOn);
        showWelcomePanel.add(showWelcomeOff);
        add(showWelcomePanel);
	}

	protected void revertToUserSettings() {
		plIniName.setText(us.getPlDotIniPath());
		powerLoaderEngine.setText(us.getETLUserSettings().getString(ETLUserSettings.PROP_PL_ENGINE_PATH,""));
		etlLogFileName.setText(us.getETLUserSettings().getString(ETLUserSettings.PROP_ETL_LOG_PATH,""));
		ddlLogFileName.setText(us.getDDLUserSettings().getString(DDLUserSettings.PROP_DDL_LOG_PATH,""));
		if (us.getSwingSettings().getBoolean(SwingUserSettings.PLAYPEN_RENDER_ANTIALIASED, false)) {
		    playPenAntialiasOn.setSelected(true);
		} else {
		    playPenAntialiasOff.setSelected(true);
		}
        if (us.getSwingSettings().getBoolean(SwingUserSettings.SHOW_WELCOMESCREEN, true)) {
            showWelcomeOn.setSelected(true);
        } else {
            showWelcomeOff.setSelected(true);
        }
        if (us.getQfaUserSettings().getBoolean(QFAUserSettings.EXCEPTION_REPORTING, true)) {
            exceptionReportOn.setSelected(true);
        } else {
            exceptionReportOff.setSelected(true);
        }
	}

	public boolean applyChanges() {
		us.setPlDotIniPath(plIniName.getText());
	    us.getETLUserSettings().setString(ETLUserSettings.PROP_PL_ENGINE_PATH,powerLoaderEngine.getText());
		us.getETLUserSettings().setString(ETLUserSettings.PROP_ETL_LOG_PATH,etlLogFileName.getText());
		us.getDDLUserSettings().setString(DDLUserSettings.PROP_DDL_LOG_PATH,ddlLogFileName.getText());
		us.getSwingSettings().setBoolean(SwingUserSettings.PLAYPEN_RENDER_ANTIALIASED, playPenAntialiasOn.isSelected());
        us.getSwingSettings().setBoolean(SwingUserSettings.SHOW_WELCOMESCREEN, showWelcomeOn.isSelected());
        us.getQfaUserSettings().setBoolean(QFAUserSettings.EXCEPTION_REPORTING, exceptionReportOn.isSelected());
		ArchitectFrame.getMainInstance().getProject().getPlayPen().setRenderingAntialiased(playPenAntialiasOn.isSelected());
		return true;
	}

	public void discardChanges() {
		revertToUserSettings();
	}

	// generic action for browse buttons
	protected class ChooseFileAction extends AbstractAction {
		JTextField fileName;
		FileFilter filter;
		public ChooseFileAction(JTextField fileName, FileFilter filter, String buttonText) {
			super(buttonText);
			this.fileName = fileName;
			this.filter = filter;
		}
		public void actionPerformed(ActionEvent e) {
			JFileChooser fc = new JFileChooser();
			if (fileName.getText() != null && fileName.getText().length() > 0) {
				File initialLocation = new File(fileName.getText());
				if (initialLocation.exists()) {
					fc.setCurrentDirectory(initialLocation);
				}
			}
			fc.addChoosableFileFilter(filter);
			int returnVal = fc.showOpenDialog(PreferencesPanel.this);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				fileName.setText(fc.getSelectedFile().getAbsolutePath());
			}
		}
	}

	public JPanel getPanel() {
		return this;
	}
}