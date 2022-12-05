package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.service.pao.model.Pao;
import java.util.Collection;
import java.util.List;

public record ExplainGraph(List<ExplainGraphNode> explainGraph, Collection<Pao> explainPaos) {}
