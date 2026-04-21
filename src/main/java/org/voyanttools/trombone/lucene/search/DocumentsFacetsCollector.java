/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.util.BitSet;

/**
 * @author sgs
 * @deprecated This class is no longer used. Corpus document filtering
 *             is now handled at the query level or via BitSet in collectors.
 */
@Deprecated
public class DocumentsFacetsCollector extends FacetsCollector {

	BitSet documentBits;
	
	public DocumentsFacetsCollector(BitSet bitSet) {
		super();
		documentBits = bitSet;
	}
	
}
