package ca.sqlpower.architect.swingui.action;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.architect.ddl.GenericDDLGenerator;
import ca.sqlpower.architect.etl.ETLUserSettings;
import ca.sqlpower.architect.etl.PLExport;
import ca.sqlpower.architect.etl.PLUtils;
import ca.sqlpower.architect.swingui.ASUtils;
import ca.sqlpower.architect.swingui.ArchitectFrame;
import ca.sqlpower.architect.swingui.ArchitectPanelBuilder;
import ca.sqlpower.architect.swingui.ArchitectSwingWorker;
import ca.sqlpower.architect.swingui.CommonCloseAction;
import ca.sqlpower.architect.swingui.EngineExecPanel;
import ca.sqlpower.architect.swingui.JDefaultButton;
import ca.sqlpower.architect.swingui.PLExportPanel;
import ca.sqlpower.architect.swingui.ProgressWatcher;
import ca.sqlpower.architect.swingui.QuickStartWizard;
import ca.sqlpower.architect.swingui.SwingUserSettings;
import ca.sqlpower.architect.swingui.WizardDialog;
import ca.sqlpower.security.PLSecurityException;

public class ExportPLTransAction extends AbstractAction {
	private static final Logger logger = Logger.getLogger(ExportPLTransAction.class);

	private ArchitectFrame architectFrame;
	private List<SQLTable> tables;

	/** The PLExport object that this action uses to create PL transactions. */
	private PLExport plexp;

	/** The dialog box that this action uses to configure plexp. */
	private JDialog d;

	/** Progress Bar to tell the user PL Export is still running */
	private JProgressBar plCreateTxProgressBar;
	private JLabel plCreateTxLabel;

	public ExportPLTransAction() {
		super("PL Transaction Export...",
			  ASUtils.createIcon("PLTransExport",
								 "PL Transaction Export",
								 ArchitectFrame.getMainInstance().getSprefs().getInt(SwingUserSettings.ICON_SIZE, 24)));
		architectFrame = ArchitectFrame.getMainInstance();
		putValue(SHORT_DESCRIPTION, "PL Transaction Export");
	}

