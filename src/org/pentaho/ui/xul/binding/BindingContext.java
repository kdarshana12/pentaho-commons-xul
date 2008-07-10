package org.pentaho.ui.xul.binding;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.XulEventSource;

public class BindingContext {

  private XulDomContainer container;

  private List<Binding> bindings = new ArrayList<Binding>();

  private static final Log logger = LogFactory.getLog(BindingContext.class);

  //internal map of Binding to PropChangeListeners, used to cleanup upon removal
  private Map<Binding, List<PropertyChangeListener>> bindingListeners = new HashMap<Binding, List<PropertyChangeListener>>();

  public BindingContext(XulDomContainer container) {
    this.container = container;
  }

  public void add(XulComponent source, String expr) {
    BindingExpression expression = BindingExpression.parse(expr);
    XulComponent target = container.getDocumentRoot().getElementById(expression.target);
    Binding newBinding = new Binding(source, expression.sourceAttr, target, expression.targetAttr);
    add(newBinding);
  }

  public void remove(Binding bind) {
    if (!bindingListeners.containsKey(bind) && !bindings.contains(bind)) {
      return;
    }
    bind.destroyBindings();
    bindingListeners.remove(bind);
    bindings.remove(bind);

  }

  public void add(Binding bind) {
    try {
      bindings.add(bind);
      bind.bindForward();

      if (!bindingListeners.containsKey(bind)) {
        bindingListeners.put(bind, new ArrayList<PropertyChangeListener>());
      }
      bindingListeners.get(bind).add(bind.getForwardListener());

      if (bind.getBindingType() == Binding.Type.BI_DIRECTIONAL) {
        bind.bindReverse();
        bindingListeners.get(bind).add(bind.getReverseListener());
      }
      
      bind.setContext(this);
    } catch (Throwable t) {
      throw new BindingException("Binding failed: " + bind.getSource() + "." + bind.getSourceAttr() + " <==> "
          + bind.getTarget() + "." + bind.getTargetAttr(), t);
    }
  }
}