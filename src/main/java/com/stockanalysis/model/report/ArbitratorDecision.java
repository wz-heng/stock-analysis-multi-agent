package com.stockanalysis.model.report;
import lombok.Data;
import java.util.List;

@Data
public class ArbitratorDecision {
    private String finalRating;
    private int confidencePercent;
    private String strongestArgument;
    private String weakestArgument;
    private String synthesisText;
    private boolean needsSecondRound;
    private String secondRoundFocus;
    private List<DebateArgument> debateArguments;
}
