/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.sqlpower.architect.swingui.action;

import ca.sqlpower.architect.swingui.ArchitectFrame;
import ca.sqlpower.architect.swingui.ArchitectSwingSession;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.swingui.SPSUtils;
import ca.sqlpower.util.BrowserUtil;
import ca.sqlpower.util.XsltTransformation;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.apache.log4j.Logger;


/**
 *
 * A panel to select the XSLT stylesheet and the output file to be generated.
 *
 * The panel has a start and close button so that it is self-contained.
 * showDialog() will display this panel in a non-modal JDialog.
 *
 * The last 15 selected XSLT transformations will be remembered and made available
 * through a dropdown box.
 */
public class ExportHTMLPanel
		extends JPanel
		implements ActionListener {

	private static final Logger logger = Logger.getLogger(ExportHTMLPanel.class);
	
	private JRadioButton builtin;
	private JRadioButton external;
	private JComboBox xsltFile;
	private JButton selectXslt;
	private JButton selectOutput;
	private JButton startButton;
	private JButton closeButton;
	private JTextField outputFile;

	private JLabel statusBar;
	
	private ArchitectSwingSession session;

	private JDialog dialog;

	private static final String ENCODING = "UTF-8";
	private PipedOutputStream xmlOutputStream;
	private FileOutputStream result;

	private static final String PREF_KEY_BUILTIN = "htmlgen.builtin";
	private static final String PREF_KEY_LAST_XSLT = "htmlgen.lastxslt";
	private static final String PREF_KEY_XSLT_HISTORY = "htmlgen.xslt.recent";
	private static final String PREF_KEY_OUTPUT = "htmlgen.lastoutput";
	private static final int MAX_HISTORY_ENTRIES = 15;
	
	public ExportHTMLPanel(ArchitectSwingSession architect) {

		super(new BorderLayout());
		session = architect;
		JPanel content = new JPanel(new GridBagLayout());

		ButtonGroup group = new ButtonGroup();
		builtin = new JRadioButton(Messages.getString("XSLTSelectionPanel.labelBuiltIn"));
		external = new JRadioButton(Messages.getString("XSLTSelectionPanel.labelExternal"));
		group.add(builtin);
		group.add(external);

		// place Radio buttons
		GridBagConstraints cc = new GridBagConstraints();
		cc.gridx = 0;
		cc.gridy = 0;
		cc.anchor = GridBagConstraints.WEST;
		cc.gridwidth = 3;
		content.add(builtin, cc);

		cc = new GridBagConstraints();
		cc.gridx = 0;
		cc.gridy = 1;
		cc.anchor = GridBagConstraints.WEST;
		cc.gridwidth = 3;
		content.add(external, cc);

		// Selection of XSLT file
		cc = new java.awt.GridBagConstraints();
		cc.gridx = 0;
		cc.gridy = 2;
		cc.anchor = GridBagConstraints.WEST;
		content.add(new JLabel(Messages.getString("XSLTSelectionPanel.labelTransformation")), cc);
		
		xsltFile = new JComboBox();
		xsltFile.setRenderer(new ComboTooltipRenderer());
		xsltFile.setEditable(true);
		cc = new GridBagConstraints();
		cc.gridx = 1;
		cc.gridy = 2;
		cc.fill = GridBagConstraints.HORIZONTAL;
		cc.weightx = 1.0;
		cc.insets = new java.awt.Insets(0, 6, 0, 0);
		content.add(xsltFile, cc);

		selectXslt = new JButton("...");
		cc = new GridBagConstraints();
		cc.gridx = 2;
		cc.gridy = 2;
		cc.insets = new java.awt.Insets(0, 6, 0, 7);
		content.add(selectXslt, cc);

		// Output selection
		cc = new java.awt.GridBagConstraints();
		cc.gridx = 0;
		cc.gridy = 3;
		cc.anchor = GridBagConstraints.WEST;
		content.add(new JLabel(Messages.getString("XSLTSelectionPanel.labelOutput")), cc);

		outputFile = new JTextField(30);
		cc = new GridBagConstraints();
		cc.gridx = 1;
		cc.gridy = 3;
		cc.fill = GridBagConstraints.HORIZONTAL;
		cc.weightx = 1.0;
		cc.insets = new java.awt.Insets(0, 6, 0, 0);
		content.add(outputFile, cc);

		selectOutput = new JButton("...");
		cc = new GridBagConstraints();
		cc.gridx = 2;
		cc.gridy = 3;
		cc.insets = new java.awt.Insets(0, 6, 0, 7);
		content.add(selectOutput, cc);

		// "Statusbar"
		cc = new GridBagConstraints();
		cc.gridx = 0;
		cc.gridy = 4;
		cc.gridwidth = 3;
		cc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		cc.anchor = java.awt.GridBagConstraints.NORTHWEST;
		cc.weighty = 1.0;
		cc.insets = new java.awt.Insets(6, 0, 0, 6);
		statusBar = new JLabel(" ");

		content.add(statusBar, cc);
		
		selectXslt.addActionListener(this);
		selectOutput.addActionListener(this);
		builtin.addActionListener(this);
		external.addActionListener(this);
		builtin.setSelected(true);

		add(content, BorderLayout.CENTER);

		startButton = new JButton(Messages.getString("XSLTSelectionPanel.startOption"));
		startButton.addActionListener(this);
		
		closeButton = new JButton(Messages.getString("XSLTSelectionPanel.closeOption"));
		closeButton.addActionListener(this);

		JPanel bp = ButtonBarFactory.buildRightAlignedBar(startButton, closeButton);
		add(bp, BorderLayout.SOUTH);

		setBorder(Borders.DIALOG_BORDER);
		
		restoreSettings();
	}

	/**
	 * Displays this selection panel to the user.
	 *
	 * @return true if the user clicked OK, false otherwise
	 */
	public void showDialog() {

		if (dialog == null) {
			ArchitectFrame frame = session.getArchitectFrame();

			dialog = new JDialog(frame, Messages.getString("XSLTSelectionPanel.dialogTitle"));
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			SPSUtils.makeJDialogCancellable(dialog, new AbstractAction() {

				public void actionPerformed(ActionEvent e) {
					closeDialog();
				}
			});
			dialog.setContentPane(this);
			dialog.pack();
			dialog.setLocationRelativeTo(frame);
		}
        dialog.setVisible(true);
	}

	/**
	 * Return the filename the user selected. If the user
	 * chose to use the built-in XSLT, this returns null
	 *
	 * @return the filename selected by the user, or null if the internal
	 *  XSLT should be used.
	 */
	public File getXsltFile() {
		if (builtin.isSelected()) {
			return null;
		}
		Object o = xsltFile.getSelectedItem();
		if (o instanceof File) {
			return (File)o;
		} else if (o instanceof String) {
			// might happen if the user entered the filename manually
			return new File((String)o);
		}
		return null;
	}

	public String getOutputFilename() {
		return outputFile.getText();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == selectXslt) {
			selectXslt();
		} else if (e.getSource() == selectOutput) {
			selectOutput();
		} else if (e.getSource() == startButton) {
			transformFile();
		} else if (e.getSource() == closeButton) {
			closeDialog();
		} else if (e.getSource() == xsltFile) {
			checkDropDown();
		} else if (e.getSource() == builtin) {
			checkDropDown();
		} else if (e.getSource() == external) {
			checkDropDown();
		}
	}

	private void checkDropDown() {
		File f = this.getXsltFile();
		if (f == null) {
			builtin.setSelected(true);
			xsltFile.setEnabled(false);
		} else {
			external.setSelected(true);
			xsltFile.setEnabled(true);
			xsltFile.setToolTipText(getFullName(f));
		}
	}

	private void syncDropDown() {
		// if the user pasted the filename into the editable part
		// of the dropdown, this will not be part of the actual dropdown items
		// so I'm adding that here "manually"
		File current = getXsltFile();

		if (current == null) return;
		boolean found = false;

		int numEntries = xsltFile.getItemCount() ;
		for (int index = 0; index < numEntries; index++) {
			if (xsltFile.getItemAt(index).equals(current)) {
				found = true;
				break;
			}
		}

		if (!found) {
			xsltFile.addItem(new ComboBoxFile(current));
		}
	}

	private void saveSettings() {
		Preferences prefs = Preferences.userNodeForPackage(getClass());
		prefs.putBoolean(PREF_KEY_BUILTIN, builtin.isSelected());
		prefs.put(PREF_KEY_OUTPUT, outputFile.getText());

		syncDropDown();
		
		File f = getXsltFile();
		if (f != null) {
			prefs.put(PREF_KEY_LAST_XSLT, getFullName(f));
		} else {
			prefs.remove(PREF_KEY_LAST_XSLT);
		}

		int numEntries = (xsltFile.getItemCount() > MAX_HISTORY_ENTRIES ? MAX_HISTORY_ENTRIES : xsltFile.getItemCount());
		
		for (int i=0; i < numEntries; i++) {
			Object o = xsltFile.getItemAt(i);
			String key = PREF_KEY_XSLT_HISTORY + "." + i;
			if (o instanceof File) {
				prefs.put(key, getFullName((File)o));
			} else if (o instanceof String) {
				prefs.put(key, (String)o);
			} else {
				prefs.remove(key);
			}
		}
	}

	private void restoreSettings() {
		Preferences prefs = Preferences.userNodeForPackage(getClass());
		final boolean useBuiltin = prefs.getBoolean(PREF_KEY_BUILTIN, true);
		builtin.setSelected(useBuiltin);
		external.setSelected(!useBuiltin);
		EventQueue.invokeLater(new Runnable() {

			public void run() {
				if (useBuiltin) {
					builtin.requestFocusInWindow();
				} else {
					external.requestFocusInWindow();
				}
			}
		});

		// Restore the history
		for (int i=0; i < 15; i++) {
			String fname = prefs.get(PREF_KEY_XSLT_HISTORY + "." + i, null);
			if (fname == null) break;
			ComboBoxFile f = new ComboBoxFile(fname);
			xsltFile.addItem(f);
		}

		// The last used XSLT
		String file = prefs.get(PREF_KEY_LAST_XSLT, null);
		if (file != null) {
			ComboBoxFile f = new ComboBoxFile(file);
			xsltFile.setSelectedItem(f);
		}
		outputFile.setText(prefs.get(PREF_KEY_OUTPUT, ""));
		checkDropDown();
	}

	public static String getFullName(File fo) {
		if (fo == null) return null;
		try {
			return fo.getCanonicalPath();
		} catch (IOException io) {
			return fo.getAbsolutePath();
		}
	}


	private void selectXslt() {
		JFileChooser chooser = new JFileChooser(session.getProject().getFile());
		chooser.addChoosableFileFilter(SPSUtils.XSLT_FILE_FILTER);
		chooser.setDialogTitle(Messages.getString("XSLTSelectionPanel.selectXsltTitle"));
		int response = chooser.showOpenDialog(session.getArchitectFrame());
		if (response != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File file = chooser.getSelectedFile();

		ComboBoxFile cf = new ComboBoxFile(file);
		external.setSelected(true);
		xsltFile.setEnabled(true);
		xsltFile.addItem(cf);
		xsltFile.setSelectedItem(cf);
	}

	private void closeDialog() {
		if (dialog != null) {
			saveSettings();
			dialog.setVisible(false);
			dialog.dispose();
		}
	}

	private void selectOutput() {
		JFileChooser chooser = new JFileChooser(session.getProject().getFile());
		chooser.addChoosableFileFilter(SPSUtils.HTML_FILE_FILTER);
		chooser.setDialogTitle(Messages.getString("XSLTSelectionPanel.saveAsTitle"));
		
		int response = chooser.showSaveDialog(session.getArchitectFrame());

		if (response != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File file = chooser.getSelectedFile();
		if (!file.getPath().endsWith(".html")) { //$NON-NLS-1$
			file = new File(file.getPath() + ".html"); //$NON-NLS-1$
		}

		try {
			outputFile.setText(file.getCanonicalPath());
		} catch (IOException io) {
			outputFile.setText(file.getAbsolutePath());
		}

	}

	/**
	 * Transforms the current playpen according to the selection that the user made.
	 * <br/>
	 * An xml OutputStream(using a {@link PipedOutputStream}) is generated, based on the
	 * current playPen content and is read by a {@link PipedInputStream} which is used as the xml source. 
	 * <br/>
	 * The stylesheet and the xml source are passed as parameters to the
	 * {@link XsltTransformation} methods to generate an HTML report off the content
	 * to a location specified by the user.
	*/
	protected void transformFile() {

		File file = new File(outputFile.getText());
		
		if (file.exists()) {
			int response = JOptionPane.showConfirmDialog(session.getArchitectFrame(),
			  Messages.getString("XSLTSelectionPanel.fileAlreadyExists", file.getPath()), //$NON-NLS-1$
					Messages.getString("XSLTSelectionPanel.fileAlreadyExistsDialogTitle"),
					JOptionPane.YES_NO_OPTION); //$NON-NLS-1$
			if (response == JOptionPane.NO_OPTION) {
				return;
			}
		}

		statusBar.setText(Messages.getString("XSLTSelectionPanel.msgGenerating"));
		Thread t = new Thread(new Runnable() {
			public void run() {
				_transformFile();
			}
		});
		t.setName("HTML Generation Thread");
		t.setDaemon(true);
		t.start();
	}
	
	protected void _transformFile() {
		PipedInputStream xmlInputStream = new PipedInputStream();
		try {
			xmlOutputStream = new PipedOutputStream(xmlInputStream);
			new Thread(
					new Runnable() {

						public void run() {
							try {
								session.getProject().save(xmlOutputStream, ENCODING);
							} catch (IOException e2) {
								SPSUtils.showExceptionDialogNoReport(session.getArchitectFrame(), "You got an error", e2);
							} catch (SQLObjectException e2) {
								SPSUtils.showExceptionDialogNoReport(session.getArchitectFrame(), "You got an error", e2);
							}
						}
					}).start();
			xmlOutputStream.flush();

		} catch (IOException e2) {
			SPSUtils.showExceptionDialogNoReport(session.getArchitectFrame(), "You got an error", e2);
		}

		File file = new File(getOutputFilename());
		try {
			result = new FileOutputStream(file);
		} catch (FileNotFoundException e2) {
			SPSUtils.showExceptionDialogNoReport(session.getArchitectFrame(), "You got an error", e2);
		}

		XsltTransformation xsltTransform = new XsltTransformation();

		InputStream xsltInput = null;
		try {
			File xslt = getXsltFile();
			if (xslt == null) {
				xsltTransform.transform("/xsltStylesheets/architect2html.xslt", xmlInputStream, result);
			} else {
				xsltInput = new FileInputStream(xslt);
				xsltTransform.transform(xsltInput, xmlInputStream, result);
			}

			EventQueue.invokeLater(new Runnable() {

				public void run() {
					statusBar.setText(Messages.getString("XSLTSelectionPanel.msgStartingBrowser"));
				}
			});

			//Opens up the html file in the default browser
			BrowserUtil.launch(file.toURI().toString());
		} catch (Exception e1) {
			SPSUtils.showExceptionDialogNoReport(session.getArchitectFrame(), "You got an error", e1);
		} finally {
			closeQuietly(result);
			closeQuietly(xmlInputStream);
			closeQuietly(xmlOutputStream);
		}

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				statusBar.setText("");
			}
		});

	}

	private void closeQuietly(Closeable stream) {
		try {
			stream.close();
		} catch (IOException io) {
			logger.error("Error closing file", io);
		}

	}

}
class ComboBoxFile
	extends File {

	public ComboBoxFile(File f) {
		super(f.getAbsolutePath());
	}

	public ComboBoxFile(String pathname) {
		super(pathname);
	}

	public String toString() {
		return getName();
	}
}

class ComboTooltipRenderer extends DefaultListCellRenderer {

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		JComponent comp = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected,
				cellHasFocus);

		if (value instanceof File) {
			comp.setToolTipText(ExportHTMLPanel.getFullName((File)value));
		} else {
			comp.setToolTipText(null);
		}
		return comp;
	}
}
