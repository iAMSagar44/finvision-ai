package dev.sagar.cash_flow_analyser.views.finance_services;

record EvaluationResponse(Evaluation evaluation, String feedback) {
  public enum Evaluation {
    PASS, NEEDS_IMPROVEMENT, FAIL, UNKNOWN
  }

}
