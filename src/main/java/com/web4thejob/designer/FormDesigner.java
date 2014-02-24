package com.web4thejob.designer;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.*;

import java.util.Map;

/**
 * @author Veniamin Isaias
 * @since 1.0.0
 */
public class FormDesigner extends SelectorComposer<Component> {
    private static final String STYLE_FOCUSED = "focused";
    private static final String ATTRIB_DROPCLASS = "dropclass";
    private final MouseOverHandler MOUSE_OVER_HANDLER = new MouseOverHandler();
    private final MouseClickHandler MOUSE_CLICK_HANDLER = new MouseClickHandler();
    private final MouseOutHandler MOUSE_OUT_HANDLER = new MouseOutHandler();
    private final DropHandler DROP_HANDLER = new DropHandler();
    private final CtrlKeyHandler CTRL_KEY_HANDLER = new CtrlKeyHandler();

    private boolean designMode;
    @Wire
    private Panel canvas;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.addEventListener(Events.ON_CTRL_KEY, CTRL_KEY_HANDLER);
        canvas.addEventListener(Events.ON_DROP, DROP_HANDLER);
        canvas.addEventListener(Events.ON_MOUSE_OVER, MOUSE_OVER_HANDLER);
        canvas.addEventListener(Events.ON_MOUSE_OUT, MOUSE_OUT_HANDLER);
        canvas.addEventListener(Events.ON_CLICK, MOUSE_CLICK_HANDLER);

        traverseChildren(comp, new ChildDelegate() {
            @Override
            public void onChild(Component child) {
                if (child.hasAttribute(ATTRIB_DROPCLASS)) {
                    ((HtmlBasedComponent) child).setDraggable("true");
                }
            }
        });
    }


    private void markSelected(Component comp) {
        ((HtmlBasedComponent) comp).setSclass(STYLE_FOCUSED);
        EventQueues.lookup("myMessageQueue", EventQueues.SESSION, true)
                .publish(new Event("onSelected", null, comp));
    }

    private void markAllUnselected() {
        traverseChildren(FormDesigner.this.canvas, new ChildDelegate() {
            @Override
            public void onChild(Component child) {
                markUnselected(child);
            }
        });
    }

    private void markUnselected(Component comp) {
        ((HtmlBasedComponent) comp).setSclass("");
    }

    private class MouseOverHandler implements EventListener<MouseEvent> {

        @Override
        public void onEvent(MouseEvent event) throws Exception {
            //onDragging event
            if (!designMode && event.getKeys() == 258) { //Ctrl
                markAllUnselected();
                designMode = true;
            }

            final boolean[] focusedChild = {false};
            traverseChildren(event.getTarget(), new ChildDelegate() {
                @Override
                public void onChild(Component child) {
                    if (child instanceof HtmlBasedComponent) {
                        if (STYLE_FOCUSED.equals(((HtmlBasedComponent) child).getSclass())) {
                            focusedChild[0] = true;
                        }
                    }
                }
            });

            if (designMode && !focusedChild[0] && event.getTarget() instanceof HtmlBasedComponent) {
                markSelected(event.getTarget());
            }
        }
    }

    private class MouseOutHandler implements EventListener<MouseEvent> {

        @Override
        public void onEvent(MouseEvent event) throws Exception {
            if (designMode && event.getTarget() instanceof HtmlBasedComponent) {
                markUnselected(event.getTarget());
            }
        }
    }

    private class CtrlKeyHandler implements EventListener<KeyEvent> {

        @Override
        public void onEvent(KeyEvent event) throws Exception {
            if (event.isCtrlKey()) {
                designMode = true;
            }
        }
    }

    private class DropHandler implements EventListener<DropEvent> {

        @Override
        public void onEvent(DropEvent event) throws Exception {
            designMode = false;

            Component comp;
            if (event.getDragged().hasAttribute(ATTRIB_DROPCLASS)) {
                String dropclass = (String) event.getDragged().getAttribute(ATTRIB_DROPCLASS);
                Class<?> clazz = Class.forName(dropclass);
                comp = (Component) clazz.newInstance();
            } else {
                comp = event.getDragged();
            }

            Component parent = event.getTarget();
            if (parent instanceof Panel)
                comp.setParent(((Panel) parent).getPanelchildren());
            else if (parent instanceof Tabbox) {
                if (comp instanceof Tab) {
                    if (((Tabbox) parent).getTabs() == null) {
                        new Tabs().setParent(parent);
                    }
                    comp.setParent(((Tabbox) parent).getTabs());
                } else if (comp instanceof Tabpanel) {
                    if (((Tabbox) parent).getTabpanels() == null) {
                        new Tabpanels().setParent(parent);
                    }
                    comp.setParent(((Tabbox) parent).getTabpanels());
                }
            } else
                comp.setParent(event.getTarget());

            if (event.getDragged().hasAttribute("defaults")) {
                Map<String, Object> map = (Map<String, Object>) event.getDragged().getAttribute("defaults");

                for (String key : map.keySet()) {
                    DesignerUtils.invokeSetter(comp, key, new Object[]{map.get(key)});
                }
            }

            trackComponent((HtmlBasedComponent) comp, event.getDragged().getAttribute("properties"));
            markAllUnselected();
            markSelected(comp);
        }
    }

    private void trackComponent(HtmlBasedComponent comp, Object initProperies) {
        comp.setAttribute("properties", initProperies);

        comp.setDroppable("true");
        comp.setDraggable("true");

        comp.addEventListener(Events.ON_DROP, DROP_HANDLER);
        comp.addEventListener(Events.ON_MOUSE_OVER, MOUSE_OVER_HANDLER);
        comp.addEventListener(Events.ON_MOUSE_OUT, MOUSE_OUT_HANDLER);
        comp.addEventListener(Events.ON_CLICK, MOUSE_CLICK_HANDLER);
    }


    private class MouseClickHandler implements EventListener<MouseEvent> {

        @Override
        public void onEvent(MouseEvent event) throws Exception {
            designMode = false;
            markAllUnselected();
            markSelected(event.getTarget());
        }
    }

    private static void traverseChildren(Component parent, ChildDelegate childDelegate) {
        childDelegate.onChild(parent);
        for (Component child : parent.getChildren()) {
            traverseChildren(child, childDelegate);
        }
    }

    private interface ChildDelegate {
        void onChild(Component child);
    }


}


