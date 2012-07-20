#ImageTerrier Examples

These are some usage examples for the ImageTerrier command-line tools. Note that the order of the command-line options and flags is important as certain options
must be first enabled to allow other options that are related to them.

##Indexing

NOTE: all the indexing examples use the "-f" switch to force the features to be re-generated. This is important if you are playing with ImageTerrier as unless you
are using a fixed vocabulary (specified by -q), a new vocabulary will be generated that won't match the cached features.

###Basic Indexing

Build a `BASIC` index from the images in `/Users/jsh2/Desktop/test-data/` using `FASTKMEANS` clustering  (the default) with `100` centres. Force the features to be regenerated.

	java -jar ImageTerrierTools-3.5-SNAPSHOT-jar-with-dependencies.jar BasicIndexer -m IMAGES -f -t BASIC -k 100 -o ./test-basic.idx /Users/jsh2/Desktop/test-data/


Build a BASIC index from the images in `/Users/jsh2/Desktop/test-data/` using `RANDOMSET` clustering with `100` centres, and save a copy of the quantiser to `./quant-rndset-100.voc`. Force the features to be regenerated.

	java -jar ImageTerrierTools-3.5-SNAPSHOT-jar-with-dependencies.jar BasicIndexer -m IMAGES -f -t BASIC -qt RANDOMSET -k 100 -q ./quant-rndset-100.voc -o ./test-basic-rndset.idx /Users/jsh2/Desktop/test-data/

###Position Indexing

Build a `POSITION` index from the images in `/Users/jsh2/Desktop/test-data/` using `FASTKMEANS` clustering  (the default) with `100` centres. Force the features to be regenerated. The index will contain the (`x`, `y`, `scale`, `orientation`) of each feature. Each of the position values is quantised to an 8-bit integer in the index (`-nb`), and we have to specify the minimum and maximum expected value for each of the values using the `-mins` and `-maxs`. The minimum value for `x`, `y` and `scale` will always be zero. The maximum values for `x` and `y` will be the largest width and height for the images being indexed. `150.0` is a good estimate for the scale. The orientations of the SIFT features being indexed are always `-Pi:Pi`.

	java -jar ImageTerrierTools-3.5-SNAPSHOT-jar-with-dependencies.jar BasicIndexer -m IMAGES -f -t POSITION -pm SPATIAL_SCALE_ORI -mins 0.0,0.0,0.0,-3.14157 -maxs 1000.0,1000.0,150.0,3.14157 -nb 8,8,8,8 -k 100 -o ./test-position.idx /Users/jsh2/Desktop/test-data/


Build a `POSITION` index from the images in `/Users/jsh2/Desktop/test-data/` using the `100` centre vocabulary built by the second `BASIC` example above. Force the features to be regenerated. The index will contain the `orientation` of each feature quantised into an 8-bit integer.

	java -jar ImageTerrierTools-3.5-SNAPSHOT-jar-with-dependencies.jar BasicIndexer -m IMAGES -f -t POSITION -pm ORI -mins -3.14157 -maxs 3.14157 -nb 8 -q ./quant-rndset-100.voc -k 100 -o ./test-position-ori.idx /Users/jsh2/Desktop/test-data/
	
##Searching

Perform a basic search using an image that was indexed. We expect the image to be the first one returned as it is an exact match. This command will work with all types of index. The scores will be slightly different due to differences in the vocabulary used; if you built two indexes (`BASIC` and `POSITION`) as described above, then the results should be identical.

	java -jar ImageTerrierTools-3.5-SNAPSHOT-jar-with-dependencies.jar BasicSearcher -i ./test-basic.idx -q /Users/jsh2/Desktop/test-data/1000.jpeg

Indexes with `x` and `y` position information let you apply an additional re-ranking schemes to improve the precision. For example, an Affine-transform could be fitted between the query image and each image in the result set; only results where a consistent transform was found would be retained:

	java -jar ImageTerrierTools-3.5-SNAPSHOT-jar-with-dependencies.jar BasicSearcher -i ./test-position.idx/ -q /Users/jsh2/Desktop/test-data/1000.jpeg -sm AFFINE
	
If you have orientation indexed, then this can also be used to improve precision:
	
	java -jar ImageTerrierTools-3.5-SNAPSHOT-jar-with-dependencies.jar BasicSearcher -i ./test-position.idx/ -q /Users/jsh2/Desktop/test-data/1000.jpeg -sm CONSISTENT_ORI	
	
	