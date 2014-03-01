package com.web4thejob.designer;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author Veniamin Isaias
 * @since 1.0.0
 */
public class PropertyEditorController extends SelectorComposer<Component> {
    private final SaveProperties SAVE_CHANGES_HANDLER = new SaveProperties();

    @Wire
    private Tabpanel properties;

    private Component comp;


    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        EventQueues.lookup("myMessageQueue", EventQueues.SESSION, true).subscribe(
                new EventListener<Event>() {
                    public void onEvent(Event event) throws ClassNotFoundException, NoSuchMethodException,
                            InvocationTargetException, IllegalAccessException, IOException {
                        PropertyEditorController.this.comp = (Component) event.getData();
                        renderProperties();
                    }
                });

    }

    private void renderProperties() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, IOException {
        properties.getChildren().clear();

        Map<String, Map<String, Map<String, String>>> propsMap = getDefinition(comp);
        for (String group : propsMap.keySet()) {
            Groupbox groupbox = new Groupbox();
            groupbox.setParent(properties);
            groupbox.setMold("3d");
            Caption caption = new Caption(group);
            caption.setParent(groupbox);
            Grid grid = new Grid();
            grid.setParent(groupbox);
            new Columns().setParent(grid);
            new Column().setParent(grid.getColumns());
            ((Column) grid.getColumns().getFirstChild()).setWidth("100px");
            new Column().setParent(grid.getColumns());
            new Rows().setParent(grid);

            List<String> sortedPropNames = new ArrayList<>(propsMap.get(group).keySet());
            Collections.sort(sortedPropNames);
            for (String propName : sortedPropNames) {
                Row row = new Row();
                row.setParent(grid.getRows());

                Label name = new Label(propName);
                name.setParent(row);

                String editor = propsMap.get(group).get(propName).get("editor");
                String type = propsMap.get(group).get(propName).get("type");
                Object val = DesignerUtils.invokeGetter(comp, propName, "boolean".equals(type));
                Component value = createEditor(editor, val);

                value.setAttribute("property", propName);
                value.setParent(row);

            }

        }


    }

    private Component createEditor(String editor, Object value) {
        String name, params;
        if (editor.contains("|")) {
            name = editor.split("\\|")[0];
            params = editor.split("\\|")[1];
        } else {
            name = editor;
            params = null;
        }

        switch (name) {
            case "textbox":
                Textbox textbox = new Textbox();
                textbox.setValue(value != null ? value.toString() : "");
                textbox.setHflex("true");
                textbox.addEventListener(Events.ON_CHANGE, SAVE_CHANGES_HANDLER);
                return textbox;
            case "combobox":
                Combobox combobox = new Combobox();
                if (params != null) {
                    for (String p : StringUtils.commaDelimitedListToSet(params)) {
                        Comboitem item = new Comboitem(p);
                        item.setParent(combobox);
                    }
                }
                combobox.setValue(value != null ? value.toString() : "");
                combobox.setHflex("true");
                combobox.addEventListener(Events.ON_CHANGE, SAVE_CHANGES_HANDLER);
                return combobox;
            case "checkbox":
                Checkbox checkbox = new Checkbox();
                if (value != null) {
                    checkbox.setChecked((Boolean) value);
                }
                checkbox.addEventListener(Events.ON_CHECK, SAVE_CHANGES_HANDLER);
                return checkbox;
            case "intbox":

                Intbox intbox = new Intbox();
                if (value != null) {
                    intbox.setValue(Integer.valueOf(value.toString()));
                }
                intbox.addEventListener(Events.ON_CHECK, SAVE_CHANGES_HANDLER);
                return intbox;
        }

        return null;
    }

    private class SaveProperties implements EventListener<Event> {
        @Override
        public void onEvent(Event event) throws Exception {

            if (event instanceof InputEvent) {
                try {
                    DesignerUtils.invokeSetter(comp, event.getTarget().getAttribute("property").toString(),
                            new Object[]{((InputEvent) event).getValue()});
                } catch (Exception e) {
                    DesignerUtils.invokeSetter(event.getTarget(), "value",
                            new Object[]{((InputEvent) event).getPreviousValue()});
                    throw e;
                }
            } else if (event instanceof CheckEvent) {
                DesignerUtils.invokeSetter(comp, event.getTarget().getAttribute("property").toString(),
                        new Object[]{((CheckEvent) event).isChecked()});
            }
        }
    }

    //group/property/attribute=value
    private static Map<String, Map<String, Map<String, String>>> getDefinition(Component comp) throws IOException {
        Map<String, Map<String, Map<String, String>>> map = new LinkedHashMap<>();

        if (comp.hasAttribute("properties")) {
            Set<Properties> properties = new LinkedHashSet<>();
            loadImports(comp.getAttribute("properties").toString(), properties);

            for (Properties p : properties) {
                String group = "Unknown";
                if (p.getProperty("group") != null) {
                    group = p.getProperty("group");
                    p.remove("group");
                }

                Map<String, Map<String, String>> groupMap;
                if (!map.containsKey(group)) {
                    map.put(group, new LinkedHashMap<String, Map<String, String>>());
                }
                groupMap = map.get(group);

                for (String name : p.stringPropertyNames()) {
                    String property = name.split("\\.")[0];
                    String attribute = name.split("\\.")[1];
                    String value = p.getProperty(name);

                    Map<String, String> propMap;
                    if (!groupMap.containsKey(property)) {
                        groupMap.put(property, new LinkedHashMap<String, String>());
                    }
                    propMap = groupMap.get(property);

                    propMap.put(attribute, value);

                }
            }


        }

        return map;
    }

    private static void loadImports(String name, Set<Properties> properties) throws IOException {
        Resource f = new ClassPathResource("com/web4thejob/designer/properties/" + name + ".properties");
        if (f.exists()) {
            Properties p = new Properties();
            p.load(f.getInputStream());
            properties.add(p);

            if (p.getProperty("import") != null) {
                for (String imp : StringUtils.commaDelimitedListToSet(p.getProperty("import"))) {
                    loadImports(imp, properties);
                }
                p.remove("import");
            }
        }

    }
}
