package org.fabri1983.signaling.core.handler.signal;

import java.util.Arrays;
import java.util.List;

import org.nextrtc.signalingserver.domain.Signal;

public class SignalDisjunction implements SignalJunction {

	private List<Signal> orList;
	
	public SignalDisjunction(Signal[] orList) {
		this.orList = Arrays.asList(orList);
	}
	
	@Override
	public boolean apply(Signal signal) {
		return orList.stream().anyMatch( s -> s.equals(signal) );
	}
	
	public static SignalDisjunction or(Signal... orList){
		return new SignalDisjunction(orList);
	}
	
}
