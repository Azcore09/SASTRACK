package com.sastrack.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ViolationCatalog {
    private static final Map<String, List<String>> SECTIONS = createSections();

    private static Map<String, List<String>> createSections() {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        sections.put("1.1 Unauthorized Leave from Class and/or School", List.of(
                "a. No class slip when absent",
                "b. Leaving school by climbing the wall",
                "c. Cutting classes"));
        sections.put("1.2 School Uniform", List.of(
                "a. ID/uniform/tapping violations",
                "b. Grooming policy violations"));
        sections.put("1.3 Intellectual Dishonesty", List.of(
                "a. Plagiarism",
                "b. Data fabrication or falsification",
                "c. Academic dishonesty",
                "d. Academic misconduct",
                "e. Submitting the same work to multiple subjects"));
        sections.put("1.4 Fraud", List.of("a. False statements"));
        sections.put("1.5 Harm", List.of(
                "a. Sending poison letters",
                "b. Fights or disturbances on campus",
                "c. Attacking a person",
                "d. Scandalous acts",
                "e. Sexual harassment",
                "f. Discrimination against women (RA 9710)",
                "g. Disrespectful or threatening behavior"));
        sections.put("1.6 Damage to Property", List.of(
                "a. Damaging school property",
                "b. Stealing or tampering with belongings"));
        sections.put("1.7 Possession and/or Use", List.of(
                "a. Pornographic material",
                "b. Alcohol or intoxication",
                "c. Dangerous objects"));
        sections.put("1.8 Other Inappropriate Behavior", List.of(
                "a. Obscene content or foul language",
                "b. Public display of affection",
                "c. Smoking",
                "d. Gambling",
                "e. Obstructing investigations or disobeying orders",
                "f. Repeated classroom rule violations",
                "g. Fighting while in uniform",
                "h. Repeated warning offenses",
                "i. Firecrackers"));
        sections.put("1.9 IT Resources", List.of("a. Violating IT policies"));
        sections.put("2.1 Fraud", List.of(
                "a. False statements during registration",
                "b. False statements when using facilities",
                "c. False statements for grants or prizes",
                "d. Hiding member/officer changes",
                "e. Misusing the organization's name"));
        sections.put("2.2 Harm to Persons", List.of(
                "a. Fights or disturbances",
                "b. Accepting members without one-semester residency",
                "c. Violating Anti-Hazing Law (RA 8049)"));
        sections.put("2.3 Damage to Property", List.of(
                "a. Damage, littering, vandalism, or unauthorized entry"));
        sections.put("2.4 Any Other Misconduct", List.of(
                "a. Disobeying school authorities",
                "b. Harming the school's order or reputation"));
        return Collections.unmodifiableMap(sections);
    }

    public static List<String> sections() {
        return List.copyOf(SECTIONS.keySet());
    }

    public static List<String> violationsFor(String section) {
        return SECTIONS.getOrDefault(section, List.of());
    }

    private ViolationCatalog() {
    }
}
