package com.acme.obs.apm;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;

public abstract class AbstractApmClient implements ApmClient {
  protected final ApmProperties props;
  protected final ThreadLocal<Deque<ApmSpan>> spanStack = ThreadLocal.withInitial(ArrayDeque::new);

  protected AbstractApmClient(ApmProperties props){ this.props = props; }

  protected void push(ApmSpan span){ spanStack.get().push(span); }
  protected void pop(){
    Deque<ApmSpan> s = spanStack.get();
    if(!s.isEmpty()){ s.pop(); }
  }

  @Override public ApmSpan startSpan(String name){ return startSpan(name, Collections.emptyMap()); }

  @Override public ApmSpan startSpan(String name, Map<String,String> attrs){
    ApmSpan span = doStartSpan(name, attrs);
    push(span);
    return new GuardedSpan(span, this);
  }

  protected abstract ApmSpan doStartSpan(String name, Map<String,String> attrs);

  private static class GuardedSpan implements ApmSpan {
    private final ApmSpan delegate;
    private final AbstractApmClient owner;
    private boolean closed=false;
    GuardedSpan(ApmSpan d, AbstractApmClient o){ this.delegate=d; this.owner=o; }

    @Override public ApmSpan setAttribute(String k, String v){ delegate.setAttribute(k,v); return this; }
    @Override public ApmSpan setAttribute(String k, long v){ delegate.setAttribute(k,v); return this; }
    @Override public ApmSpan setAttribute(String k, double v){ delegate.setAttribute(k,v); return this; }
    @Override public ApmSpan recordException(Throwable t){ delegate.recordException(t); return this; }

    @Override public void close(){
      if(!closed){
        try { delegate.close(); }
        finally { owner.pop(); closed = true; }
      }
    }
  }
}
