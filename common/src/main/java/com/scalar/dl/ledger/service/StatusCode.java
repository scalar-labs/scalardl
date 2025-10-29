package com.scalar.dl.ledger.service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Status code of registration or execution requests to ledger.
 *
 * <p>The 2xx class of status code indicates that the request has succeeded.
 *
 * <p>The 3xx class of status code indicates that an asset record in the database is in an invalid
 * state and possibly tampered.
 *
 * <p>The 4xx class of status code indicates that the server cannot or will not process the request
 * due to something that is perceived to be a client error (e.g., invalid signature, invalid key
 * pair, execution error inside contracts or contract is not found).
 *
 * <p>The 5xx class of status code indicates that the ledger server encountered an unexpected
 * condition that prevented it from fulfilling the request.
 *
 * <p>The 6xx class of status code indicates that the client encountered an unexpected condition
 * that prevented it from fulfilling the request.
 *
 * @author Hiroyuki Yamada
 */
public enum StatusCode {
  /** StatusCode: 200. This indicates that the registration/execution request has succeeded. */
  OK(200),

  /**
   * StatusCode: 300. This indicates that the existing hash value of an asset record is different
   * from the expected value.
   */
  INVALID_HASH(300),

  /**
   * StatusCode: 301. This indicates that the existing prev_hash value of an asset record is
   * different from the expected value.
   */
  INVALID_PREV_HASH(301),

  /**
   * StatusCode: 302. This indicates that some previously executed contract produced an asset record
   * which could not be validated.
   */
  INVALID_CONTRACT(302),

  /**
   * StatusCode: 303. This indicates that the existing data value of an asset record is different
   * from the expected value.
   */
  INVALID_OUTPUT(303),

  /** StatusCode: 304. This indicates that the same nonce value has been used more than once. */
  INVALID_NONCE(304),

  /**
   * StatusCode: 305. This indicates that the ledger states between multiple organizations are
   * inconsistent.
   */
  INCONSISTENT_STATES(305),

  /**
   * StatusCode: 306. This indicates that a request is inconsistent and could be maliciously
   * tampered.
   */
  INCONSISTENT_REQUEST(306),

  /**
   * StatusCode: 400. This indicates that the given signature is invalid or a signature can not be
   * created for some reason.
   */
  INVALID_SIGNATURE(400),

  /**
   * StatusCode: 401. This indicates that the given key could not be loaded for some reason, e.g.,
   * it is invalid.
   */
  UNLOADABLE_KEY(401),

  /**
   * StatusCode: 402. This indicates that the given contract could not be loaded for some reason,
   * e.g., instantiation failure.
   */
  UNLOADABLE_CONTRACT(402),

  /** StatusCode: 403. This indicates that the given certificate is not found. */
  CERTIFICATE_NOT_FOUND(403),

  /** StatusCode: 404. This indicates that the given contract is not found. */
  CONTRACT_NOT_FOUND(404),

  /** StatusCode: 405. This indicates that the given certificate is already registered. */
  CERTIFICATE_ALREADY_REGISTERED(405),

  /** StatusCode: 406. This indicates that the given contract is already registered. */
  CONTRACT_ALREADY_REGISTERED(406),

  /** StatusCode: 407. This indicates that the request is invalid. */
  INVALID_REQUEST(407),

  /**
   * StatusCode: 408. This indicates that the contract has a contextual error that is not
   * recoverable by the ledger.
   */
  CONTRACT_CONTEXTUAL_ERROR(408),

  /** StatusCode: 409. This indicates that the specified asset is not found. */
  ASSET_NOT_FOUND(409),

  /** StatusCode: 410. This indicates that the given function is not found. */
  FUNCTION_NOT_FOUND(410),

  /**
   * StatusCode: 411. This indicates that the given function could not be loaded for some reason.
   */
  UNLOADABLE_FUNCTION(411),

  /** StatusCode: 412. This indicates that the given function is invalid */
  INVALID_FUNCTION(412),

  /** StatusCode: 413. This indicates that the given secret is already registered. */
  SECRET_ALREADY_REGISTERED(413),

  /** StatusCode: 414. This indicates that the argument is invalid. */
  INVALID_ARGUMENT(414),

  /** StatusCode: 415. This indicates that the given secret is not found. */
  SECRET_NOT_FOUND(415),

  /**
   * StatusCode: 500. This indicates that the system encountered a database error such as IO error.
   */
  DATABASE_ERROR(500),

  /** StatusCode: 501. This indicates that the system encountered a unknown transaction status. */
  UNKNOWN_TRANSACTION_STATUS(501),

  /** StatusCode: 502. This indicates that the system encountered a runtime error. */
  RUNTIME_ERROR(502),

  /** StatusCode: 503. This indicates that the system is temporarily unavailable. */
  UNAVAILABLE(503),

  /** StatusCode: 504. This indicates that the system encountered conflicting transactions. */
  CONFLICT(504);

  private static final Map<Integer, StatusCode> lookup = new HashMap<>();

  static {
    for (StatusCode c : EnumSet.allOf(StatusCode.class)) lookup.put(c.get(), c);
  }

  private final int code;

  StatusCode(int code) {
    this.code = code;
  }

  public int get() {
    return this.code;
  }

  public static StatusCode get(int code) {
    return lookup.get(code);
  }
}
