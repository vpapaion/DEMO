package gr.vaios.demo.domain;

public final class Capability {
    private final String title;
    private final String description;
    private final int score;

    public Capability(String title, String description, int score) {
        this.title = title;
        this.description = description;
        this.score = Math.max(0, Math.min(100, score));
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getScore() { return score; }
}
