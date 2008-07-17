/*
 * Copyright (c) 2008, SQL Power Group Inc.
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
package ca.sqlpower.architect.swingui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLType;
import ca.sqlpower.swingui.DataEntryPanel;

public class ColumnEditPanel extends JPanel
	implements ActionListener, DataEntryPanel {

	private static final Logger logger = Logger.getLogger(ColumnEditPanel.class);

    /**
     * The column we're editing.
     */
    private SQLColumn column;

	private JLabel sourceDB;
	private JLabel sourceTableCol;
	private JTextField colName;
	private JComboBox colType;
	private JSpinner colScale;
	private JSpinner colPrec;
	private JCheckBox colNullable;
	private JTextField colRemarks;
	private JTextField colDefaultValue;
	private JCheckBox colInPK;
	private JCheckBox colAutoInc;
    private JTextField colAutoIncSequenceName;

    /**
     * The prefix string that comes before the current column name
     * in the sequence name.  This is set via the {@link #discoverSequenceNamePattern()}
     * method, which should be called automatically whenever the user
     * changes the sequence name.
     */
    private String seqNamePrefix;

    /**
     * The suffix string that comes after the current column name
     * in the sequence name.  This is set via the {@link #discoverSequenceNamePattern()}
     * method, which should be called automatically whenever the user
     * changes the sequence name.
     */
    private String seqNameSuffix;

	public ColumnEditPanel(SQLColumn col) throws ArchitectException {
		super(new BorderLayout(12,12));
		logger.debug("ColumnEditPanel called");
        buildUI();
		editColumn(col);
	}

    private void buildUI() {
        JPanel centerBox = new JPanel();
		centerBox.setLayout(new BoxLayout(centerBox, BoxLayout.Y_AXIS));
		centerBox.add(Box.createVerticalGlue());
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new FormLayout(5, 5));

		centerPanel.add(new JLabel("Source Database"));
		centerPanel.add(sourceDB = new JLabel());

		centerPanel.add(new JLabel("Source Table.Column"));
		centerPanel.add(sourceTableCol = new JLabel());

		centerPanel.add(new JLabel("Name"));
		centerPanel.add(colName = new JTextField());


		centerPanel.add(new JLabel("Type"));
		centerPanel.add(colType = createColTypeEditor());
		colType.addActionListener(this);

		centerPanel.add(new JLabel("Precision"));
		centerPanel.add(colPrec = createPrecisionEditor());
		
		centerPanel.add(new JLabel("Scale"));
		centerPanel.add(colScale = createScaleEditor());

		centerPanel.add(new JLabel("In Primary Key"));
		centerPanel.add(colInPK = new JCheckBox());
		colInPK.addActionListener(this);

		centerPanel.add(new JLabel("Allows Nulls"));
		centerPanel.add(colNullable = new JCheckBox());
		colNullable.addActionListener(this);

        centerPanel.add(new JLabel("Auto Increment"));
        centerPanel.add(colAutoInc = new JCheckBox());
        colAutoInc.addActionListener(this);

        centerPanel.add(new JLabel("Sequence Name"));
        centerPanel.add(colAutoIncSequenceName = new JTextField());
        centerPanel.add(new JLabel(""));
        centerPanel.add(new JLabel("Only applies to target platforms that use sequences"));
        
        // Listener to update the sequence name when the column name changes
        colName.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { doSync(); }
            public void insertUpdate(DocumentEvent e) { doSync(); }
            public void removeUpdate(DocumentEvent e) { doSync(); }
            private void doSync() {
                syncSequenceName();
            }
        });

        // Listener to rediscover the sequence naming convention, and reset the sequence name
        // to its original (according to the column's own sequence name) naming convention when
        // the user clears the sequence name field
        colAutoIncSequenceName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (colAutoIncSequenceName.getText().trim().equals("")) {
                    colAutoIncSequenceName.setText(column.getAutoIncrementSequenceName());
                    discoverSequenceNamePattern(column.getName());
                    syncSequenceName();
                } else {
                    discoverSequenceNamePattern(colName.getText());
                }
            }
        });
        
        
		centerPanel.add(new JLabel("Remarks"));
		centerPanel.add(colRemarks = new JTextField());
	
		centerPanel.add(new JLabel("Default Value"));
		centerPanel.add(colDefaultValue = new JTextField());
		colDefaultValue.addActionListener(this);
		
		
		Dimension maxSize = centerPanel.getLayout().preferredLayoutSize(centerPanel);
		maxSize.width = Integer.MAX_VALUE;
		centerPanel.setMaximumSize(maxSize);
		centerBox.add(centerPanel);
		centerBox.add(Box.createVerticalGlue());
		add(centerBox, BorderLayout.CENTER);
    }

	private JSpinner createScaleEditor() {
		return new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
	} 

	private JSpinner createPrecisionEditor() {
		return createScaleEditor();  // looks better if both spinners are same size
	}

	private JComboBox createColTypeEditor() {
		return new JComboBox(SQLType.getTypes());
	}

	/**
	 * Updates all the UI components to reflect the given column's properties.
     * Also saves a reference to the given column so the changes made in the
     * UI can be written back into the column.
     * 
     * @param col The column to edit
	 */
	public void editColumn(SQLColumn col) throws ArchitectException {
		logger.debug("Edit Column '"+col+"' is being called");
        if (col == null) throw new NullPointerException("Edit null column is not allowed");
        column = col;
		if (col.getSourceColumn() == null) {
			sourceDB.setText("None Specified");
			sourceTableCol.setText("None Specified");
		} else {
			StringBuffer sourceDBSchema = new StringBuffer();
			SQLObject so = col.getSourceColumn().getParentTable().getParent();
			while (so != null) {
				sourceDBSchema.insert(0, so.getName());
				sourceDBSchema.insert(0, ".");
				so = so.getParent();
			}
			sourceDB.setText(sourceDBSchema.toString().substring(1));
			sourceTableCol.setText(col.getSourceColumn().getParentTable().getName()
								   +"."+col.getSourceColumn().getName());
		}
		colName.setText(col.getName());
		colType.setSelectedItem(SQLType.getType(col.getType()));
		colScale.setValue(new Integer(col.getScale()));
		colPrec.setValue(new Integer(col.getPrecision()));
		colNullable.setSelected(col.getNullable() == DatabaseMetaData.columnNullable);
		colRemarks.setText(col.getRemarks());
		colDefaultValue.setText(col.getDefaultValue());
		colInPK.setSelected(col.getPrimaryKeySeq() != null);
		colAutoInc.setSelected(col.isAutoIncrement());
        colAutoIncSequenceName.setText(col.getAutoIncrementSequenceName());
		updateComponents();
        discoverSequenceNamePattern(col.getName());
		colName.requestFocus();
		colName.selectAll();
	}

    /**
     * Figures out what the sequence name prefix and suffix strings are,
     * based on the current contents of the sequence name and column name
     * fields.
     */
    private void discoverSequenceNamePattern(String colName) {
        String seqName = this.colAutoIncSequenceName.getText();
        int prefixEnd = seqName.indexOf(colName);
        if (prefixEnd >= 0 && colName.length() > 0) {
            seqNamePrefix = seqName.substring(0, prefixEnd);
            seqNameSuffix = seqName.substring(prefixEnd + colName.length());
        } else {
            seqNamePrefix = null;
            seqNameSuffix = null;
        }
    }
    
    /**
     * Modifies the contents of the "auto-increment sequence name" field to
     * match the naming scheme as it is currently understood. This modification
     * is only performed if the naming scheme has been successfully determined
     * by the {@link #discoverSequenceNamePattern(String)} method. The new
     * sequence name is written directly to the {@link #colAutoIncSequenceName}
     * field.
     */
    private void syncSequenceName() {
        if (seqNamePrefix != null && seqNameSuffix != null) {
            String newName = seqNamePrefix + colName.getText() + seqNameSuffix;
            colAutoIncSequenceName.setText(newName);
        }
    }
    
	/**
	 * Implementation of ActionListener.
	 */
	public void actionPerformed(ActionEvent e) {
		logger.debug("action event "+e);
		updateComponents();
	}

	/**
	 * Implementation of ChangeListener.
	 */
	public void stateChanged(ChangeEvent e) {
		logger.debug("State change event "+e);
	}
	
	/**
	 * Examines the components and makes sure they're in a consistent
	 * state (they are legal with respect to the model).
	 */
	private void updateComponents() {
		// allow nulls is free unless column is in PK 
		if (colInPK.isSelected()) {
			colNullable.setEnabled(false);
		} else {
			colNullable.setEnabled(true);
		}

		// primary key is free unless column allows nulls
		if (colNullable.isSelected()) {
			colInPK.setEnabled(false);
		} else {
			colInPK.setEnabled(true);
		}

		if (colInPK.isSelected() && colNullable.isSelected()) {
		    //this should not be physically possible
		    colNullable.setSelected(false);
		    colNullable.setEnabled(false);
		}
		
		if (colAutoInc.isSelected()) {
		    colDefaultValue.setText("");
		    colDefaultValue.setEnabled(false);
		} else {
		    colDefaultValue.setEnabled(true);
		}
        
        colAutoIncSequenceName.setEnabled(colAutoInc.isSelected());
	}
	
	/**
	 * Sets the properties of the current column in the model to match
	 * those on screen.
     * 
     * @return A list of error messages if the update was not successful.
	 */
    private List<String> updateModel() {
        logger.debug("Updating model");
        List<String> errors = new ArrayList<String>();
        try {
            column.startCompoundEdit("Column Edit Panel Changes");
            if (colName.getText().trim().length() == 0) {
                errors.add("Column name is required");
            } else {
                column.setName(colName.getText());
            }
            column.setType(((SQLType) colType.getSelectedItem()).getType());
            column.setScale(((Integer) colScale.getValue()).intValue());
            column.setPrecision(((Integer) colPrec.getValue()).intValue());
            column.setNullable(colNullable.isSelected()
                    ? DatabaseMetaData.columnNullable
                            : DatabaseMetaData.columnNoNulls);
            column.setRemarks(colRemarks.getText());
            if (!(column.getDefaultValue() == null && colDefaultValue.getText().equals("")))
            {
                column.setDefaultValue(colDefaultValue.getText());
            }
            // Autoincrement has to go before the primary key or 
            // this column will never allow nulls
            column.setAutoIncrement(colAutoInc.isSelected());
            if (column.getPrimaryKeySeq() == null) {
                column.setPrimaryKeySeq(colInPK.isSelected() ? new Integer(column.getParentTable().getPkSize()) : null);
            } else {
                column.setPrimaryKeySeq(colInPK.isSelected() ? new Integer(column.getPrimaryKeySeq()) : null);
            }
            column.setAutoIncrementSequenceName(colAutoIncSequenceName.getText());
        } finally {
            column.endCompoundEdit("Column Edit Panel Changes");
        }
        return errors;
    }
	
	// ------------------ ARCHITECT PANEL INTERFACE ---------------------
	
	/**
	 * Calls updateModel since the user may have clicked "ok" before
	 * hitting enter on a text field.
	 */
	public boolean applyChanges() {
		List<String> errors = updateModel();
        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this, errors.toString());
            return false;
        } else {
            return true;
        }
	}

	/**
	 * Does nothing.  The column's properties will not have been
     * modified.
	 */
	public void discardChanges() {
	}
	
	public JPanel getPanel() {
		return this;
	}
	
	// THESE GETTERS ARE TO BE USED FOR TESTING ONLY
	public JCheckBox getColAutoInc() {
		return colAutoInc;
	}

	public JTextField getColDefaultValue() {
		return colDefaultValue;
	}

	public JCheckBox getColInPK() {
		return colInPK;
	}

	public JTextField getColName() {
		return colName;
	}

	public JCheckBox getColNullable() {
		return colNullable;
	}

	public JSpinner getColPrec() {
		return colPrec;
	}

	public JTextField getColRemarks() {
		return colRemarks;
	}

	public JSpinner getColScale() {
		return colScale;
	}

	public JComboBox getColType() {
		return colType;
	}

	public JLabel getSourceDB() {
		return sourceDB;
	}

	public JLabel getSourceTableCol() {
		return sourceTableCol;
	}

    public boolean hasUnsavedChanges() {
        // TODO return whether this panel has been changed
        return true;
    }
}
  