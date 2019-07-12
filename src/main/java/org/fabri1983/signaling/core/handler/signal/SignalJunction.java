package org.fabri1983.signaling.core.handler.signal;

import org.nextrtc.signalingserver.domain.Signal;

public interface SignalJunction {

	boolean apply(Signal signal);

}
