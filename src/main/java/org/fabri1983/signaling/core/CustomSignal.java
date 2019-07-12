package org.fabri1983.signaling.core;

import org.nextrtc.signalingserver.domain.Signal;

public class CustomSignal {

	public static final Signal PONG = Signal.fromString("pong");
	public static final Signal REJECT = Signal.fromString("reject");
	public static final Signal OPEN = Signal.fromString("open");
	public static final Signal DROP_SIMU = Signal.fromString("drop_simu");
	public static final Signal DROP = Signal.fromString("drop");
	public static final Signal ONHOLD = Signal.fromString("onhold");
	public static final Signal CANCEL_CALL = Signal.fromString("cancel_call");
	public static final Signal OTHER_IN_CALL = Signal.fromString("other_in_call");
	public static final Signal OTHER_IN_ROOM = Signal.fromString("other_in_room");
	
	public static final Signal VIDEO_OFF = Signal.fromString("video_off");
	public static final Signal VIDEO_ON = Signal.fromString("video_on");
	public static final Signal AUDIO_OFF = Signal.fromString("audio_off");
	public static final Signal AUDIO_ON = Signal.fromString("audio_on");
	
}
