Trombone
========

Trombone is a multi-purpose text analysis library. It makes it relatively easy
to create corpora from a variety of sources (URLs, files, strings, etc.) and in
a variety of formats (XML, HTML, PDF, etc.). Some basic term frequency,
distribution, and keyword-in-context functionality is available (intended for
use by more sophisticated front-end interfaces like Voyant Tools).

## Quick Reference ##

While awaiting more detailed documentation, here's a quick overview of how to use Trombone in stand-alone mode. The most important thing is to run org.voyanttools.trombone.Controller as a Java Application (in Eclipse, just right-click on the file, choose *Run As* and select *Java Application*). This should produce results in the console (not very interesting ones since we haven't provided any parameters). To run more interesting operations, edit the run configuration that was created when you just ran the application. Use quotes around key=value pairs that have spaces. Here are some useful parameters:

* **storage**: *file|memory* use *file* to force things to be written (and reused) on disk
* importing – each of these can be duplicated (e.g. string=the string=a)
	* **file**: the location of a file or directory to import 
	* **uri**: the location of a URI to import
	* **string**: a string to import
* tools: specify the tool key and use one of these values:
	* **corpus.CorpusMetadata**: get high-level metadata (this is useful for minimal output during corpus ingestion)
	* **corpus.CorpusTerms**: list the terms in the corpus
	* **corpus.DocumentTerms**: list the terms in each document
		* **docIndex**: *number* the location of the document(s) in the corpus
		* **docId**: the document ID

## Build an executable jar ##

To build an executable jar run the command :
```bash
$ mvn clean compile assembly:single
```
A `.jar` named `trombone-<version>-jar-with-dependencies.jar` should be created.
You can now run the jar with the desired tool, for example, with the `corpus.DocumentContexts` :
```bash
$ java -jar \
    ./target/trombone-5.2.1-SNAPSHOT-jar-with-dependencies.jar \
    tool=corpus.DocumentContexts \
    query=cash \
    query=mine \
    file=path/to/file-or-directory \
    outputFile=path/to/output/output.json
```

License
-------

Trombone is currently under a GPLv3 license. See the [license file](.license.txt).
