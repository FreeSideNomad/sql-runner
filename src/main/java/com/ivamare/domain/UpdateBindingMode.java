package com.ivamare.domain;

/** Defines how UPDATE SQL parameters are bound to values. */
public enum UpdateBindingMode {
  /** Standard mode - UPDATE uses only user-input parameters (current behavior). */
  STANDARD,

  /** Batch mode - Collects all primary key values into :id_list for IN clause. */
  BATCH,

  /** Row-by-row mode - Executes one UPDATE per preview row, binding column values. */
  ROW_BY_ROW
}
