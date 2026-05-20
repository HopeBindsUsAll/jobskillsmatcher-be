package com.jobskillsmatcher.resource.model;

import java.util.Optional;

// Allowlist of accepted learning-resource providers. Free-form input is matched
// case-insensitively against the canonical string. OTHER is the escape hatch.
public enum Provider {
    COURSERA("Coursera"),
    UDEMY("Udemy"),
    EDX("edX"),
    PLURALSIGHT("Pluralsight"),
    YOUTUBE("YouTube"),
    MDN("MDN"),
    FREECODECAMP("freeCodeCamp"),
    OREILLY("O'Reilly"),
    MANNING("Manning"),
    GITHUB("GitHub"),
    MICROSOFT_LEARN("Microsoft Learn"),
    MICROSOFT("Microsoft"),
    REAL_PYTHON("Real Python"),
    SPRING_ACADEMY("Spring Academy"),
    BAELDUNG("Baeldung"),
    GOOGLE("Google"),
    APPLE("Apple"),
    AWS("AWS"),
    HASHICORP("HashiCorp"),
    DOCKER("Docker"),
    KUBERNETES("Kubernetes"),
    MONGODB("MongoDB"),
    REDIS("Redis"),
    POSTGRES("PostgreSQL"),
    MIT_PRESS("MIT Press"),
    MIT("MIT"),
    ADDISON_WESLEY("Addison-Wesley"),
    NO_STARCH("No Starch Press"),
    WILEY("Wiley"),
    PEARSON("Pearson"),
    A_BOOK_APART("A Book Apart"),
    KODECO("Kodeco"),
    HACKING_WITH_SWIFT("Hacking with Swift"),
    JETBRAINS("JetBrains"),
    MOZILLA("Mozilla"),
    PYTORCH("PyTorch"),
    TENSORFLOW("TensorFlow"),
    SCIKIT_LEARN("scikit-learn"),
    HUGGING_FACE("Hugging Face"),
    NUMPY("NumPy"),
    PANDAS("pandas"),
    REACT_TEAM("React Team"),
    ANGULAR_TEAM("Angular Team"),
    NODE_TEAM("Node.js Team"),
    GO_TEAM("Go Team"),
    RUST_TEAM("Rust Team"),
    APACHE("Apache"),
    DATABRICKS("Databricks"),
    SNOWFLAKE("Snowflake"),
    GITLAB("GitLab"),
    GITHUB_ACTIONS("GitHub Actions"),
    OWASP("OWASP"),
    STANFORD("Stanford"),
    APOLLO("Apollo"),
    OFFICIAL_DOCS("Official Docs"),
    OTHER("Other");

    private final String canonical;

    Provider(String canonical) {
        this.canonical = canonical;
    }

    public String canonical() {
        return canonical;
    }

    public static Optional<Provider> fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return Optional.empty();
        }
        String norm = label.trim();
        for (Provider p : values()) {
            if (p.canonical.equalsIgnoreCase(norm)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
