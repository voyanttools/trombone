package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

public abstract class AbstractAsyncCorpusTool extends AbstractCorpusTool {
	
	@XStreamOmitField
	protected ListenableFuture<?> future;

	public AbstractAsyncCorpusTool(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		runAsync(corpusMapper);
	}

	public abstract ListenableFuture<?> runAsync(CorpusMapper corpusMapper) throws IOException;

	public ListenableFuture<?> getFuture() {
		return future;
	}
	
}
