package org.voyanttools.trombone.model;

public enum EntityType {
	location, person, organization, misc, // stanford 4 class model
	money, time, percent, date, // stanford 7 class model
	duration, set, unknown, // ?
	gpe, product, cardinal, quantity, event, fac, language, law, norp, work_of_art; // spacy
	public static EntityType getForgivingly(String type) {
		String typeString = type.toLowerCase();
		for (EntityType t : EntityType.values()) {
			if (t.name().equals(typeString)) return t;
		}
		return unknown;
	}
}