	public void setExportingTables(List<SQLTable> exportingTables) {
        logger.debug("setExportingTables(): got new list of tables to export: "+exportingTables);
		tables = new ArrayList<SQLTable>(exportingTables);
	}
	
		
	/**
	 * Sets up the dialog the first time it is called.  After that,
	 * just returns without doing anything.
	 *
	 * <p>Note: the <code>plexp</code> variable must be initialized before calling this method, and
     * the exportTable list must also be initialized to list the tables you want in your PL job!
	 *
	 * @throws NullPointerException if <code>plexp</code> is null.
	 */
	public synchronized void setupDialog() {

		logger.debug("running setupDialog()");
		if (plexp == null) {
			throw new NullPointerException("setupDialog: plexp was null");
		}

		// always refresh Target Database (it might have changed)
		plexp.setTargetDataSource(ArchitectFrame.getMainInstance().getProject().getTargetDatabase().getDataSource());
				
		
		// Cannot use ArchitectPanelBuilder here yet because
		// of the progressbar.
		
		d = new JDialog(ArchitectFrame.getMainInstance(),
						"Export ETL Transactions to PL Repository");

		// set export defaults if necessary
		if (plexp.getFolderName() == null || plexp.getFolderName().trim().length() == 0) {
			plexp.setFolderName(PLUtils.toPLIdentifier(architectFrame.getProject().getName()+"_FOLDER"));
		}

		if (plexp.getJobId() == null || plexp.getJobId().trim().length() == 0) {
			plexp.setJobId(PLUtils.toPLIdentifier(architectFrame.getProject().getName()+"_JOB"));
		}
		
		JPanel plp = new JPanel(new BorderLayout(12,12));
		plp.setBorder(BorderFactory.createEmptyBorder(12,12,12,12)); 
		
		final PLExportPanel plPanel = new PLExportPanel();
		plPanel.setPLExport(plexp);
		plp.add(plPanel, BorderLayout.CENTER);
		
		// make an intermediate JPanel
		JPanel bottomPanel = new JPanel(new GridLayout(1,2,25,0)); // 25 pixel hgap		

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		JButton okButton = new JButton(ArchitectPanelBuilder.OK_BUTTON_LABEL);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if (!plPanel.applyChanges()) {
					return;
				}

				try {
					List targetDBWarnings = listMissingTargetTables(tables);
					if (!targetDBWarnings.isEmpty()) {
						// modal dialog (hold things up until the user says YES or NO)
						JList warnings = new JList(targetDBWarnings.toArray());
						JPanel cp = new JPanel(new BorderLayout());
						cp.add(new JLabel("<html>The target database schema is not identical to your Architect schema.<br><br>Here are the differences:</html>"), BorderLayout.NORTH);
						cp.add(new JScrollPane(warnings), BorderLayout.CENTER);
						cp.add(new JLabel("Do you want to continue anyway?"), BorderLayout.SOUTH);
						int choice = JOptionPane.showConfirmDialog(architectFrame, cp, "Target Database Structure Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if (choice != JOptionPane.YES_OPTION) {
							return;
						}
					}
					// got this far, so it's ok to run the PL Export thread
					ExportTxProcess etp = new ExportTxProcess(plexp,tables,d,
					        plCreateTxProgressBar,plCreateTxLabel);
					new Thread(etp).start();
				} catch (SQLException esql) {
					JOptionPane.showMessageDialog (architectFrame,"Can't export Transaction: "+esql.getMessage());
					logger.error("Got exception while exporting Trans", esql);
					return;
				} catch (ArchitectException arex){
					JOptionPane.showMessageDialog (architectFrame,"Can't export Transaction: "+arex.getMessage());
					logger.error("Got exception while exporting Trans",arex);
					return;
				}		
				
			}
		});
		buttonPanel.add(okButton);

		Action cancelAction = new AbstractAction() {
				public void actionPerformed(ActionEvent evt) {
					plPanel.discardChanges();
					d.setVisible(false);
				}
		};
		cancelAction.putValue(Action.NAME, ArchitectPanelBuilder.CANCEL_BUTTON_LABEL);
		ArchitectPanelBuilder.makeJDialogCancellable(d, cancelAction);
		d.getRootPane().setDefaultButton(okButton);
		JButton cancelButton = new JButton(cancelAction);
		buttonPanel.add(cancelButton);

		// stick in the progress bar here...
		JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));		
 	    plCreateTxProgressBar = new JProgressBar();
		plCreateTxProgressBar.setStringPainted(true); 
		progressPanel.add(plCreateTxProgressBar);		
	    plCreateTxLabel = new JLabel ("Exporting PL Transactions...");
		progressPanel.add(plCreateTxLabel);
		
		// figure out how much space this needs before setting 
		// child components to be invisible
		progressPanel.setPreferredSize(progressPanel.getPreferredSize());  
		plCreateTxProgressBar.setVisible(false);		
		plCreateTxLabel.setVisible(false);		

		bottomPanel.add(progressPanel); // left side, left justified
		bottomPanel.add(buttonPanel); // right side, right justified

		plp.add(bottomPanel, BorderLayout.SOUTH);
		
		d.setContentPane(plp);
		
		// experiment with preferred size crap:
		d.pack();
		d.setLocationRelativeTo(ArchitectFrame.getMainInstance());
	}


	public void actionPerformed(ActionEvent e) {
		plexp = architectFrame.getProject().getPLExport();
		setupDialog();
		d.setVisible(true); 
	}

		
	/**
	 * Checks for missing tables in the target database.  Returns a
	 * list of table names that need to be created.
	 */
	public List listMissingTargetTables(List targetTables) throws SQLException, ArchitectException {
		List missingTables = new LinkedList();
		Iterator targetTableIt = targetTables.iterator();
		while (targetTableIt.hasNext()) {
			SQLTable t = (SQLTable) targetTableIt.next();
			String tableStatus = checkTargetTable(t);
			if (tableStatus != null) {
				missingTables.add(tableStatus);
			}
		}
		return missingTables;
	}
	
	/**
	 * Checks for the existence of the given table in the actual
	 * target database, and also compares its columns to those of the
	 * actual table (if the table exists in the target database).
	 *
	 * @return A short message describing the differences between the
	 * given table <code>t</code> and its counterpart in the physical
	 * target database.  If the actual target table is identical to
	 * <code>t</code>, returns <code>null</code>.
	 */
	private String checkTargetTable(SQLTable t) throws SQLException, ArchitectException {
		GenericDDLGenerator ddlg = ArchitectFrame.getMainInstance().getProject().getDDLGenerator();
		logger.debug("DDLG class is: " + ddlg.getClass().getName());
		String tableName = ddlg.toIdentifier(t.getName());
		List ourColumns = new ArrayList();
		Iterator it = t.getColumns().iterator();
		while (it.hasNext()) {
			SQLColumn c = (SQLColumn) it.next();
			ourColumns.add(ddlg.toIdentifier(c.getName()).toLowerCase());
		}

		List actualColumns = new ArrayList();
		Connection con = null;
		ResultSet rs = null;
		try {
			con = t.getParentDatabase().getConnection();
			DatabaseMetaData dbmd = con.getMetaData();
			logger.debug("Fetching columns of "+plexp.getTargetSchema()+"."+tableName);
			rs = dbmd.getColumns(null, plexp.getTargetSchema(), tableName, null);
			while (rs.next()) {
				actualColumns.add(rs.getString(4).toLowerCase()); // column name
			}
		} finally {
			try {
				if (rs != null) rs.close();
			} catch (SQLException ex) {
				logger.error("Could not close result set", ex);
			}
			try {
				if (con != null) con.close();
			} catch (SQLException ex) {
				logger.error("Could not close connection", ex);
			}
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("   ourColumns = "+ourColumns);
			logger.debug("actualColumns = "+actualColumns);
		}
		if (actualColumns.isEmpty()) {
			return "Target table \""+tableName+"\" does not exist";
		} else {
			if (actualColumns.containsAll(ourColumns)) {
				return null;
			} else {
				return "Target table \""+tableName+"\" exists but is missing columns";
			}
		}
	}
	
	public class ExportTxProcess extends ArchitectSwingWorker {		
		
		PLExport plExport;
		final JDialog d;
		private Runnable nextProcess;
        private List<SQLTable> exportTables;

		public ExportTxProcess(PLExport plExport,List<SQLTable> exportTables, JDialog parentDialog,
				JProgressBar progressBar, JLabel label) {
			this.plExport = plExport;
            this.exportTables = exportTables;
            logger.debug("Creating new ExportTxProcess for tables: "+exportTables);
			d = parentDialog;
			label.setText("Exporting Meta Data...");			
			new ProgressWatcher(progressBar, plExport, label);			
		}		

		public void doStuff() {
			if (isCanceled())
				return;
			// now implements Monitorable, so we can ask it how it's doing
			try {
				plExport.export(exportTables);
				// if the user requested, try running the PL Job afterwards
				if (plExport.getRunPLEngine()) {
					logger.debug("Running PL Engine");
					File plEngine = new File(architectFrame.getUserSettings().getETLUserSettings().getString(ETLUserSettings.PROP_PL_ENGINE_PATH,""));					
					File plDir = plEngine.getParentFile();
					File engineExe = new File(plDir, PLUtils.getEngineExecutableName(plExport.getRepositoryDataSource()));
					final StringBuffer commandLine = new StringBuffer(1000);
					commandLine.append(engineExe.getPath());
					commandLine.append(" USER_PROMPT=N");
					commandLine.append(" JOB=").append(plExport.getJobId());            	
					commandLine.append(" USER=").append(PLUtils.getEngineConnectString(plExport.getRepositoryDataSource()));
					commandLine.append(" DEBUG=N SEND_EMAIL=N SKIP_PACKAGES=N CALC_DETAIL_STATS=N COMMIT_FREQ=100 APPEND_TO_JOB_LOG_IND=N");
					commandLine.append(" APPEND_TO_JOB_ERR_IND=N");
					commandLine.append(" SHOW_PROGRESS=100");
					commandLine.append(" SHOW_PROGRESS=10");
					logger.debug(commandLine.toString());
					// worker thread must not talk to Swing directly...
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							try {
								final Process proc = Runtime.getRuntime().exec(commandLine.toString());
								// Could in theory make this use ArchitectPanelBuilder, by creating
								// a JPanel subclass, but may not be worthwhile as it has both an
								// Abort and a Close button...
								final JDialog pld = new JDialog(architectFrame, "Power*Loader Engine");
									
								EngineExecPanel eep = new EngineExecPanel(commandLine.toString(), proc);
								pld.setContentPane(eep);
								
								Action closeAction = new CommonCloseAction(pld);
								JButton abortButton = new JButton(eep.getAbortAction());
								JDefaultButton closeButton = new JDefaultButton(closeAction);
                           		
                           		
								JCheckBox scrollLockCheckBox = new JCheckBox(eep.getScrollBarLockAction());
            	       			
								JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
								buttonPanel.add(abortButton);
								buttonPanel.add(closeButton);
								buttonPanel.add(scrollLockCheckBox);
								eep.add(buttonPanel, BorderLayout.SOUTH);
								
								// XXX what should "<Escape> do to a dialog that has
								// both an Abort and a Close button??
								// ArchitectPanelBuilder.makeJDialogCancellable(pld,
								//		eep.getAbortAction(), closeAction);
								
								pld.getRootPane().setDefaultButton(closeButton);
								pld.pack();
								pld.setLocationRelativeTo(d);
								pld.setVisible(true);
							} catch (IOException ie){
								JOptionPane.showMessageDialog(architectFrame, "Unexpected Exception running Engine:\n"+ie);
								logger.error("IOException while trying to run engine.",ie);
							}
						}
					});
				}
			} catch (PLSecurityException ex) {
				final Exception fex = ex;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog
							(architectFrame,
							 "Can't export Transaction: "+fex.getMessage());
						logger.error("Got exception while exporting Trans", fex);	
					}
				});
			} catch (SQLException esql) {
				final Exception fesql = esql;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog
							(architectFrame,
							 "Can't export Transaction: "+fesql.getMessage());
						logger.error("Got exception while exporting Trans", fesql);
					}
				});
			} catch (ArchitectException arex){
				final Exception farex = arex;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog
							(architectFrame,
							 "Can't export Transaction: "+farex.getMessage());
						logger.error("Got exception while exporting Trans", farex);
					}
				});
			} 
		}

		@Override
		public void cleanup() throws Exception {
			if (!(d instanceof WizardDialog))
				d.setVisible(false);
			else {
				if (nextProcess != null)
					new Thread(nextProcess).start();
				WizardDialog wd = ((WizardDialog)d);
				((QuickStartWizard)wd.getWizard()).UpdateTextArea();
				
			}
		}

	}

    public List<SQLTable> getExportingTables() {
        return tables;
    }	
}