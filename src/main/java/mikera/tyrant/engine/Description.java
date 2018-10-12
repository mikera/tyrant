package mikera.tyrant.engine;

// interface for all object descriptions
// Describer is the standard Description object
public interface Description {
	// Description type constants
	int NAMETYPE_NORMAL = 0; // most items and monsters
	int NAMETYPE_PROPER = 1; // proper names
	int NAMETYPE_QUANTITY = 2; // materials, measurable
												   // quantities and plural
												   // nouns (e.g. "trousers")

	int GENDER_NEUTER = 0;
	int GENDER_MALE = 1;
	int GENDER_FEMALE = 2;

	int CASE_NOMINATIVE = 0;
	int CASE_ACCUSATIVE = 1;
	int CASE_GENITIVE = 2;

	int ARTICLE_NONE = 0;
	int ARTICLE_DEFINITE = 1;
	int ARTICLE_INDEFINITE = 2;
	int ARTICLE_POSSESIVE = 3;
	
	int NUMBER_SINGULAR = 1;
	int NUMBER_PLURAL = 1000000;

	String getName(int number, int article);

	String getDescriptionText();

}