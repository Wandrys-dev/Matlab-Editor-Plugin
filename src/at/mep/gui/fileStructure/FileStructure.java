package at.mep.gui.fileStructure;


import at.mep.editor.EditorWrapper;
import at.mep.gui.components.JTextFieldSearch;
import at.mep.gui.components.UndecoratedFrame;
import at.mep.prefs.Settings;
import at.mep.util.KeyStrokeUtil;
import at.mep.util.RunnableUtil;
import at.mep.util.ScreenSize;
import com.mathworks.matlab.api.editor.Editor;
import com.mathworks.util.tree.Tree;
import com.mathworks.widgets.text.mcode.MTree;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Created by Andreas Justin on 2016 - 02 - 24. */
public class FileStructure extends UndecoratedFrame {
    private static final int IFW = JComponent.WHEN_IN_FOCUSED_WINDOW;
    private static FileStructure INSTANCE;
    private static Editor activeEditor;
    private static JTextFieldSearch jTFS;
    private static JTextArea jTextArea;
    private static JScrollPane docuScrollPane;
    private static JRadioButton functions = new JRadioButton("Functions", true);
    private static JRadioButton sections = new JRadioButton("Sections", false);
    private static JRadioButton classes = new JRadioButton("Class", false);
    private static JCheckBox regex = new JCheckBox("<html>regex <font color=#8F8F8F>(CTRL + R)</font></html>");
    private static JCheckBox inherited = new JCheckBox("<html>inherited <font color=#8F8F8F>(CTRL + F12)</font></html>");
    private JTreeFilter jTree;
    private AbstractAction enterAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
            if (jTree.getMaxSelectionRow() < 0) return;
            Node node = (Node) jTree.getSelectionPath().getLastPathComponent();
            if (node.hasNode()) {
                EditorWrapper.goToLine(node.node().getStartLine(), false);
            }
        }
    };

    private AbstractAction updateAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            findPattern(jTFS.getText());
            if (jTree.getRowCount() > 1) {
                jTree.setSelectionRow(1);
            }
        }
    };

    public FileStructure() {
        Runnable runnable = new Runnable() {
            public void run() {
                setLayout();
            }
        };
        RunnableUtil.invokeInDispatchThreadIfNeeded(runnable);
    }

    public static FileStructure getInstance() {
        if (INSTANCE != null) return INSTANCE;
        INSTANCE = new FileStructure();
        return INSTANCE;
    }

    private void setLayout() {
        setTitle("FileStructureViewer");
        int width = ScreenSize.getWidth();
        int height = ScreenSize.getHeight();

        setSize(500, 600);
        setLocation(width / 2 - getWidth() / 2, height / 2 - getHeight() / 2);

        getRootPane().setLayout(new GridBagLayout());

        // creating search box
        createSearchField();
        GridBagConstraints cjtfs = new GridBagConstraints();
        cjtfs.gridy = 0;
        cjtfs.gridx = 0;
        cjtfs.weightx = 1;
        cjtfs.fill = GridBagConstraints.BOTH;
        getRootPane().add(jTFS, cjtfs);

        //creating radio buttons for selecting category
        JPanel panelSettings = createSettingsPanel();
        GridBagConstraints cSet = new GridBagConstraints();
        cSet.gridy = 1;
        cSet.gridx = 0;
        cSet.weightx = 1;
        cSet.fill = GridBagConstraints.BOTH;
        getRootPane().add(panelSettings, cSet);

        //create the jTree by passing in the root node
        JScrollPane scrollPaneTree = createTree();
        GridBagConstraints cSP = new GridBagConstraints();
        cSP.gridy = 2;
        cSP.gridx = 0;
        cSP.weighty = 1;
        cSP.weightx = cSet.weightx;
        cSP.fill = GridBagConstraints.BOTH;
        getRootPane().add(scrollPaneTree, cSP);

        //create the documentation viewer
        jTextArea = new JTextArea();
        jTextArea.setForeground(new Color(11, 134, 0));
        docuScrollPane = new JScrollPane(jTextArea);
        GridBagConstraints cDSP = new GridBagConstraints();
        cDSP.gridy = 3;
        cDSP.gridx = 0;
        cDSP.weighty = 0.3;
        cDSP.weightx = cSet.weightx;
        cDSP.fill = GridBagConstraints.BOTH;
        cDSP.insets = new Insets(5, 0, 0, 0);
        getRootPane().add(docuScrollPane, cDSP);
    }

    private void createSearchField() {
        jTFS = new JTextFieldSearch(30);
        jTFS.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateAction.actionPerformed(null);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateAction.actionPerformed(null);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        KeyStroke ksU = KeyStrokeUtil.getKeyStroke(KeyEvent.VK_UP);
        jTFS.getInputMap(JComponent.WHEN_FOCUSED).put(ksU, "UP");
        jTFS.getActionMap().put("UP", new AbstractAction("UP") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = jTree.getMaxSelectionRow(); // single selection
                if (row < 0) {
                    jTree.setSelectionRow(0);
                }
                jTree.setSelectionRow(row - 1); // zero is top, so up means -1
                jTree.scrollRowToVisible(jTree.getMaxSelectionRow());
            }
        });

        KeyStroke ksD = KeyStrokeUtil.getKeyStroke(KeyEvent.VK_DOWN);
        jTFS.getInputMap(JComponent.WHEN_FOCUSED).put(ksD, "DOWN");
        jTFS.getActionMap().put("DOWN", new AbstractAction("DOWN") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = jTree.getMaxSelectionRow(); // single selection
                if (row < 0) {
                    jTree.setSelectionRow(0);
                }
                if (jTree.getRowCount() - 1 > row) {
                    jTree.setSelectionRow(row + 1); // zero is top, so down means +1
                }
                jTree.scrollRowToVisible(jTree.getMaxSelectionRow());
            }
        });
    }

    private JPanel createSettingsPanel() {
        ButtonGroup bg = new ButtonGroup();
        bg.add(sections);
        bg.add(functions);
        bg.add(classes);
        final JPanel panelSettings = new JPanel();
        panelSettings.setBorder(BorderFactory.createTitledBorder("Type"));
        panelSettings.setLayout(new FlowLayout());
        panelSettings.add(sections);
        panelSettings.add(functions);
        panelSettings.add(classes);
        panelSettings.add(inherited);
        panelSettings.add(regex);

        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                populate();
            }
        };
        sections.addActionListener(actionListener);
        functions.addActionListener(actionListener);
        classes.addActionListener(actionListener);

        KeyStroke ksR = KeyStroke.getKeyStroke("control released R");
        getRootPane().getInputMap(IFW).put(ksR, "CTRL + R");
        getRootPane().getActionMap().put("CTRL + R", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                regex.setSelected(!regex.isSelected());
            }
        });

        KeyStroke ksF12 = KeyStroke.getKeyStroke("control released F12");
        getRootPane().getInputMap(IFW).put(ksF12, "CTRL + F12");
        getRootPane().getActionMap().put("CTRL + F12", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!inherited.isEnabled()) return;
                inherited.setSelected(!inherited.isSelected());
                populate();
            }
        });

        return panelSettings;
    }

    private JScrollPane createTree() {
        jTree = new JTreeFilter();
        jTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        JScrollPane scrollPaneTree = new JScrollPane(jTree);
        scrollPaneTree.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPaneTree.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        jTree.addMouseListener(mlClick);
        jTree.addMouseMotionListener(mlMove);
        jTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if (e.getClickCount() > 1) {
                    enterAction.actionPerformed(new ActionEvent(e, 0, null));
                }
            }
        });
        jTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                jTFS.requestFocus();
                if (jTree.getMaxSelectionRow() < 0) return;
                Node node = (Node) jTree.getSelectionPath().getLastPathComponent();
                jTextArea.setText(node.getDocumentation());
                moveBarsDocuScrollpane();
            }
        });

        KeyStroke ksE = KeyStrokeUtil.getKeyStroke(KeyEvent.VK_ENTER);
        jTree.getInputMap(IFW).put(ksE, "ENTER");
        jTree.getActionMap().put("ENTER", new AbstractAction("ENTER") {
            @Override
            public void actionPerformed(ActionEvent e) {
                enterAction.actionPerformed(new ActionEvent(e, 0, null));
            }
        });

        return scrollPaneTree;
    }

    private void findPattern(String pattern) {
        if (regex.isSelected()) {
            try {
                Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                jTFS.setForeground(null);
                setTreeRoot(jTree.filter(p), true);
            } catch (PatternSyntaxException e) {
                jTFS.setForeground(Color.RED);
            }
        } else {
            setTreeRoot(jTree.filter(pattern), true);
        }
    }

    /** for radio buttons */
    private void populate() {
        Node root = new Node(EditorWrapper.getShortName());
        
        MTree.NodeType nodeType;
        if (sections.isSelected()) {
            nodeType = MTree.NodeType.CELL_TITLE;
            root = EditorWrapper.getSectionNodeFast(activeEditor);
        } else if (functions.isSelected()) {
            nodeType = MTree.NodeType.FUNCTION;
            root = EditorWrapper.getFunctionNodeFast(activeEditor);
        } else if (classes.isSelected()) {
            nodeType = MTree.NodeType.CLASSDEF;
            root = EditorWrapper.getClassNodeFast(activeEditor);
        } else {
            nodeType = MTree.NodeType.CELL_TITLE;
            sections.setSelected(true);
        }

        // inherited.setEnabled(classes.isSelected());
        inherited.setEnabled(false);

        MTree mTree = EditorWrapper.getMTreeFast(activeEditor);
        Tree<MTree.Node> nodeTree = mTree.findAsTree(nodeType);
        if (nodeType.equals(MTree.NodeType.CLASSDEF) & nodeTree.getChildCount(nodeTree.getRoot()) < 1) {
            // class was selected, but file is no class
            root = EditorWrapper.getFunctionNodeFast(activeEditor);
        }
        setTreeRoot(root, false);
    }

    public void populateTree() {
        if (activeEditor != EditorWrapper.getActiveEditor()) {
            jTFS.setText(""); // resetting search if activeEditor has been changed
            activeEditor = EditorWrapper.getActiveEditor();
            setDefaultSettings();
        }
        populate();
    }

    public void setDefaultSettings() {
        // preferred classes, if only functions or sections are available, radioButtons will be set accordingly
        classes.setSelected(classes.isEnabled());
        functions.setSelected(!classes.isEnabled() & !sections.isEnabled() & functions.isEnabled());
        sections.setSelected(!classes.isEnabled() & sections.isEnabled() & !functions.isEnabled());

        // (disable/enable) class RadioButton if the current file (is no/is) class
        MTree mTree = EditorWrapper.getMTree();
        Tree<MTree.Node> nodeTree = mTree.findAsTree(MTree.NodeType.CLASSDEF);
        classes.setEnabled(
                (nodeTree.getChildCount(nodeTree.getRoot()) > 0) // is a valid CLASSDEF
                && EditorWrapper.getFile().exists() // Untitled may hold classdef
        );

        nodeTree = EditorWrapper.getTreeFunction(activeEditor);
        functions.setEnabled((nodeTree.getChildCount(nodeTree.getRoot()) > 0));

        nodeTree = EditorWrapper.getTreeSection(activeEditor);
        sections.setEnabled((nodeTree.getChildCount(nodeTree.getRoot()) > 0));

        // preferred classes, if only functions or cells are available, radioButtons will be set accordingly
        classes.setSelected(classes.isEnabled()); // classes preferred
        functions.setSelected(!classes.isEnabled() & functions.isEnabled()); // functions before sections preferred
        sections.setSelected(!classes.isEnabled() & sections.isEnabled() & !functions.isEnabled());

        // search field request focus after resetting Settings
        jTFS.requestFocus();
    }

    private void setTreeRoot(Node root, boolean filtered) {
        if (!filtered) {
            jTree.setOriginalRoot(root);
        }
        jTree.setModel(new DefaultTreeModel(root));
        jTree.setCellRenderer(new TreeRenderer());
        expandAll();
    }

    public void showDialog() {
        setVisible(true);
        // ISSUE: #36
        // findPattern(jTFS.getText()); // show last search (if activeEditor has not been changed @populate)
        jTFS.setText("");
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            jTextArea.setFont(new Font("Courier New", Font.PLAIN, Settings.getPropertyInt("fs.fontSizeDocu")));
            moveBarsDocuScrollpane();
        }
    }

    private void moveBarsDocuScrollpane() {
        docuScrollPane.getHorizontalScrollBar().setValue(docuScrollPane.getHorizontalScrollBar().getMinimum());
        docuScrollPane.getVerticalScrollBar().setValue(docuScrollPane.getVerticalScrollBar().getMinimum());
    }

    public void expandAll() {
        for (int i = 0; i < jTree.getRowCount(); i++) {
            jTree.expandRow(i);
        }
    }

    public void collapseAll() {
        for (int i = 0; i < jTree.getRowCount(); i++) {
            jTree.collapseRow(i);
        }
    }
}

