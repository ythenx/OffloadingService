package com.symlab.dandelion.lib;

import java.io.Serializable;

public abstract class Offloadable implements Serializable {

	private static final long serialVersionUID = 1L;

	public abstract void copyState(Offloadable state);
}
