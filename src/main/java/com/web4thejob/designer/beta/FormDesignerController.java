package com.web4thejob.designer.beta;

import nu.xom.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.*;
import org.zkoss.zul.impl.Utils;
import org.zkoss.zul.impl.XulElement;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class FormDesignerController extends SelectorComposer<Component> {
    @Wire
    private Window appwindow;
    @Wire
    private Panel canvas;
    @Wire
    private Toolbar mainToolbar;
    @Wire
    private Tabbox components;
    @Wire
    private Tabbox views;
    @Wire
    private Tree outline;
    @Wire
    private Textbox code;
    private OutlineContextMenu outlineContextMenu = new OutlineContextMenu();
    private OutlineDesignStart outlineDesignStart = new OutlineDesignStart();

    private Menupopup contextMenu = new Menupopup();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        contextMenu.setPage(appwindow.getPage());
        onNew();
    }

    @Listen("onCancel=#appwindow")
    public void onEsc(KeyEvent event) {
        traverseChildren(components, Collections.emptyMap(), new ChildDelegate() {
            @Override
            public void onChild(Component child, Map params) {
                if (child instanceof Toolbarbutton) {
                    ((Toolbarbutton) child).setChecked(false);
                }
            }
        });

        Clients.evalJavaScript("w4tjDesigner.templateUUID=undefined;");
    }

    public Toolbarbutton getSelectedTemplate() {
        final Map result = new HashMap();
        traverseChildren(components, Collections.emptyMap(), new ChildDelegate() {
            @Override
            public void onChild(Component child, Map params) {
                if (child instanceof Toolbarbutton) {
                    if (((Toolbarbutton) child).isChecked()) {
                        result.put("output", child);
                    }
                }
            }
        });

        return (Toolbarbutton) result.get("output");
    }

    @Listen("onDesigneStart=#canvas")
    public void onDesigneStart(Event event) throws IOException {
        String template = getByUuid(((Map) event.getData()).get("templateUUID").toString()).getAttribute("template")
                .toString();
        Component parent = getByUuid(((Map) event.getData()).get("parent").toString());

        String uri = appwindow.getAttribute("templates").toString() + "/" + template;
        Resource resource = new ClassPathResource(uri);

        Map<String, Object> args = new HashMap<>();
        args.put("parent", parent);

        try (Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(),
                Charset.forName("UTF-8")))) {
            Component target = Executions.createComponentsDirectly(reader, null, parent, args);
            Events.echoEvent("onDesignEnd", canvas, target.getUuid());
        }
    }

    @Listen("onDesignEnd=#canvas")
    public void onDesignEnd(Event event) {
        Clients.evalJavaScript("w4tjDesigner.select('#" + event.getData() + "');");
    }

    @Listen("onSelection=#canvas")
    public void onSelection(Event event) {
        Component target = getByUuid(((Map) event.getData()).get("target").toString());
        //Clients.alert(target.toString());
    }

    @Listen("onContextMenu=#canvas")
    public void onCanvasContextMenu(Event event) {
        Component target = getByUuid((String) ((Map) event.getData()).get("target"));
        if (target instanceof XulElement) {
            prepareMenu(target);
            ((XulElement) target).setContext(contextMenu);
        }
    }

    private static boolean isFlex(String v) {
        return StringUtils.hasText(v) && !(v.equals("false") || v.equals("0"));
    }

    private void prepareMenu(Component target) {
        contextMenu.getChildren().clear();

        final Menuitem id = new Menuitem(target.getClass().getSimpleName());
        id.setParent(contextMenu);
        id.setDisabled(true);

        new Menuseparator().setParent(contextMenu);

        if (target instanceof HtmlBasedComponent) {
            final Menuitem hflex = new Menuitem("hflex");
            hflex.setParent(contextMenu);
            hflex.setAttribute("target", target);
            hflex.setCheckmark(true);
            hflex.setChecked(isFlex(((HtmlBasedComponent) target).getHflex()));
            hflex.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
                @Override
                public void onEvent(Event event) throws Exception {
                    HtmlBasedComponent target = ((HtmlBasedComponent) hflex.getAttribute("target"));
                    target.setHflex(Boolean.valueOf(!((Menuitem) event
                            .getTarget()).isChecked()).toString());

                    if (isFlex(target.getHflex())) {
                        target.setWidth(null);
                    }
                }
            });

            final Menuitem vflex = new Menuitem("vflex");
            vflex.setParent(contextMenu);
            vflex.setAttribute("target", target);
            vflex.setCheckmark(true);
            vflex.setChecked(isFlex(((HtmlBasedComponent) target).getVflex()));
            vflex.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
                @Override
                public void onEvent(Event event) throws Exception {
                    HtmlBasedComponent target = ((HtmlBasedComponent) vflex.getAttribute("target"));
                    target.setVflex(Boolean.valueOf(!((Menuitem) event
                            .getTarget()).isChecked()).toString());

                    if (isFlex(target.getVflex())) {
                        target.setHeight(null);
                    }
                }
            });

        }

        new Menuseparator().setParent(contextMenu);

        final Menuitem detach = new Menuitem("Detach");
        detach.setParent(contextMenu);
        detach.setAttribute("target", target);
        detach.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                ((Component) detach.getAttribute("target")).detach();
            }
        });


    }

    @Listen("onCheck=#components toolbarbutton")
    public void onTemplateSet(CheckEvent event) {
        final Toolbarbutton btn = (Toolbarbutton) event.getTarget();
        if (btn.isChecked()) {
            traverseChildren(components, Collections.emptyMap(), new ChildDelegate() {
                @Override
                public void onChild(Component child, Map params) {
                    if (child instanceof Toolbarbutton && !btn.equals(child)) {
                        ((Toolbarbutton) child).setChecked(false);
                    }
                }
            });

            Clients.evalJavaScript("w4tjDesigner.templateUUID='" + event.getTarget().getUuid() + "';");
        } else {
            Clients.evalJavaScript("w4tjDesigner.templateUUID=undefined;");
        }
    }

    @Listen("onClientTemplateSet=#canvas")
    public void onClientTemplateSet(Event event) {
        if (event.getData() == null) {
            traverseChildren(components, Collections.emptyMap(), new ChildDelegate() {
                @Override
                public void onChild(Component child, Map params) {
                    if (child instanceof Toolbarbutton) {
                        ((Toolbarbutton) child).setChecked(false);
                    }
                }
            });
        }
    }


    @Listen("onCheck=#mtTracking")
    public void onTrackingSet(CheckEvent event) {
        if (event.isChecked()) {
            Clients.evalJavaScript("w4tjDesigner.showTracking=true;");
        } else {
            Clients.evalJavaScript("w4tjDesigner.showTracking=false;");
        }
    }

    @Listen("onCheck=#mtTooltip")
    public void onTooltipsSet(CheckEvent event) {
        if (event.isChecked()) {
            Clients.evalJavaScript("w4tjDesigner.showTooltip=true;");
        } else {
            Clients.evalJavaScript("w4tjDesigner.showTooltip=false;");
        }
    }

    @Listen("onClick=#mtNew")
    public void onNew() {
        canvas.getPanelchildren().getChildren().clear();
        Events.echoEvent("onNewEcho", canvas, null);
    }

    @Listen("onNewEcho=#canvas")
    public void onNewEcho() {
        Clients.evalJavaScript(
                "w4tjDesigner.canvas='" + canvas.getUuid() + "';" +
                        "w4tjDesigner.monitorClicks();");
    }

    private static void traverseChildren(Component parent, Map params, ChildDelegate childDelegate) {
        childDelegate.onChild(parent, params);
        for (Component child : parent.getChildren()) {
            traverseChildren(child, new HashMap(params), childDelegate);
        }
    }

    private interface ChildDelegate {
        void onChild(Component child, Map<?, ?> params);
    }


    private static void traverseElements(Element parent, Map params, ElementDelegate elementDelegate) {
        elementDelegate.onElement(parent, params);
        for (int i = 0; i < parent.getChildElements().size(); i++) {
            traverseElements(parent.getChildElements().get(i), new HashMap(params), elementDelegate);
        }
    }

    private interface ElementDelegate {
        void onElement(Element child, Map<?, ?> params);
    }


    private Component getByUuid(final String uuid) {
        return Utils.getComponentById(appwindow, "uuid(" + uuid + ")");
    }

    public void refreshOutline() {
        outline.clear();
        Map<String, Object> params = new HashMap<>();

        Treeitem root = new Treeitem(canvas.toString());
        root.setParent(outline.getTreechildren());
        root.setValue(canvas);
        params.put("parent", root);

        traverseChildren(canvas.getPanelchildren(), params, new ChildDelegate() {
            @Override
            public void onChild(Component child, Map params) {
                Treeitem parent = (Treeitem) params.get("parent");
                if (parent.getTreechildren() == null) {
                    new Treechildren().setParent(parent);
                }

                Treeitem item = new Treeitem(child.toString());
                item.setParent(parent.getTreechildren());
                item.setValue(child);
                item.addEventListener(Events.ON_RIGHT_CLICK, outlineContextMenu);
                item.addEventListener(Events.ON_CLICK, outlineDesignStart);
                item.setContext(contextMenu);
                params.put("parent", item);
            }
        });
    }


    @Listen("onSelect=#views")
    public void onSelectView(SelectEvent event) throws IOException, ParsingException {
        switch (event.getTarget().getId()) {
            case "canvasView":
                refreshCanvas();
                break;
            case "outlineView":
                refreshOutline();
                break;
            case "codeView":
                refreshCode();
                break;
        }

    }

    private void refreshCanvas() throws ParsingException, IOException {
        onNew();
        //Component target = Executions.createComponentsDirectly(code.getValue(), null, canvas.getPanelchildren(),
        // null);

        final Builder parser = new Builder(false);
        final Document dom = parser.build(code.getValue(), null);
        Element root = dom.getRootElement();

        Map<String, Component> params = new HashMap<>();
        params.put("parent", canvas.getPanelchildren());

        traverseElements(root, params, new ElementDelegate() {
            @Override
            public void onElement(Element child, Map params) {
                Element clone = (Element) child.copy();
                clone.removeChildren();

                if (clone.getLocalName().equals("zk")) {
                    params.put("zk", clone);
                    return;
                }

                if (params.containsKey("zk")) {
                    Element zk = (Element) ((Element) params.get("zk")).copy();
                    zk.appendChild(clone);
                    clone = zk;
                }

                Component target = Executions.createComponentsDirectly(clone.toXML(), null,
                        (Component) params.get("parent"),
                        null);

                //TODO save non-visible data to target (eg attributes, comments etc)

                params.put("parent", target);
            }
        });

    }

    private void refreshCode() throws IOException {
        final String ZK_NS = "http://www.zkoss.org/2005/zul";

        final Element zk = new Element("zk", ZK_NS);
        zk.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        zk.addAttribute(new Attribute("xsi:schemaLocation", "http://www.w3.org/2001/XMLSchema-instance",
                "http://www.zkoss.org/2005/zul http://www.zkoss.org/2005/zul.xsd"));

        Map<String, Object> params = new HashMap<>();
        params.put("parent", zk);

        traverseChildren(canvas, params, new ChildDelegate() {
            @Override
            public void onChild(Component child, Map params) {
                String tag = StringUtils.uncapitalize(child.getWidgetClass().substring(child.getWidgetClass()
                        .lastIndexOf(".") + 1));
                Element element = new Element(tag, ZK_NS);
                Element parent = (Element) params.get("parent");
                parent.appendChild(element);

                //TODO append attributes

                params.put("parent", element);
            }
        });


        //zk.appendChild(root);
        Document dom = new Document(zk);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Serializer serializer = new Serializer(out, "UTF-8");
        serializer.setIndent(2);
        serializer.write(dom);
        serializer.setMaxLength(120);
        serializer.flush();
        code.setValue(out.toString("UTF-8"));
    }

    private class OutlineContextMenu implements EventListener<MouseEvent> {

        @Override
        public void onEvent(MouseEvent event) throws Exception {
            Treeitem item = ((Treeitem) event.getTarget());
            prepareMenu((Component) item.getValue());
        }
    }

    private class OutlineDesignStart implements EventListener<MouseEvent> {

        @Override
        public void onEvent(MouseEvent event) throws Exception {
            Toolbarbutton template = getSelectedTemplate();
            if (template != null) {
                Map<String, String> data = new HashMap<>();
                data.put("templateUUID", template.getUuid());
                data.put("parent", ((Component) ((Treeitem) event.getTarget()).getValue()).getUuid());
                Event dummy = new Event("onDesigneStart", null, data);
                onDesigneStart(dummy);
            }
        }
    }

}