/*
// ///////////////////////////
// /////// UNUSED CODE ///////
// ///////////////////////////

private Node forClass(MTree mTree, MTree.Node classDef) {
    Node classDefNode = new Node(classDef);

    // TODO: properties
    // current problem is identifying properties in a way that always works.
    // properties have "ID" as type, but so does MetaProperty Attributes like
    // <Constant, GetAccess..., private..., true ...>
    // see "doc property attributes"
    //
    //    0 = {MTree$Node@13044} "PROPERTIES [5, 5] to [9, 7]"
    //    1 = {MTree$Node@13058} "ATTRIBUTES [5, 16] to [6, 31]"
    //    2 = {MTree$Node@13059} "ATTR [5, 26] to [0, 0]"
    //    3 = {MTree$Node@13046} "ID (Constant) [5, 17] to [0, 0]"
    //    4 = {MTree$Node@13060} "ID (true) [5, 28] to [0, 0]"
    //    5 = {MTree$Node@13061} "ATTR [6, 23] to [0, 0]"
    //    6 = {MTree$Node@13062} "ID (GetAccess) [6, 13] to [0, 0]"
    //    7 = {MTree$Node@13063} "ID (public) [6, 25] to [0, 0]"
    //    8 = {MTree$Node@13064} "EQUALS [7, 15] to [0, 0]"
    //    9 = {MTree$Node@13065} "ID (prop1) [7, 9] to [0, 0]"
    //
// Tree<MTree.Node> propertyTree = mTree.findAsTree(MTree.EMetaNodeType.PROPERTIES);
// classDefNode = fillClassNode(classDefNode, propertyTree, MTree.EMetaNodeType.ID);

Tree<MTree.Node> methodsTree = mTree.findAsTree(MTree.NodeType.METHODS);
    classDefNode = fillClassNode(classDefNode, methodsTree, MTree.NodeType.FUNCTION);
            return classDefNode;
            }

private Node forClassGetTree(MTree mTree, MTree.Node classDef) {
    Node classDefNode = new Node(classDef);

    Tree<MTree.Node> propertyTree = mTree.findAsTree(MTree.NodeType.PROPERTIES);
    Tree<MTree.Node> methodsTree = mTree.findAsTree(MTree.NodeType.METHODS);
    for (int i = 0; i < methodsTree.getChildCount(methodsTree.getRoot()); i++) {
    MTree.Node method = methodsTree.getChild(methodsTree.getRoot(), i);
    Node methodNode = new Node(method);

    List<MTree.Node> methodsSub = method.getSubtree();
    for (MTree.Node methodSub : methodsSub) {
    if (methodSub.getType() == MTree.NodeType.FUNCTION) {
    methodNode.add(new Node(methodSub));
    }
    }
    classDefNode.add(methodNode);
    }
    return classDefNode;
    }

private static Node fillClassNode(Node classDefNode, Tree<MTree.Node> tree, MTree.NodeType type) {
for (int i = 0; i < tree.getChildCount(tree.getRoot()); i++) {
MTree.Node typeNodes = tree.getChild(tree.getRoot(), i);
List<MTree.Node> subs = typeNodes.getSubtree();
for (MTree.Node sub : subs) {
    if (sub.getType() == type) {
        if (type == MTree.NodeType.ID
                && (sub.getText().matches("(Constant|Static|true|false|GetAccess|SetAccess|Access|private|public)"))) {
            continue;
        }
        classDefNode.add(new Node(sub));
    }
}
}
return classDefNode;
}
 */
