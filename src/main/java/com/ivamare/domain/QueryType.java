package com.ivamare.domain;

/** Type of query template. */
public enum QueryType {
  /** Simple SELECT query with results display. */
  SELECT,

  /** UPDATE workflow with 5-step wizard (preview, confirm, backup, execute, rollback). */
  UPDATE_WORKFLOW
}
