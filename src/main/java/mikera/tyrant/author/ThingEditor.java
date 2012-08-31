package mikera.tyrant.author;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.MenuBar;
import java.awt.Panel;
import java.awt.SystemColor;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import mikera.tyrant.Game;
import mikera.tyrant.QuestApp;
import mikera.tyrant.TPanel;
import mikera.tyrant.engine.Thing;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class ThingEditor implements Runnable {
    private final class SourceRevertPressed implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            sourceTextArea.setText(mapText);
        }
    }

    private final class SourceOKPressed implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String newText = sourceTextArea.getText();
            if(newText.equals(mapText)) return;
            mikera.tyrant.engine.Map newMap = new MapMaker().create(newText, true);
            if(newMap == null) {
                sourceStatusBar.setVisible(true);
                sourceStatusLabel.setText("There is an error creating the map");
                sourceStatusBar.getParent().invalidate();
                sourceStatusBar.getParent().validate();
                new Thread(ThingEditor.this).start();
                return;
            }
            if(designer != null) {
                designer.reloadMap(newMap);
                mapText = new MapMaker().store(newMap);
                sourceTextArea.setText(mapText);
            }
        }
    }

    private Frame frame;
    private TPanel statusBar;
    private TextField textField;
    private Label textLabel;
    private ActionListener textListener;
    private Container thingPanel;
    private Thing thing;
    private Map importantAttributes;
    private Map textFieldMapping = new HashMap();
    private Map keyTypes = new HashMap();
    private String mapText;
    private TextArea sourceTextArea;
    private Panel sourceStatusBar;
    private Label sourceStatusLabel;
    private Designer designer;
    
    public void inspect(Thing thing) {
        this.thing = thing;
        updateThingPanel();
    }

    public void inspect(Thing[] things) {
        //TODO Support multiple things
        inspect(things == null || things.length == 0? null : things[0]);
    }
    
    public void buildUI() {
        Game.loadVersionNumber();
        createFrame();
        statusBar.setVisible(false);
    }

    public void updateThingPanel() {
        if(thingPanel != null && thingPanel.isValid())
            frame.remove(thingPanel);
        createThingPanel();
        thingPanel.getParent().invalidate();
        thingPanel.getParent().validate();
    }
    

    private void createThingPanel() {
        thingPanel = new JTabbedPane(JTabbedPane.TOP);
        frame.add(thingPanel, BorderLayout.CENTER);
        createThingTabbs();
        thingPanel.setBackground(SystemColor.control);
        ((JTabbedPane) thingPanel).addTab("Source", createSourcePanel());
    }

    private void createThingTabbs() {
        if(thing == null) return;
        Thing flat = thing.getFlattened();
        Map locals = flat.getLocal();
        String[] keys = (String[]) locals.keySet().toArray(new String[locals.keySet().size()]);
        if (keys.length == 0) return;
        sortKeys(keys);
        int attriubutesPerPage = 25;
        int tabsNeed = Math.max(1, (int) Math.ceil(locals.size() / attriubutesPerPage));
        char firstChar = 'A';
        for (int i = 0; i < tabsNeed; i++) {
            Panel panel = new Panel(new BorderLayout());
            panel.setBackground(SystemColor.control);
            char next = tabsNeed == 1 ? 'Z' : keys[(i + 1) * attriubutesPerPage].charAt(0);
            String tabName = "" + firstChar + "-" + next;
            firstChar = (char) (next + 1);
            ((JTabbedPane) thingPanel).addTab(tabName, panel);
            // ((JTabbedPane)thingPanel).addTab("" + (i + 1) + "/" + tabsNeed,
            // panel);
            addAttributes2(locals, keys, panel, i * attriubutesPerPage, attriubutesPerPage);
        }
    }

    private Component createSourcePanel() {
        FormLayout layout = new FormLayout("fill:pref:grow", "fill:pref:grow");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        mapText = new MapMaker().store(designer.map);
        
        sourceTextArea = new TextArea(mapText, 10, 20);
        builder.append(sourceTextArea);
        builder.append(createSourceButtonPanel());
        builder.append(createSourceStatus());
        
        Panel container = new Panel(new BorderLayout());
        container.add(builder.getPanel(), BorderLayout.CENTER);
        
        return container;
    }

    private Panel createSourceStatus() {
        sourceStatusBar = new Panel(new BorderLayout());
        sourceStatusLabel = new Label("Bob", Label.CENTER);
        sourceStatusBar.add(sourceStatusLabel, BorderLayout.CENTER);
        sourceStatusBar.setVisible(false);
        return sourceStatusBar;
    }

    private Panel createSourceButtonPanel() {
        Panel buttonPanel = new Panel(new FlowLayout());
        Button okButton = new Button("Apply");
        Button cancelButton = new Button("Revert");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        okButton.addActionListener(new SourceOKPressed());
        cancelButton.addActionListener(new SourceRevertPressed());
        return buttonPanel;
    }

    private String determineType(String key, Object value) {
        String type = null;
        if(value instanceof Integer) {
            type = key.startsWith("Is") ? "boolean" : "int";
        }
        else if(value instanceof Double)
            type = "double";
        else if(value instanceof String)
            type = "string";
        keyTypes.put(key, type);
        return type;
    }

    private void sortKeys(String[] keys) {
        Arrays.sort(keys, new Comparator() {
            public boolean equals(Object obj) {
                return false;
            }

            public int compare(Object o1, Object o2) {
                String s1 = (String)o1;
                String s2 = (String)o2;
                Map important = getImportantAttributes();
                Integer sortOrder1 = (Integer) important.get(s1);
                Integer sortOrder2 = (Integer) important.get(s2);
                if(sortOrder1 == null && sortOrder2 == null) return s1.compareTo(s2);
                if(sortOrder1 == null && sortOrder2 != null) return 1;
                if(sortOrder1 != null && sortOrder2 == null) return -1;
                return sortOrder1.intValue() - sortOrder2.intValue();
            }
        });
    }

    protected Map getImportantAttributes() {
        if(importantAttributes == null) {
            importantAttributes = new HashMap();
            String[] names = {"Name", "Number", "Level", "Frequency", "HPS", "UName"};
            for (int i = 0; i < names.length; i++) {
                importantAttributes.put(names[i], new Integer(i));
            }
        }
        return importantAttributes;
    }

    private void createFrame() {
        frame = new Frame("ThingEditor - v" + Game.VERSION);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.setVisible(false);
            }
        });
        frame.setBackground(SystemColor.control);
        frame.setLayout(new BorderLayout());
        statusBar = new TPanel(null);
        frame.add(statusBar, BorderLayout.SOUTH);
        textField = new TextField(30);
        statusBar.setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        textLabel = new Label("Bob:");
        textLabel.setForeground(QuestApp.INFOTEXTCOLOUR);
        statusBar.add(textLabel, gridBagConstraints);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        statusBar.add(textField, gridBagConstraints);
        textListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doTextPerformed(e);
            }
        };
        frame.setSize(640, 800);
        frame.setMenuBar(createMenuBar());
    }

    protected void doTextPerformed(ActionEvent e) {
        String key = (String) textFieldMapping.get(e.getSource());
        String value = ((TextField)e.getSource()).getText();
        String type = (String) keyTypes.get(key);
        Object thingValue = value;
        if (!type.equals("string")) {
            if (type.equals("int")) thingValue = Integer.valueOf(value);
            else if (type.equals("double")) thingValue = Double.valueOf(value);
            else if (type.equals("boolean")) thingValue = Boolean.valueOf(value);
        }
        thing.set(key, thingValue);
        updateSourceTab();
    }

    private void updateSourceTab() {
        mapText = new MapMaker().store(designer.map);
        sourceTextArea.setText(mapText);
    }

    protected void doCheckboxPerformed(ItemEvent e) {
        Checkbox checkbox = (Checkbox)e.getSource();
        String key = checkbox.getLabel();
        Integer value = new Integer(checkbox.getState() ? 1 : 0);
        thing.set(key, value);
    }
    
    private MenuBar createMenuBar() {
        return null;
    }

    public void setVisible(boolean show) {
        frame.setVisible(show);
    }
    
    public boolean isVisible() {
        return frame.isVisible();
    }
    public Frame getFrame() {
        return frame;
    }

    private void addAttributes2(Map locals, String[] keys, Container container, int start, int toAdd) {
        FormLayout layout = new FormLayout("pref, 3dlu, fill:pref:grow(100)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        toAdd = Math.min(keys.length, toAdd);
        for (int i = start; i < start + toAdd; i++) {
            String key = keys[i];
            if(locals.get(key) == null) continue;
            Object value = locals.get(key);
            String type = determineType(key, value);
            if(type == null) {
                System.out.println("ignoring " + key + " " + value);
                continue;
            }
            if(type.equals("boolean")) {
                Checkbox checkbox = new Checkbox(key, ((Integer)value).intValue() == 1);
                checkbox.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        doCheckboxPerformed(e);
                    }
                });
                builder.append("");
                builder.append(checkbox);
                builder.nextLine();
                continue;
            }
            builder.append(key);
            
            if(key.equals("Message")) {
                TextArea textArea = new TextArea(String.valueOf(value), 1, 30, TextArea.SCROLLBARS_VERTICAL_ONLY);
                builder.append(textArea);
                textFieldMapping.put(textArea, key);
            } else {
                TextField textField = new TextField(String.valueOf(value));
                builder.append(textField);
                textFieldMapping.put(textField, key);
                textField.addActionListener(textListener);
                if (key.equals("Name")) {
                    textField.setEditable(false);
                }
            }
            builder.nextLine();
        }
        container.add(builder.getPanel(), BorderLayout.CENTER);
        
    }

    public void run() {
        try {
            Thread.sleep(6000);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    sourceStatusBar.setVisible(false);
                    sourceStatusLabel.setText("There is an error creating the map");
                    sourceStatusBar.getParent().invalidate();
                    sourceStatusBar.getParent().validate();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public void setDesigner(Designer designer) {
        this.designer = designer;
    }
}
