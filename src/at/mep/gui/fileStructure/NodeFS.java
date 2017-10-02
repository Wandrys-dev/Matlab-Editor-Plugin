package at.mep.gui.fileStructure;

import at.mep.editor.tree.MFile;
import at.mep.meta.*;
import at.mep.util.NodeUtils;
import com.mathworks.matlab.api.editor.Editor;
import com.mathworks.widgets.text.mcode.MTree;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

/**
 * Created by z0032f1t on 02.08.2016.
 * <p>
 * Represents a NodeFS in JTree and also an Matlab MTree.NodeFS class.
 */
public class NodeFS extends DefaultMutableTreeNode {
    private MTree.Node node; // might not always be set, e.g.: First node is just the string of the filename
    private String nodeText = "DEFAULT NODE TEXT";
    private MTree.NodeType nodeType = MTree.NodeType.JAVA_NULL_NODE;

    // custom NodeFS properties for metaClass
    private boolean isStatic = false;  // same as constant
    private boolean isSealed = false;
    private boolean isAbstract = false;
    private boolean isConstructOnlOad = false;
    private boolean isHandleCompatible = false;
    private boolean isEnumeration = false;
    private boolean isDependent = false;
    private boolean isTransient = false;
    private boolean isImmutable = false;
    private EAccess Access = EAccess.PUBLIC;
    private boolean isHidden = false;
    private EAccess GetAccessPrivate = EAccess.PUBLIC;
    private EAccess SetAccessPrivate = EAccess.PUBLIC;
    private boolean hasDefaults = false;
    private String documentation = "";
    private String detailedDocumentation = "";

    private NodeFS() {

    }

    public NodeFS(NodeFS nodeFS) {
        node = nodeFS.node;
        nodeText = nodeFS.nodeText;
        nodeType = nodeFS.nodeType;

        isStatic = nodeFS.isStatic;
        isSealed = nodeFS.isSealed;
        isAbstract = nodeFS.isAbstract;
        isConstructOnlOad = nodeFS.isConstructOnlOad;
        isHandleCompatible = nodeFS.isHandleCompatible;
        isEnumeration = nodeFS.isEnumeration;
        isDependent = nodeFS.isDependent;
        isTransient = nodeFS.isTransient;
        isImmutable = nodeFS.isImmutable;
        Access = nodeFS.Access;
        isHidden = nodeFS.isHidden;
        GetAccessPrivate = nodeFS.GetAccessPrivate;
        SetAccessPrivate = nodeFS.SetAccessPrivate;
        hasDefaults = nodeFS.hasDefaults;
        documentation = nodeFS.documentation;
        detailedDocumentation = nodeFS.detailedDocumentation;
    }

    public NodeFS(String nodeText) {
        this.nodeText = nodeText;
        this.nodeType = MTree.NodeType.JAVA_NULL_NODE;
    }

    public MTree.Node node() {
        return node;
    }

    public boolean hasNode() {
        return node != null;
    }

    public String nodeText() {
        return nodeText;
    }


    public MTree.NodeType getType() {
        return nodeType;
    }

    public String getDocumentation() {
        if (documentation == null || documentation.length() < 1) {
           documentation = nodeDocumentation();
        }
        return documentation;
    }

    public String getDetailedDocumentation() {
        return detailedDocumentation;
    }

    public boolean isProperty() {
        return nodeType == MTree.NodeType.EQUALS;
    }

    public boolean isStatic() {
        // (eMetaNodeType == eMetaNodeType.META_PROPERTY || eMetaNodeType == eMetaNodeType.META_METHOD) &&
        // isStatic is only set in Meta<Class,Property,Method> constructors, otherwise it's always false
        // so it is safe to just return isStatic
        return isStatic;
    }

    public EAccess getAccess() {
        return Access;
    }

    public boolean isHidden() {
        return isHidden;
    }

    private String nodeDocumentation() {
        if (!(getType() == MTree.NodeType.FUNCTION || getType() == MTree.NodeType.CLASSDEF)) {
            return "";
        }
        List<MTree.Node> nodeList = NodeUtils.getDocumentationNodesForNode(node);
        String s = "";
        for (MTree.Node node : nodeList) {
            s += NodeUtils.getTextForNode(node).trim() + "\n";
        }
        if (s.length() > 0) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static NodeFS constructForCellTitle(Editor editor) {
        MFile mFile = MFile.construct(editor);
        NodeFS root = new NodeFS(mFile.getName());
        for (MFile.CellTitle cellTitle : mFile.getCellTitles()) {
            NodeFS nodeFS = new NodeFS();
            nodeFS.node = cellTitle.getNode();
            nodeFS.nodeText = cellTitle.getTitleString();
            nodeFS.nodeType = MTree.NodeType.CELL_TITLE;

            root.add(nodeFS);
        }
        return root;
    }

    public static NodeFS constructForFunctions(Editor editor) {
        MFile mFile = MFile.construct(editor);
        NodeFS root = new NodeFS(mFile.getName());
        for (MFile.ClassDef.Method.Function function : mFile.getFunctions()) {
            NodeFS nodeFS = new NodeFS();
            nodeFS.node = function.getNode();
            nodeFS.nodeText = function.getFunctionString();
            nodeFS.nodeType = MTree.NodeType.FUNCTION;

            root.add(nodeFS);

        }
        return root;
    }

    public static NodeFS constructForClassDef(Editor editor) {
        MFile mFile = MFile.construct(editor);
        NodeFS root = new NodeFS(mFile.getName());
        MFile.ClassDef classDef = mFile.getClassDefs().get(0);
        root.node = classDef.getNode();
        root.nodeType = MTree.NodeType.CLASSDEF;

        // properties
        for (MFile.ClassDef.Properties properties : classDef.getProperties()) {
            List<MFile.Attributes.Attribute> attributeList = null;
            if (properties.hasAttributes()) {
                attributeList = properties.getAttributes().get(0).getAttributeList();
            }
            for (MFile.ClassDef.Properties.Property property : properties.getPropertyList()) {
                NodeFS nodeFS = new NodeFS();

                nodeFS.node = property.getNode();
                nodeFS.nodeText = property.getNode().getText();
                nodeFS.nodeType = MTree.NodeType.EQUALS;

                if (property.hasGetter()) {
                    NodeFS getter = new NodeFS();
                    getter.node = property.getGetter().getNode();
                    getter.nodeText = property.getGetter().getNode().getText();
                    getter.nodeType = MTree.NodeType.FUNCTION;

                    nodeFS.add(getter);
                }

                if (property.hasSetter()) {
                    NodeFS setter = new NodeFS();
                    setter.node = property.getSetter().getNode();
                    setter.nodeText = property.getSetter().getNode().getText();
                    setter.nodeType = MTree.NodeType.FUNCTION;

                    nodeFS.add(setter);
                }

                root.add(nodeFS);
            }
        }
        
        // functions
        for (MFile.ClassDef.Method method : classDef.getMethod()) {
            List<MFile.Attributes.Attribute> attributeList = null;
            if (method.hasAttributes()) {
                attributeList = method.getAttributes().get(0).getAttributeList();
            }
            for (MFile.ClassDef.Method.Function function : method.getFunctionList()) {
                if (function.isGetter() || function.isSetter()) {
                    continue;
                }
                NodeFS nodeFS = new NodeFS();
                nodeFS.node = function.getNode();
                nodeFS.nodeText = function.getFunctionString();
                nodeFS.nodeType = MTree.NodeType.FUNCTION;
                root.add(nodeFS);
            }
        }
        return root;
    }
}
