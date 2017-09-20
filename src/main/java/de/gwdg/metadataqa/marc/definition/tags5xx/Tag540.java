package de.gwdg.metadataqa.marc.definition.tags5xx;

import de.gwdg.metadataqa.marc.definition.Cardinality;
import de.gwdg.metadataqa.marc.definition.DataFieldDefinition;
import de.gwdg.metadataqa.marc.definition.Indicator;

/**
 * Terms Governing Use and Reproduction Note
 * http://www.loc.gov/marc/bibliographic/bd540.html
 */
public class Tag540 extends DataFieldDefinition {

	private static Tag540 uniqueInstance;

	private Tag540() {
		initialize();
	}

	public static Tag540 getInstance() {
		if (uniqueInstance == null)
			uniqueInstance = new Tag540();
		return uniqueInstance;
	}

	private void initialize() {

		tag = "540";
		label = "Terms Governing Use and Reproduction Note";
		cardinality = Cardinality.Repeatable;
		ind1 = new Indicator("");
		ind2 = new Indicator("");
		setSubfieldsWithCardinality(
			"a", "Terms governing use and reproduction", "NR",
			"b", "Jurisdiction", "NR",
			"c", "Authorization", "NR",
			"d", "Authorized users", "NR",
			"u", "Uniform Resource Identifier", "R",
			"3", "Materials specified", "NR",
			"5", "Institution to which field applies", "NR",
			"6", "Linkage", "NR",
			"8", "Field link and sequence number", "R"
		);
	}
}