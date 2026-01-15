package com.ivamare.domain;

/** Status of a query execution. */
public enum ExecutionStatus {
  /** Query executed successfully. */
  SUCCESS,

  /** Query execution failed with an error. */
  FAILED,

  /** Query was cancelled by user. */
  CANCELLED,

  /** Query exceeded timeout limit. */
  TIMEOUT
}
