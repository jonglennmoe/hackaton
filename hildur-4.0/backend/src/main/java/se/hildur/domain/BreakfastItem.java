package se.hildur.domain;

import java.util.List;

/** The breakfast menu. Display names live in the frontend translations. */
public enum BreakfastItem {
    RYE, SALMON, FIL, EGG, WAFFLE, COFFEE, TEA, JUICE;

    /** Time slots offered for tomorrow's breakfast (served 07–10). */
    public static final List<String> TIMES = List.of("07:00", "07:30", "08:00", "08:30", "09:00", "09:30");
}
