package ca.sqlpower.architect.swingui;

import java.awt.event.*;
import javax.swing.*;
import org.apache.log4j.Logger;

public class SearchReplaceAction extends AbstractAction {
    private static final Logger logger = Logger.getLogger(SearchReplaceAction.class);
    
    /**
     * The PlayPen instance that owns this Action.
     */
    protected PlayPen pp;
    
    /**
     * The DBTree instance that is associated with this Action.
     */
    protected DBTree dbt;
    
    public SearchReplaceAction() {
        super("Find/Replace...",
                ASUtils.createJLFIcon("general/Find",
                        "Find/Replace",
                        ArchitectFrame.getMainInstance().sprefs.getInt(SwingUserSettings.ICON_SIZE, 24)));
        putValue(SHORT_DESCRIPTION, "Find/Replace");
    }
    
    public void actionPerformed(ActionEvent evt) {
        SearchReplace sr = new SearchReplace();
        sr.showSearchDialog(pp);
    }
    
    public void setPlayPen(PlayPen playpen) {
        pp = playpen;
    }
    
    public void setDBTree(DBTree dbTree) {
        dbt = dbTree;
    }
    
}
