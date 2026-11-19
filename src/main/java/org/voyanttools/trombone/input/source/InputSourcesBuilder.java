/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.input.source;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class InputSourcesBuilder {
	
	private FlexibleParameters parameters;

	public InputSourcesBuilder(FlexibleParameters parameters) {
		this.parameters = parameters;
	}
	
	public static boolean hasParameterSources(FlexibleParameters parameters) {
		for (String p : new String[]{"file","string","uri","upload","archive"}) {
			if (parameters.getParameterValue(p,"").isEmpty()==false) {return true;}
		}
		return false;
	}

	public List<InputSource> getInputSources(Storage storage) throws IOException {
		List<InputSource> inputSources = getInputSources(storage, parameters);
		FlexibleParameters storedparams = parameters.deepClone();
		for (String key : new String[]{"upload", "string", "uri", "archive", "tool"}) storedparams.removeParameter(key);
		for (InputSource inputSource : inputSources) {
			inputSource.getMetadata().setQueryParameters(storedparams);
		}
		return inputSources;
	}
	
	private List<InputSource> getInputSources(Storage storage, FlexibleParameters params) throws IOException {
		List<InputSource> inputSources = new ArrayList<InputSource>();
		for (String file : params.getParameterValues("file")) {
			inputSources.addAll(getInputSources(storage, new File(file)));
		}

		for (String file : params.getParameterValues("upload")) {
			File f = new File(file);
			File safeFile = resolveSafeFile(storage, f);
			InputSource inputSource = new FileInputSource(safeFile);
			inputSource.getMetadata().setLocation(safeFile.getName());
			inputSources.add(inputSource);
		}
		
		for (String string : params.getParameterValues("string")) {
			inputSources.add(new StringInputSource(string));
		}

		for (String uriString : params.getParameterValues("uri")) {
			URI uri;
			try {
				uri = new URI(uriString);
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException("The URI provided by the parameters has a problem: "+uriString, e);
			}
			inputSources.add(new UriInputSource(uri));
		}
		
		for (String archive : params.getParameterValues("archive")) {
			FlexibleParameters pms = params.deepClone();
			for (String key : new String[]{"upload", "string", "uri", "archive", "tool"}) pms.removeParameter(key);
			if (archive.startsWith("http")) {
				for (String uri : archive.split("(\r\n|\r|\n)+")) {
					if (uri.startsWith("http")) {
						pms.addParameter("uri", uri);
					}
				}
				if (pms.getParameterValues("uri").length>0) {
					inputSources.addAll(getInputSources(storage, pms));
				}
			}  else {
				pms.addParameter("string", archive);
				inputSources.addAll(getInputSources(storage, pms));
			}
		}
		return inputSources;
	}
	
	private List<InputSource> getInputSources(Storage storage, File file) throws IOException {
		List<InputSource> inputSources = new ArrayList<InputSource>();
		// directories don't get cached, so handle them differently
		if (file.isDirectory()) {
			final File[] files = file.listFiles();
			Arrays.sort(files); // make sure files are in sorted order
			for (File f : files) {
				inputSources.addAll(getInputSources(storage, f));
			}
		} else if (DocumentFormat.isSkippable(file)==false) {
			File safeFile = resolveSafeFile(storage, file);
			inputSources.add(new FileInputSource(safeFile));
		}
		return inputSources;
		
	}

	public static File resolveSafeFile(Storage storage, File untrustedFile) throws IOException {
		if (storage instanceof FileStorage) {
			Path baseDir = ((FileStorage)storage).getStorageLocation().toPath();
			return resolveSafeFile(baseDir, untrustedFile.getCanonicalPath());
		} else if (storage instanceof MemoryStorage) {
			return untrustedFile;
		} else {
			throw new IllegalArgumentException("Unrecognized storage type");
		}
	}

	public static File resolveSafeFile(Path baseDir, String untrustedPath) throws IOException {
		if (baseDir == null || untrustedPath == null || untrustedPath.isBlank()) {
			throw new IllegalArgumentException("Invalid base directory or file path.");
		}

		Path basePath = baseDir.toRealPath();

		Path targetPath = basePath.resolve(untrustedPath.replace('\\', '/')).normalize();

		if (!targetPath.startsWith(basePath)) {
			throw new SecurityException("Path traversal attempt detected.");
		}

		if (Files.exists(targetPath)) {
			Path realTargetPath = targetPath.toRealPath();
			if (!realTargetPath.startsWith(basePath)) {
				throw new SecurityException("Symlink traversal attempt detected.");
			}
		}

		return targetPath.toFile();
	}
}
