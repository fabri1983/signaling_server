package org.fabri1983.signaling.core.handler.signal;

import java.util.function.Supplier;

import org.nextrtc.signalingserver.domain.Signal;

public class ConditionalSignalActuator<B> {
	
	private Signal sourceSignal;
	private Signal targetSignal;
	private SignalJunction targetSignalJunctor;
	private Runnable action;
	private Supplier<B> supplier;
	
	/**
	 * Private constructor so only using method {@link ConditionalSignalActuator#ifSignal(String)} allows the user 
	 * to create an instance.
	 */
	private ConditionalSignalActuator() {
	}
	
	public static <B> ConditionalSignalActuator<B> ifSignal(String sourceSignal) {
		ConditionalSignalActuator<B> actuator = new ConditionalSignalActuator<B>();
		actuator.sourceSignal = Signal.fromString(sourceSignal);
		return actuator;
	}

	public ConditionalSignalActuator<B> is(Signal targetSignal) {
		this.targetSignal = targetSignal;
		return this;
	}
	
	public ConditionalSignalActuator<B> is(SignalJunction signalJunctor) {
		this.targetSignalJunctor = signalJunctor;
		return this;
	}

	public ConditionalSignalActuator<B> then(Runnable action) {
		this.action = action;
		return this;
	}
	
	public ConditionalSignalActuator<B> then(Supplier<B> supplier) {
		this.supplier = supplier;
		return this;
	}
	
	public void go() {
		if (targetSignal != null) {
			if (targetSignal.is(sourceSignal)) {
				action.run();
			}
		}
		else if (targetSignalJunctor.apply(sourceSignal)) {
			action.run();
		}
	}
	
	public B get() {
		if (targetSignal != null) {
			if (targetSignal.is(sourceSignal)) {
				return supplier.get();
			}
		}
		else if (targetSignalJunctor.apply(sourceSignal)) {
			return supplier.get();
		}
		
		return null;
	}
	
}