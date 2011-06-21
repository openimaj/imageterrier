# ImageTerrierTools Readme #

ImageTerrierTools is a collection of tools for building, inspecting and searching ImageTerrier indexes. The ImageTerrier tools are command-line utilities, and can be invoked with the following command:

	java -Xmx2G -jar ImageTerrierTools-X.Y.Z-jar-with-dependencies.jar mode [options...]

where X.Y.Z is the ImageTerrier version (i.e. 3.0.1) and mode is (currently) one of:

* BasicIndexer
* BasicSearcher
* IndexStats

The options are specific to the selected mode.

## BasicIndexer Mode ##

BasicIndexer mode is used to construct an ImageTerrier index. The BasicIndexer takes care of all the steps required to go from a set of image files to an ImageTerrier index:

1. Feature Extraction
2. [optional] Visual-word vocabulary learning
3. Feature Quantisation into a Bag-of-Visual-Words
4. Index construction

### Feature extraction ###
The BasicIndexer is (currently) capable of using the following local feature implementations from the [OpenIMAJ project](http://www.openimaj.org):

* `SIFT`  -  Standard Difference-of-Gaussian SIFT implementation
* `MIN_MAX_SIFT`  -  Extended Difference-of-Gaussian SIFT implementation that also records whether each interest point is either minima or maxima.
* `ASIFT`  -  Affine SIFT; Applies the standard SIFT algorithm to 27 affine simulations of the image in order to increase feature density and provide better robustness to affine transformations. The created local features in this mode have the same form as for the standard SIFT implementation and do not record information about which affine simulation they resulted from.
* `ASIFTENRICHED`  -  As for ASIFT, but records the simulation index and the affine (tilt & theta) parameters for each local feature.

The type of feature chosen will affect the types of index that can be created as described below. The type of feature to be extracted is chosen using the `-ft` or `--feature-type` option:

	java -Xmx2G -jar ImageTerrierTools-X.Y.Z-jar-with-dependencies.jar BasicIndexer -ft SIFT

### Vocabulary learning ###
In order to quantise the extracted features into visual terms, the BasicIndexer is capable of either loading a pre-existing vocabulary, or creating one from the extracted features directly. A number of techniques for creating vocabularies are included:

* `RANDOMSET`  -  Randomly select features to be prototypical visual words.
* `FASTKMEANS`  -  Use Exact or approximate K-Means to learn prototypical visual words.
* `HKMEANS`  -  Use hierarchical (exact or approximate) k-means to learn prototypical visual words.

To load an existing vocabulary, use the `-q` or `--quant-file` option; if specified the quantiser file doesn't exist a new vocabulary will be created instead, and the results saved to this file.

The `-qt` or `--quant-type` option controls the vocabulary type. Different vocabulary types have different options: 

* `RANDOMSET`
	* `-K` specifies the number of visual words to learn.
* `FASTKMEANS`
	* `-K` specifies the number of visual words to learn.
	* `-itr` specifies the number of k-means iterations (default 30).
	* `-ex` enables exact mode instead of approximate (boolean flag; has no argument).
* `HKMEANS`
	* `-d` specifies the depth of the hierarchy.
	* `-k` specifies the number of branches per node.
	* `-kt` choose between exact (`FASTKMEANS_EXACT`) and approximate modes (`FASTKMEANS_KDTREE`). Defaults to exact.

In addition, all the types share a common option, `-p`, which specifies the precision (`INT` or `BYTE`). For all currently implemented features it is strongly recommended that the precision is set to `BYTE` in order to reduce the required memory (this doesn't affect retrieval performance in any way).

For example, the following command would create a vocabulary with 10000 terms using the `RANDOMSET` method from SIFT features. The resultant vocabulary is saved to a file `file.voc` (assumed not to pre-exist) for later re-use.

	java -Xmx2G -jar ImageTerrierTools-X.Y.Z-jar-with-dependencies.jar BasicIndexer -ft SIFT -q file.voc -qt RANDOMSET -K 10000 -p BYTE
	
### Feature Quantisation ###
As previously described, the vocabulary for quantisation is either loaded from a pre-existing file with the `-q` option, or a new vocabulary is created. There are no additional options for the quantisation phase.

### Index Construction ###
BasicIndexer is capable of producing a number of different types of index:

* `BASIC` - A basic index with no term payload information (i.e. geometric consistency cannot be applied). This type of index can be created from any type of feature.
* `NEAREST_NEIGHBOUR` - An index that stores the 15 nearest-neighbouring (currently this is hardcoded, but may change in the future) terms in each posting. This type of index can be created from any type of feature, and enables nearest-neighbour weighted search in BasicSearcher.
* `POSITION` - An index that stores information about the spatial location of a visual term within the postings. This type of index can be created from any type of feature, and allow various kinds of geometric consistency checks to be performed at search time.
* `AFFINESIM` - An index created from `ASIFTENRICHED` features that encodes the affine simulation index in the postings payload in order to enable affine-consistent weighting in the search.

The type of index to be created is specified by the `-t` or `--type` option. Only the `POSITION` type has additional options as described below.

#### POSITION Indexes ####



## BasicSearcher Mode ##

	Usage: java -Xmx2G -jar ImageTerrierTools-X.Y.Z-jar-with-dependencies.jar BasicSearcher [options...]
	 --display-query (-dq)                  : display query
	 --display-results (-dr)                : display results
	 --index (-i) path                      : index path
	 --interactive                          : interactive mode
	 --interest-region (-r) coords          : coordinates for query region-of-intere
	                                          st (x,y,h,w)
	 --limit (-l) number                    : limit the number of results returned
	 --query (-q) path                      : query image path
	 --score-modifier (-sm) [HOMOGRAPHY |   : Use specified model for re-ranking
	 AFFINE | CONSISTENT_ORI | CONSISTENT_S : results.
	 CALE | CONSISTENT_AFFINE | NEAREST_NEI :  
	 GHBOUR | NONE]                         :  
	 --soft-quant-neighbours N              : Number of neighbours to use for soft
	                                          quantisation
	 --time                                 : time the feature extraction and
	                                          querying
	 -matching-model (-mm) [TFIDF | L1 |    : Choose matching model
	 L1QNORM | COSINE | L1IDF]

## IndexStats Mode ##

