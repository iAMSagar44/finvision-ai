package dev.sagar.finance_analysis_mcp;

record EvaluationResponse(Evaluation evaluation, String feedback) {
	public enum Evaluation {

		PASS, NEEDS_IMPROVEMENT, FAIL, UNKNOWN

	}

}
