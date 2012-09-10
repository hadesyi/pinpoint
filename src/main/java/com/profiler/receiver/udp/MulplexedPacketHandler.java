package com.profiler.receiver.udp;

import java.net.DatagramPacket;

import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;

import com.profiler.common.dto.thrift.Span;
import com.profiler.common.util.DefaultTBaseLocator;
import com.profiler.common.util.HeaderTBaseDeserializer;
import com.profiler.common.util.TBaseLocator;
import com.profiler.data.reader.JVMDataReader;
import com.profiler.data.reader.Reader;
import com.profiler.data.reader.SpanReader;
import com.profiler.dto.JVMInfoThriftDTO;

public class MulplexedPacketHandler implements Runnable {

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private final JVMDataReader jvmDataReader = new JVMDataReader();
	private final SpanReader spanReader = new SpanReader();

	private DatagramPacket datagramPacket;
	private TBaseLocator locator = new DefaultTBaseLocator();

	public MulplexedPacketHandler(DatagramPacket datagramPacket) {
		this.datagramPacket = datagramPacket;
	}

	@Override
	public void run() {
		HeaderTBaseDeserializer deserializer = new HeaderTBaseDeserializer();
		TBase<?, ?> tBase = null;
		try {
			tBase = deserializer.deserialize(locator, datagramPacket.getData());
			dispatch(tBase, datagramPacket);
		} catch (TException e) {
			logger.warn("packet serialize error " + e.getMessage(), e);
		}
	}

	private void dispatch(TBase<?, ?> tBase, DatagramPacket datagramPacket) {
		Reader readHandler = getReadHandler(tBase, datagramPacket);
		if (logger.isDebugEnabled()) {
			logger.debug("handler name:" + readHandler.getClass().getName());
		}
		readHandler.handler(tBase, datagramPacket);
	}

	private Reader getReadHandler(TBase<?, ?> tBase, DatagramPacket datagramPacket) {
		if (tBase instanceof JVMInfoThriftDTO) {
			return jvmDataReader;
		}
		if (tBase instanceof Span) {
			return spanReader;
		}
		throw new UnsupportedOperationException();
	}
}
