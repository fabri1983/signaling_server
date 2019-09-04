package org.fabri1983.signaling.core.distributed.serialization;

import java.io.Serializable;

/**
 * This class only intended to replace null objects on serialization, to avoid data shifting in the output stream.
 */
public class NullObject implements Serializable {

	private static final long serialVersionUID = 1L;

}
