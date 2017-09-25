package de.gwdg.metadataqa.marc.utils;

import de.gwdg.metadataqa.marc.definition.DataFieldDefinition;
import org.reflections.Reflections;

import java.util.*;

public class MarcTagLister {

	public static List<Class<? extends DataFieldDefinition>> listTags() {
		Reflections reflections = new Reflections("de.gwdg.metadataqa.marc.definition.tags");

		Set<Class<? extends DataFieldDefinition>> subTypes = reflections.getSubTypesOf(DataFieldDefinition.class);

		Comparator<Class<? extends DataFieldDefinition>> byTag = (e1, e2) ->
			e1.getSimpleName().compareTo(e2.getSimpleName());

		List<Class<? extends DataFieldDefinition>> tags = new ArrayList<>();

		subTypes
			.stream()
			.sorted(byTag)
			.forEach((Class tagClass) -> {
				tags.add(tagClass);
			});

		return tags;
	}
}