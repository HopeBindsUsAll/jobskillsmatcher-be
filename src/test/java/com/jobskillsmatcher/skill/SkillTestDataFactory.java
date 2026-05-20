package com.jobskillsmatcher.skill;

import com.jobskillsmatcher.skill.impl.jpa.Skill;

public class SkillTestDataFactory {

    public final static String JAVA_ID = "esco/skill/java";
    public final static String PYTHON_ID = "esco/skill/python";
    public final static String SQL_ID = "esco/skill/sql";

    public static Skill java() {
        return newSkill(JAVA_ID, "Java", new String[]{"jdk", "jvm"},
                "General-purpose object-oriented programming language for the JVM.");
    }

    public static Skill python() {
        return newSkill(PYTHON_ID, "Python", new String[]{"python3"},
                "Dynamic programming language popular for scripting and machine learning.");
    }

    public static Skill sql() {
        return newSkill(SQL_ID, "SQL", new String[]{"structured query language"},
                "Declarative query language for relational databases.");
    }

    private static Skill newSkill(String id, String label, String[] altLabels, String description) {
        Skill e = new Skill();
        e.setId(id);
        e.setPreferredLabel(label);
        e.setAltLabels(altLabels);
        e.setDescription(description);
        return e;
    }
}
