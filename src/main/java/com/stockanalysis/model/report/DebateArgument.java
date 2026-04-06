package com.stockanalysis.model.report;

import lombok.Data;

@Data
public class DebateArgument {
    private Stance stance;    // BULL / BEAR / NEUTRAL
    private String model;
    private String mainPoints;
    private String evidence;
    private String conclusion;
}
