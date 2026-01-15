package com.ivamare.domain;

/** Type of query execution. */
public enum ExecutionType {
  /** Simple SELECT query execution. */
  SELECT,

  /** UPDATE workflow execution. */
  UPDATE,

  /** Rollback of a previous UPDATE. */
  ROLLBACK
}
