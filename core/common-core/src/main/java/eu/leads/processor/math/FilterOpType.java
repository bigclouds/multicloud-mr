package eu.leads.processor.math;

/**
 * Created by vagvaz on 9/24/14.
 */
public enum FilterOpType {
  // Unary expression
  NOT,

  // Binary expression
  AND,
  OR,
  EQUAL,
  IS_NULL,
  NOT_EQUAL,
  LTH,
  LEQ,
  GTH,
  GEQ,
  //   PLUS(BinaryEval.class, "+"),
  //   MINUS(BinaryEval.class, "-"),
  //   MODULAR(BinaryEval.class, "%"),
  //   MULTIPLY(BinaryEval.class, "*"),
  //   DIVIDE(BinaryEval.class, "/"),

  // Binary Bitwise expressions
  //   BIT_AND(BinaryEval.class, "&"),
  //   BIT_OR(BinaryEval.class, "|"),
  //   BIT_XOR(BinaryEval.class, "|"),

  // Function
  //   WINDOW_FUNCTION(WindowFunctionEval.class),
  AGG_FUNCTION,
  FUNCTION,

  // String operator or pattern matching predicates
  LIKE,
  //   SIMILAR_TO,
  //   REGEX(RegexPredicateEval.class),
  //   CONCATENATE(BinaryEval.class, "||"),

  // Other predicates
  //   BETWEEN(BetweenPredicateEval.class),
  //   CASE(CaseWhenEval.class),
  //   IF_THEN(CaseWhenEval.IfThenEval.class),
  IN,

  // Value or Reference
  SIGNED,
  CAST,
  ROW_CONSTANT,
  FIELD,
  CONST;
}
