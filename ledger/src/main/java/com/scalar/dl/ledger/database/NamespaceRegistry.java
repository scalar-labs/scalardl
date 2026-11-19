package com.scalar.dl.ledger.database;

import java.util.List;

public interface NamespaceRegistry {

  /**
   * Creates a namespace.
   *
   * @param namespace a namespace name to create
   */
  default void create(String namespace) {
    create(namespace, false);
  }

  /**
   * Creates a namespace. If {@code imported} is true, the namespace is marked as the one created
   * for importing data from another deployment.
   *
   * @param namespace a namespace name to create
   * @param imported whether the namespace is created for importing data
   */
  void create(String namespace, boolean imported);

  void drop(String namespace);

  List<String> scan(String pattern);

  /**
   * Returns whether the specified namespace is marked as imported.
   *
   * @param namespace a namespace name
   * @return true if the namespace is marked as imported
   */
  boolean isImported(String namespace);

  /**
   * Returns whether at least one namespace marked as imported exists.
   *
   * @return true if at least one namespace marked as imported exists
   */
  boolean existsImported();
}
