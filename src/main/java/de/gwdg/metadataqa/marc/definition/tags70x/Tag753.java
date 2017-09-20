package de.gwdg.metadataqa.marc.definition.tags70x;

import de.gwdg.metadataqa.marc.definition.Cardinality;
import de.gwdg.metadataqa.marc.definition.DataFieldDefinition;
import de.gwdg.metadataqa.marc.definition.Indicator;
import de.gwdg.metadataqa.marc.definition.general.codelist.SubjectHeadingAndTermSourceCodes;

/**
 * System Details Access to Computer Files
 * http://www.loc.gov/marc/bibliographic/bd753.html
 */
public class Tag753 extends DataFieldDefinition {

	private static Tag753 uniqueInstance;

	private Tag753() {
		initialize();
	}

	public static Tag753 getInstance() {
		if (uniqueInstance == null)
			uniqueInstance = new Tag753();
		return uniqueInstance;
	}

	private void initialize() {
		tag = "753";
		label = "System Details Access to Computer Files";
		cardinality = Cardinality.Repeatable;
		ind1 = new Indicator("");
		ind2 = new Indicator("");
		setSubfieldsWithCardinality(
			"a", "Make and model of machine", "NR",
			"b", "Programming language", "NR",
			"c", "Operating system", "NR",
			"0", "Authority record control number or standard number", "R",
			"2", "Source of term", "NR",
			"6", "Linkage", "NR",
			"8", "Field link and sequence number", "R"
		);
		getSubfield("2").setCodeList(SubjectHeadingAndTermSourceCodes.getInstance());
	}
}